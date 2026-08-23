package com.example.ehrsystem.modules.scheduling.service;

import com.example.ehrsystem.modules.appointment.repository.AppointmentRepository;
import com.example.ehrsystem.modules.doctor.entity.Doctor;
import com.example.ehrsystem.modules.doctor.repository.DoctorRepository;
import com.example.ehrsystem.modules.scheduling.dto.response.AvailableSlotResponse;
import com.example.ehrsystem.modules.scheduling.dto.response.AvailableSlotsResponse;
import com.example.ehrsystem.modules.scheduling.entity.DayOfWeekEnum;
import com.example.ehrsystem.modules.scheduling.entity.DoctorAvailability;
import com.example.ehrsystem.modules.scheduling.entity.DoctorAvailabilityException;
import com.example.ehrsystem.modules.scheduling.entity.ScheduleType;
import com.example.ehrsystem.modules.scheduling.repository.DoctorAvailabilityExceptionRepository;
import com.example.ehrsystem.modules.scheduling.repository.DoctorAvailabilityRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Dynamically generates appointment slots from a doctor's recurring schedule,
 * accounting for breaks, exceptions, and existing bookings.
 *
 * <p>Algorithm:
 * <ol>
 *   <li>Determine the day-of-week for the requested date</li>
 *   <li>Load effective WORK and BREAK periods for that day</li>
 *   <li>Load exceptions for that date</li>
 *   <li>Generate candidate slots from WORK periods</li>
 *   <li>Remove slots that overlap BREAK periods</li>
 *   <li>Remove slots affected by exceptions</li>
 *   <li>Query existing appointments and mark booked slots</li>
 *   <li>Return the final slot list with availability status</li>
 * </ol>
 * </p>
 */
@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
public class SlotGenerationService {

    private final DoctorRepository doctorRepository;
    private final DoctorAvailabilityRepository availabilityRepository;
    private final DoctorAvailabilityExceptionRepository exceptionRepository;
    private final AppointmentRepository appointmentRepository;

    /**
     * Generate available slots for a doctor on a specific date.
     *
     * @param doctorUuid the doctor's external UUID
     * @param date       the date to generate slots for
     * @return response containing all candidate slots with availability flags
     */
    public AvailableSlotsResponse generateSlots(UUID doctorUuid, LocalDate date) {
        Doctor doctor = doctorRepository.findByUuidAndDeletedAtIsNull(doctorUuid)
                .orElseThrow(() -> new EntityNotFoundException(
                        "Doctor not found with UUID: " + doctorUuid));

        int slotDuration = doctor.getConsultationDurationMinutes();
        DayOfWeekEnum dayOfWeek = DayOfWeekEnum.from(date.getDayOfWeek());

        // ── 1. Check full-day exceptions first (early exit) ──
        if (exceptionRepository.existsFullDayException(doctor.getId(), date)) {
            log.debug("Doctor {} has full-day exception on {}", doctorUuid, date);
            return AvailableSlotsResponse.builder()
                    .doctorUuid(doctorUuid)
                    .date(date)
                    .slotDurationMinutes(slotDuration)
                    .slots(List.of())
                    .build();
        }

        // ── 2. Load effective schedule for the day ──
        List<DoctorAvailability> workPeriods = availabilityRepository.findEffectiveByDoctorDayAndType(
                doctor.getId(), dayOfWeek, ScheduleType.WORK, date);

        if (workPeriods.isEmpty()) {
            log.debug("No WORK schedule for doctor {} on {} ({})", doctorUuid, date, dayOfWeek);
            return AvailableSlotsResponse.builder()
                    .doctorUuid(doctorUuid)
                    .date(date)
                    .slotDurationMinutes(slotDuration)
                    .slots(List.of())
                    .build();
        }

        List<DoctorAvailability> breakPeriods = availabilityRepository.findEffectiveByDoctorDayAndType(
                doctor.getId(), dayOfWeek, ScheduleType.BREAK, date);

        // ── 3. Load partial-day exceptions ──
        List<DoctorAvailabilityException> exceptions =
                exceptionRepository.findByDoctorIdAndDate(doctor.getId(), date);

        // ── 4. Generate candidate slots from WORK periods ──
        List<SlotCandidate> candidates = new ArrayList<>();
        for (DoctorAvailability work : workPeriods) {
            LocalTime cursor = work.getStartTime();
            while (cursor.plusMinutes(slotDuration).compareTo(work.getEndTime()) <= 0) {
                candidates.add(new SlotCandidate(cursor, cursor.plusMinutes(slotDuration)));
                cursor = cursor.plusMinutes(slotDuration);
            }
        }

        // ── 5. Build the result, marking availability ──
        List<AvailableSlotResponse> slots = new ArrayList<>();
        for (SlotCandidate candidate : candidates) {
            boolean available = true;

            // Check against BREAK periods
            if (overlapsAnyBreak(candidate, breakPeriods)) {
                available = false;
            }

            // Check against exceptions (partial-day)
            if (available && overlapsAnyException(candidate, exceptions)) {
                available = false;
            }

            // Check against existing appointments
            if (available) {
                LocalDateTime slotStart = LocalDateTime.of(date, candidate.start);
                LocalDateTime slotEnd = LocalDateTime.of(date, candidate.end);
                if (appointmentRepository.existsOverlappingDoctorAppointment(
                        doctor.getId(), slotStart, slotEnd, null)) {
                    available = false;
                }
            }

            slots.add(AvailableSlotResponse.builder()
                    .startTime(candidate.start)
                    .endTime(candidate.end)
                    .available(available)
                    .build());
        }

        return AvailableSlotsResponse.builder()
                .doctorUuid(doctorUuid)
                .date(date)
                .slotDurationMinutes(slotDuration)
                .slots(slots)
                .build();
    }

