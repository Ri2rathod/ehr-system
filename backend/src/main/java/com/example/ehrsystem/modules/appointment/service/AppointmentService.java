package com.example.ehrsystem.modules.appointment.service;

import com.example.ehrsystem.common.security.SecurityContextAccessor;
import com.example.ehrsystem.modules.appointment.dto.request.CreateAppointmentRequest;
import com.example.ehrsystem.modules.appointment.dto.response.AppointmentResponse;
import com.example.ehrsystem.modules.appointment.entity.Appointment;
import com.example.ehrsystem.modules.appointment.entity.AppointmentStatus;
import com.example.ehrsystem.modules.appointment.repository.AppointmentRepository;
import com.example.ehrsystem.modules.doctor.entity.Doctor;
import com.example.ehrsystem.modules.doctor.repository.DoctorRepository;
import com.example.ehrsystem.modules.patient.entity.Patient;
import com.example.ehrsystem.modules.patient.repository.PatientRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AppointmentService {

    private final AppointmentRepository appointmentRepository;
    private final PatientRepository patientRepository;
    private final DoctorRepository doctorRepository;
    private final AppointmentNumberService appointmentNumberService;
    private final SecurityContextAccessor securityContext;

    @Transactional
    public AppointmentResponse create(CreateAppointmentRequest request) {
        if (request.getStartTime().isAfter(request.getEndTime()) || request.getStartTime().isEqual(request.getEndTime())) {
            throw new IllegalArgumentException("Start time must be before end time");
        }

        Patient patient = patientRepository.findByUuidAndDeletedAtIsNull(request.getPatientUuid())
                .orElseThrow(() -> new EntityNotFoundException("Patient not found with UUID: " + request.getPatientUuid()));

        Doctor doctor = doctorRepository.findByUuidAndDeletedAtIsNull(request.getDoctorUuid())
                .orElseThrow(() -> new EntityNotFoundException("Doctor not found with UUID: " + request.getDoctorUuid()));

        // Conflict Validation
        if (appointmentRepository.existsOverlappingDoctorAppointment(
                doctor.getId(), request.getStartTime(), request.getEndTime(), null)) {
            throw new IllegalArgumentException("Doctor is already booked for this time slot.");
        }

        if (appointmentRepository.existsOverlappingPatientAppointment(
                patient.getId(), request.getStartTime(), request.getEndTime(), null)) {
            throw new IllegalArgumentException("Patient already has another appointment during this time slot.");
        }

        Long currentUserId = securityContext.getCurrentUserId();

        Appointment appointment = Appointment.builder()
                .patient(patient)
                .doctor(doctor)
                .appointmentDate(request.getStartTime().toLocalDate())
                .startTime(request.getStartTime())
                .endTime(request.getEndTime())
                .visitType(request.getVisitType())
                .status(AppointmentStatus.SCHEDULED)
                .reasonForVisit(request.getReasonForVisit())
                .notes(request.getNotes())
                .createdBy(currentUserId)
                .updatedBy(currentUserId)
                .build();

        appointment.setAppointmentNumber(appointmentNumberService.generateAppointmentNumber());

        Appointment saved = appointmentRepository.save(appointment);
        return toResponse(saved);
    }

    public AppointmentResponse getByUuid(UUID uuid) {
        Appointment appointment = appointmentRepository.findByUuidAndDeletedAtIsNull(uuid)
                .orElseThrow(() -> new EntityNotFoundException("Appointment not found with UUID: " + uuid));
        return toResponse(appointment);
    }

    public AppointmentResponse getByAppointmentNumber(String appointmentNumber) {
        Appointment appointment = appointmentRepository.findByAppointmentNumberAndDeletedAtIsNull(appointmentNumber)
                .orElseThrow(() -> new EntityNotFoundException("Appointment not found with appointment number: " + appointmentNumber));
        return toResponse(appointment);
    }

    public Page<AppointmentResponse> getAll(Pageable pageable) {
        return appointmentRepository.findByDeletedAtIsNull(pageable)
                .map(this::toResponse);
    }

    @Transactional
    public AppointmentResponse updateStatus(UUID uuid, AppointmentStatus newStatus, String cancellationReason) {
        Appointment appointment = appointmentRepository.findByUuidAndDeletedAtIsNull(uuid)
                .orElseThrow(() -> new EntityNotFoundException("Appointment not found with UUID: " + uuid));

        validateStatusTransition(appointment.getStatus(), newStatus);

        appointment.setStatus(newStatus);
        LocalDateTime now = LocalDateTime.now();

        if (newStatus == AppointmentStatus.CHECKED_IN) {
            appointment.setCheckedInAt(now);
        } else if (newStatus == AppointmentStatus.IN_PROGRESS) {
            appointment.setInProgressAt(now);
        } else if (newStatus == AppointmentStatus.COMPLETED) {
            appointment.setCompletedAt(now);
        } else if (newStatus == AppointmentStatus.CANCELLED) {
            appointment.setCancelledAt(now);
            appointment.setCancellationReason(cancellationReason);
        }

        appointment.setUpdatedBy(securityContext.getCurrentUserId());

        Appointment saved = appointmentRepository.save(appointment);
        return toResponse(saved);
    }

    @Transactional
    public AppointmentResponse reschedule(UUID uuid, LocalDateTime newStartTime, LocalDateTime newEndTime) {
        Appointment appointment = appointmentRepository.findByUuidAndDeletedAtIsNull(uuid)
                .orElseThrow(() -> new EntityNotFoundException("Appointment not found with UUID: " + uuid));

        if (appointment.getStatus() == AppointmentStatus.COMPLETED ||
            appointment.getStatus() == AppointmentStatus.CANCELLED ||
            appointment.getStatus() == AppointmentStatus.NO_SHOW) {
            throw new IllegalArgumentException("Cannot reschedule a terminal appointment");
        }

        if (newStartTime.isAfter(newEndTime) || newStartTime.isEqual(newEndTime)) {
            throw new IllegalArgumentException("Start time must be before end time");
        }

        // Conflict validation excluding this appointment
        if (appointmentRepository.existsOverlappingDoctorAppointment(
                appointment.getDoctor().getId(), newStartTime, newEndTime, uuid)) {
            throw new IllegalArgumentException("Doctor is already booked for this time slot.");
        }

        if (appointmentRepository.existsOverlappingPatientAppointment(
                appointment.getPatient().getId(), newStartTime, newEndTime, uuid)) {
            throw new IllegalArgumentException("Patient already has another appointment during this time slot.");
        }

        appointment.setStartTime(newStartTime);
        appointment.setEndTime(newEndTime);
        appointment.setAppointmentDate(newStartTime.toLocalDate());
        appointment.setStatus(AppointmentStatus.SCHEDULED); // Reset status to SCHEDULED on reschedule
        appointment.setUpdatedBy(securityContext.getCurrentUserId());

        Appointment saved = appointmentRepository.save(appointment);
        return toResponse(saved);
    }

    private void validateStatusTransition(AppointmentStatus currentStatus, AppointmentStatus newStatus) {
        if (currentStatus == newStatus) {
            return;
        }

        if (currentStatus == AppointmentStatus.COMPLETED ||
            currentStatus == AppointmentStatus.CANCELLED ||
            currentStatus == AppointmentStatus.NO_SHOW) {
            throw new IllegalArgumentException("Cannot change status from terminal state: " + currentStatus);
        }

        boolean allowed = false;
        switch (currentStatus) {
            case SCHEDULED:
                allowed = (newStatus == AppointmentStatus.CONFIRMED ||
                           newStatus == AppointmentStatus.CANCELLED ||
                           newStatus == AppointmentStatus.RESCHEDULED ||
                           newStatus == AppointmentStatus.NO_SHOW);
                break;
            case CONFIRMED:
                allowed = (newStatus == AppointmentStatus.CHECKED_IN ||
                           newStatus == AppointmentStatus.CANCELLED ||
                           newStatus == AppointmentStatus.RESCHEDULED ||
                           newStatus == AppointmentStatus.NO_SHOW);
                break;
            case CHECKED_IN:
                allowed = (newStatus == AppointmentStatus.IN_PROGRESS ||
                           newStatus == AppointmentStatus.CANCELLED);
                break;
            case IN_PROGRESS:
                allowed = (newStatus == AppointmentStatus.COMPLETED ||
                           newStatus == AppointmentStatus.CANCELLED);
                break;
            case RESCHEDULED:
                // RESCHEDULED is terminal for the old record, no further transition allowed
                allowed = false;
                break;
            default:
                allowed = false;
        }

        if (!allowed) {
            throw new IllegalArgumentException("Invalid status transition from " + currentStatus + " to " + newStatus);
        }
    }

    private AppointmentResponse toResponse(Appointment appointment) {
        return AppointmentResponse.builder()
                .id(appointment.getId())
                .uuid(appointment.getUuid())
                .appointmentNumber(appointment.getAppointmentNumber())
                .patientUuid(appointment.getPatient().getUuid())
                .patientName(appointment.getPatient().getFullName())
                .patientMrn(appointment.getPatient().getMrn())
                .doctorUuid(appointment.getDoctor().getUuid())
                .doctorName(appointment.getDoctor().getFullName())
                .doctorCode(appointment.getDoctor().getDoctorCode())
                .appointmentDate(appointment.getAppointmentDate())
                .startTime(appointment.getStartTime())
                .endTime(appointment.getEndTime())
                .visitType(appointment.getVisitType())
                .status(appointment.getStatus())
                .reasonForVisit(appointment.getReasonForVisit()) // mapped from DB field
                .notes(appointment.getNotes())
                .cancellationReason(appointment.getCancellationReason())
                .checkedInAt(appointment.getCheckedInAt())
                .inProgressAt(appointment.getInProgressAt())
                .completedAt(appointment.getCompletedAt())
                .cancelledAt(appointment.getCancelledAt())
                .createdAt(appointment.getCreatedAt())
                .updatedAt(appointment.getUpdatedAt())
                .build();
    }
}
