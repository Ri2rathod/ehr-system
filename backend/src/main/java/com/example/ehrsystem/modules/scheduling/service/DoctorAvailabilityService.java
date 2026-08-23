package com.example.ehrsystem.modules.scheduling.service;

import com.example.ehrsystem.common.security.SecurityContextAccessor;
import com.example.ehrsystem.modules.doctor.entity.Doctor;
import com.example.ehrsystem.modules.doctor.repository.DoctorRepository;
import com.example.ehrsystem.modules.scheduling.dto.request.CreateAvailabilityRequest;
import com.example.ehrsystem.modules.scheduling.dto.request.UpdateAvailabilityRequest;
import com.example.ehrsystem.modules.scheduling.dto.response.AvailabilityResponse;
import com.example.ehrsystem.modules.scheduling.entity.DoctorAvailability;
import com.example.ehrsystem.modules.scheduling.repository.DoctorAvailabilityRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class DoctorAvailabilityService {

    private final DoctorAvailabilityRepository availabilityRepository;
    private final DoctorRepository doctorRepository;
    private final SecurityContextAccessor securityContext;

    /**
     * Create a single availability block for a doctor.
     */
    @Transactional
    public AvailabilityResponse create(UUID doctorUuid, CreateAvailabilityRequest request) {
        Doctor doctor = findDoctorOrThrow(doctorUuid);
        validateTimeRange(request.getStartTime(), request.getEndTime());

        Long currentUserId = securityContext.getCurrentUserId();

        DoctorAvailability availability = DoctorAvailability.builder()
                .doctor(doctor)
                .dayOfWeek(request.getDayOfWeek())
                .scheduleType(request.getScheduleType())
                .startTime(request.getStartTime())
                .endTime(request.getEndTime())
                .isActive(request.getIsActive() != null ? request.getIsActive() : true)
                .effectiveFrom(request.getEffectiveFrom())
                .effectiveUntil(request.getEffectiveUntil())
                .createdBy(currentUserId)
                .updatedBy(currentUserId)
                .build();

        DoctorAvailability saved = availabilityRepository.save(availability);
        return toResponse(saved);
    }

    /**
     * Create multiple availability blocks at once (bulk schedule setup).
     */
    @Transactional
    public List<AvailabilityResponse> createBulk(UUID doctorUuid, List<CreateAvailabilityRequest> requests) {
        Doctor doctor = findDoctorOrThrow(doctorUuid);
        Long currentUserId = securityContext.getCurrentUserId();

        List<DoctorAvailability> availabilities = requests.stream()
                .map(request -> {
                    validateTimeRange(request.getStartTime(), request.getEndTime());
                    return DoctorAvailability.builder()
                            .doctor(doctor)
                            .dayOfWeek(request.getDayOfWeek())
                            .scheduleType(request.getScheduleType())
                            .startTime(request.getStartTime())
                            .endTime(request.getEndTime())
                            .isActive(request.getIsActive() != null ? request.getIsActive() : true)
                            .effectiveFrom(request.getEffectiveFrom())
                            .effectiveUntil(request.getEffectiveUntil())
                            .createdBy(currentUserId)
                            .updatedBy(currentUserId)
                            .build();
                })
                .collect(Collectors.toList());

        List<DoctorAvailability> saved = availabilityRepository.saveAll(availabilities);
        return saved.stream().map(this::toResponse).collect(Collectors.toList());
    }

    /**
     * Get the full weekly schedule for a doctor (active blocks only).
     */
    public List<AvailabilityResponse> getWeeklySchedule(UUID doctorUuid) {
        Doctor doctor = findDoctorOrThrow(doctorUuid);
        return availabilityRepository.findActiveByDoctorId(doctor.getId())
                .stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    /**
     * Get a single availability block by its UUID.
     */
    public AvailabilityResponse getByUuid(UUID uuid) {
        DoctorAvailability availability = availabilityRepository.findByUuid(uuid)
                .orElseThrow(() -> new EntityNotFoundException("Availability not found with UUID: " + uuid));
        return toResponse(availability);
    }

    /**
     * Update an availability block.
     */
    @Transactional
    public AvailabilityResponse update(UUID doctorUuid, UUID availabilityUuid, UpdateAvailabilityRequest request) {
        findDoctorOrThrow(doctorUuid); // ensure doctor exists

        DoctorAvailability availability = availabilityRepository.findByUuid(availabilityUuid)
                .orElseThrow(() -> new EntityNotFoundException(
                        "Availability not found with UUID: " + availabilityUuid));

        // Verify the availability belongs to the doctor
        if (!availability.getDoctor().getUuid().equals(doctorUuid)) {
            throw new IllegalArgumentException("Availability does not belong to the specified doctor");
        }

        if (request.getDayOfWeek() != null) availability.setDayOfWeek(request.getDayOfWeek());
        if (request.getScheduleType() != null) availability.setScheduleType(request.getScheduleType());
        if (request.getStartTime() != null) availability.setStartTime(request.getStartTime());
        if (request.getEndTime() != null) availability.setEndTime(request.getEndTime());
        if (request.getIsActive() != null) availability.setIsActive(request.getIsActive());
        if (request.getEffectiveFrom() != null) availability.setEffectiveFrom(request.getEffectiveFrom());
        if (request.getEffectiveUntil() != null) availability.setEffectiveUntil(request.getEffectiveUntil());

        // Re-validate after partial update
        validateTimeRange(availability.getStartTime(), availability.getEndTime());

        availability.setUpdatedBy(securityContext.getCurrentUserId());

        DoctorAvailability updated = availabilityRepository.save(availability);
        return toResponse(updated);
    }

    /**
     * Soft-deactivate an availability block (preferred over hard delete).
     */
    @Transactional
    public AvailabilityResponse deactivate(UUID doctorUuid, UUID availabilityUuid) {
        findDoctorOrThrow(doctorUuid);

        DoctorAvailability availability = availabilityRepository.findByUuid(availabilityUuid)
                .orElseThrow(() -> new EntityNotFoundException(
                        "Availability not found with UUID: " + availabilityUuid));

        if (!availability.getDoctor().getUuid().equals(doctorUuid)) {
            throw new IllegalArgumentException("Availability does not belong to the specified doctor");
        }

        availability.setIsActive(false);
        availability.setUpdatedBy(securityContext.getCurrentUserId());

        DoctorAvailability updated = availabilityRepository.save(availability);
        return toResponse(updated);
    }

    // ── Helpers ──────────────────────────────────────────────

    private Doctor findDoctorOrThrow(UUID doctorUuid) {
        return doctorRepository.findByUuidAndDeletedAtIsNull(doctorUuid)
                .orElseThrow(() -> new EntityNotFoundException(
                        "Doctor not found with UUID: " + doctorUuid));
    }

    private void validateTimeRange(java.time.LocalTime startTime, java.time.LocalTime endTime) {
        if (startTime != null && endTime != null && !startTime.isBefore(endTime)) {
            throw new IllegalArgumentException("Start time must be before end time");
        }
    }

    private AvailabilityResponse toResponse(DoctorAvailability availability) {
        return AvailabilityResponse.builder()
                .id(availability.getId())
                .uuid(availability.getUuid())
                .doctorUuid(availability.getDoctor().getUuid())
                .dayOfWeek(availability.getDayOfWeek())
                .scheduleType(availability.getScheduleType())
                .startTime(availability.getStartTime())
                .endTime(availability.getEndTime())
                .isActive(availability.getIsActive())
                .effectiveFrom(availability.getEffectiveFrom())
                .effectiveUntil(availability.getEffectiveUntil())
                .createdAt(availability.getCreatedAt())
                .updatedAt(availability.getUpdatedAt())
                .build();
    }
}