    /**
     * Validates whether a specific time slot is available for booking.
     * Used by AppointmentService before creating an appointment.
     *
     * @param doctor    the doctor entity
     * @param date      appointment date
     * @param startTime slot start time
     * @param endTime   slot end time
     * @throws IllegalArgumentException if the slot is not available
     */
    public void validateSlotAvailability(Doctor doctor, LocalDate date,
                                         LocalTime startTime, LocalTime endTime) {
        DayOfWeekEnum dayOfWeek = DayOfWeekEnum.from(date.getDayOfWeek());

        // 1. Check doctor is active
        if (!"ACTIVE".equals(doctor.getStatus())) {
            throw new IllegalArgumentException("Doctor is not currently active");
        }

        if (!Boolean.TRUE.equals(doctor.getIsAvailableForAppointments())) {
            throw new IllegalArgumentException("Doctor is not available for appointments");
        }

        // 2. Check weekly schedule exists
        if (!availabilityRepository.existsByDoctorIdAndIsActiveTrue(doctor.getId())) {
            throw new IllegalArgumentException("Doctor has no active schedule configured");
        }

        // 3. Check full-day exception
        if (exceptionRepository.existsFullDayException(doctor.getId(), date)) {
            throw new IllegalArgumentException("Doctor is unavailable on " + date + " (full-day exception)");
        }

        // 4. Check requested time is inside a WORK period
        List<DoctorAvailability> workPeriods = availabilityRepository.findEffectiveByDoctorDayAndType(
                doctor.getId(), dayOfWeek, ScheduleType.WORK, date);

        boolean insideWorkPeriod = workPeriods.stream()
                .anyMatch(w -> !startTime.isBefore(w.getStartTime()) && !endTime.isAfter(w.getEndTime()));

        if (!insideWorkPeriod) {
            throw new IllegalArgumentException(
                    "Requested time " + startTime + "-" + endTime +
                    " is outside the doctor's working hours on " + dayOfWeek);
        }

        // 5. Check overlap with BREAK periods
        List<DoctorAvailability> breakPeriods = availabilityRepository.findEffectiveByDoctorDayAndType(
                doctor.getId(), dayOfWeek, ScheduleType.BREAK, date);

        SlotCandidate slot = new SlotCandidate(startTime, endTime);
        if (overlapsAnyBreak(slot, breakPeriods)) {
            throw new IllegalArgumentException(
                    "Requested time " + startTime + "-" + endTime + " overlaps with a break period");
        }

        // 6. Check overlap with partial-day exceptions
        List<DoctorAvailabilityException> exceptions =
                exceptionRepository.findByDoctorIdAndDate(doctor.getId(), date);

        if (overlapsAnyException(slot, exceptions)) {
            throw new IllegalArgumentException(
                    "Requested time " + startTime + "-" + endTime +
                    " is blocked by a schedule exception on " + date);
        }
    }

    // ── Internal helpers ─────────────────────────────────────

    private boolean overlapsAnyBreak(SlotCandidate slot, List<DoctorAvailability> breaks) {
        return breaks.stream().anyMatch(b ->
                slot.start.isBefore(b.getEndTime()) && slot.end.isAfter(b.getStartTime()));
    }

    private boolean overlapsAnyException(SlotCandidate slot, List<DoctorAvailabilityException> exceptions) {
        return exceptions.stream()
                .filter(e -> !Boolean.TRUE.equals(e.getIsFullDay())) // full-day handled earlier
                .anyMatch(e -> e.getStartTime() != null && e.getEndTime() != null &&
                        slot.start.isBefore(e.getEndTime()) && slot.end.isAfter(e.getStartTime()));
    }

    /**
     * Internal value object for a candidate time slot.
     */
    private record SlotCandidate(LocalTime start, LocalTime end) {
    }
}
