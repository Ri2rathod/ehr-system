package com.example.ehrsystem.modules.appointment.service;

import com.example.ehrsystem.common.security.SecurityContextAccessor;
import com.example.ehrsystem.common.util.AuditLogger;
import com.example.ehrsystem.modules.appointment.dto.request.CreateAppointmentRequest;
import com.example.ehrsystem.modules.appointment.dto.response.AppointmentHistoryResponse;
import com.example.ehrsystem.modules.appointment.dto.response.AppointmentResponse;
import com.example.ehrsystem.modules.appointment.entity.Appointment;
import com.example.ehrsystem.modules.appointment.entity.AppointmentHistory;
import com.example.ehrsystem.modules.appointment.entity.AppointmentStatus;
import com.example.ehrsystem.modules.appointment.entity.VisitType;
import com.example.ehrsystem.modules.appointment.repository.AppointmentHistoryRepository;
import com.example.ehrsystem.modules.appointment.repository.AppointmentRepository;
import com.example.ehrsystem.modules.doctor.entity.Doctor;
import com.example.ehrsystem.modules.doctor.repository.DoctorRepository;
import com.example.ehrsystem.modules.patient.entity.Patient;
import com.example.ehrsystem.modules.patient.repository.PatientRepository;
import com.example.ehrsystem.modules.scheduling.service.SlotGenerationService;
import jakarta.persistence.EntityNotFoundException;
import jakarta.persistence.criteria.Predicate;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AppointmentService {

    private final AppointmentRepository appointmentRepository;
    private final AppointmentHistoryRepository historyRepository;
    private final PatientRepository patientRepository;
    private final DoctorRepository doctorRepository;
    private final AppointmentNumberService appointmentNumberService;
    private final SecurityContextAccessor securityContext;
    private final SlotGenerationService slotGenerationService;
    private final AuditLogger auditLogger;

    @Transactional
    public AppointmentResponse create(CreateAppointmentRequest request) {
        if (request.getStartTime().isAfter(request.getEndTime()) || request.getStartTime().isEqual(request.getEndTime())) {
            throw new IllegalArgumentException("Start time must be before end time");
        }

        Patient patient = patientRepository.findByUuidAndDeletedAtIsNull(request.getPatientUuid())
                .orElseThrow(() -> new EntityNotFoundException("Patient not found with UUID: " + request.getPatientUuid()));

        Doctor doctor = doctorRepository.findByUuidAndDeletedAtIsNull(request.getDoctorUuid())
                .orElseThrow(() -> new EntityNotFoundException("Doctor not found with UUID: " + request.getDoctorUuid()));

        slotGenerationService.validateSlotAvailability(
                doctor,
                request.getStartTime().toLocalDate(),
                request.getStartTime().toLocalTime(),
                request.getEndTime().toLocalTime());

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

        auditLogger.logCustomEvent("APPOINTMENT_CREATED", Map.of(
                "appointmentNumber", saved.getAppointmentNumber(),
                "doctorUuid", doctor.getUuid().toString(),
                "patientUuid", patient.getUuid().toString(),
                "startTime", request.getStartTime().toString()
        ));

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

    public Page<AppointmentResponse> search(
            UUID doctorUuid, UUID patientUuid,
            AppointmentStatus status, VisitType visitType,
            LocalDate dateFrom, LocalDate dateTo,
            Pageable pageable) {

        Long resolvedDoctorId = null;
        Long resolvedPatientId = null;

        boolean hasViewAll = securityContext.hasAuthority("PERM_APPOINTMENT_VIEW_ALL");

        if (!hasViewAll) {
            boolean hasViewOwnDoctor = securityContext.hasAuthority("PERM_DOCTOR_VIEW_OWN");
            boolean hasViewOwnPatient = securityContext.hasAuthority("PERM_PATIENT_VIEW_OWN");

            if (hasViewOwnDoctor) {
                Doctor doctor = doctorRepository.findByUserIdAndDeletedAtIsNull(securityContext.getCurrentUserId()).orElse(null);
                if (doctor != null) {
                    resolvedDoctorId = doctor.getId();
                }
            } else if (hasViewOwnPatient) {
                Patient patient = patientRepository.findByUserIdAndDeletedAtIsNull(securityContext.getCurrentUserId()).orElse(null);
                if (patient != null) {
                    resolvedPatientId = patient.getId();
                }
            }
        } else {
            if (doctorUuid != null) {
                Doctor doctor = doctorRepository.findByUuidAndDeletedAtIsNull(doctorUuid)
                        .orElseThrow(() -> new EntityNotFoundException("Doctor not found with UUID: " + doctorUuid));
                resolvedDoctorId = doctor.getId();
            }
            if (patientUuid != null) {
                Patient patient = patientRepository.findByUuidAndDeletedAtIsNull(patientUuid)
                        .orElseThrow(() -> new EntityNotFoundException("Patient not found with UUID: " + patientUuid));
                resolvedPatientId = patient.getId();
            }
        }

        final Long doctorId = resolvedDoctorId;
        final Long patientId = resolvedPatientId;
        final LocalDateTime from = dateFrom != null ? dateFrom.atStartOfDay() : null;
        final LocalDateTime to = dateTo != null ? dateTo.atTime(23, 59, 59) : null;

        Specification<Appointment> spec = (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            predicates.add(cb.isNull(root.get("deletedAt")));
            if (doctorId != null) {
                predicates.add(cb.equal(root.get("doctor").get("id"), doctorId));
            }
            if (patientId != null) {
                predicates.add(cb.equal(root.get("patient").get("id"), patientId));
            }
            if (status != null) {
                predicates.add(cb.equal(root.get("status"), status));
            }
            if (visitType != null) {
                predicates.add(cb.equal(root.get("visitType"), visitType));
            }
            if (from != null) {
                predicates.add(cb.greaterThanOrEqualTo(root.get("startTime"), from));
            }
            if (to != null) {
                predicates.add(cb.lessThanOrEqualTo(root.get("endTime"), to));
            }
            return cb.and(predicates.toArray(new Predicate[0]));
        };

        return appointmentRepository.findAll(spec, pageable)
                .map(this::toResponse);
    }

    public List<AppointmentResponse> getByDate(LocalDate date) {
        return appointmentRepository.findByAppointmentDate(date).stream()
                .map(this::toResponse)
                .toList();
    }

    public List<AppointmentResponse> getByDoctor(UUID doctorUuid, LocalDate from, LocalDate to) {
        LocalDateTime fromDate = from != null ? from.atStartOfDay() : LocalDate.now().atStartOfDay();
        LocalDateTime toDate = to != null ? to.atTime(23, 59, 59) : LocalDate.now().atTime(23, 59, 59);

        return appointmentRepository.findByDoctorUuidAndDateRange(doctorUuid, fromDate, toDate).stream()
                .map(this::toResponse)
                .toList();
    }

    public List<AppointmentResponse> getByPatient(UUID patientUuid, LocalDate from, LocalDate to) {
        LocalDateTime fromDate = from != null ? from.atStartOfDay() : LocalDate.now().minusYears(1).atStartOfDay();
        LocalDateTime toDate = to != null ? to.atTime(23, 59, 59) : LocalDate.now().atTime(23, 59, 59);

        return appointmentRepository.findByPatientUuidAndDateRange(patientUuid, fromDate, toDate).stream()
                .map(this::toResponse)
                .toList();
    }

    public List<AppointmentHistoryResponse> getHistory(UUID appointmentUuid) {
        Appointment appointment = appointmentRepository.findByUuidAndDeletedAtIsNull(appointmentUuid)
                .orElseThrow(() -> new EntityNotFoundException("Appointment not found with UUID: " + appointmentUuid));

        return historyRepository.findByAppointmentUuidOrderByCreatedAtDesc(appointment.getUuid()).stream()
                .map(this::toHistoryResponse)
                .toList();
    }

    @Transactional
    public AppointmentResponse updateStatus(UUID uuid, AppointmentStatus newStatus, String cancellationReason) {
        Appointment appointment = appointmentRepository.findByUuidAndDeletedAtIsNull(uuid)
                .orElseThrow(() -> new EntityNotFoundException("Appointment not found with UUID: " + uuid));

        validateStatusTransition(appointment.getStatus(), newStatus);

        AppointmentStatus previousStatus = appointment.getStatus();
        LocalDateTime now = LocalDateTime.now();

        appointment.setStatus(newStatus);

        if (newStatus == AppointmentStatus.CHECKED_IN) {
            appointment.setCheckedInAt(now);
        } else if (newStatus == AppointmentStatus.IN_PROGRESS) {
            appointment.setInProgressAt(now);
        } else if (newStatus == AppointmentStatus.COMPLETED) {
            appointment.setCompletedAt(now);
        } else if (newStatus == AppointmentStatus.CANCELLED) {
            appointment.setCancelledAt(now);
            appointment.setCancellationReason(cancellationReason);
        } else if (newStatus == AppointmentStatus.NO_SHOW) {
            appointment.setNoShowAt(now);
        }

        appointment.setUpdatedBy(securityContext.getCurrentUserId());

        Appointment saved = appointmentRepository.save(appointment);

        logStatusChange(saved, previousStatus, newStatus, cancellationReason);

        return toResponse(saved);
    }

    @Transactional
    public AppointmentResponse reschedule(UUID uuid, LocalDateTime newStartTime, LocalDateTime newEndTime, String reason) {
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

        slotGenerationService.validateSlotAvailability(
                appointment.getDoctor(),
                newStartTime.toLocalDate(),
                newStartTime.toLocalTime(),
                newEndTime.toLocalTime());

        if (appointmentRepository.existsOverlappingDoctorAppointment(
                appointment.getDoctor().getId(), newStartTime, newEndTime, uuid)) {
            throw new IllegalArgumentException("Doctor is already booked for this time slot.");
        }

        if (appointmentRepository.existsOverlappingPatientAppointment(
                appointment.getPatient().getId(), newStartTime, newEndTime, uuid)) {
            throw new IllegalArgumentException("Patient already has another appointment during this time slot.");
        }

        AppointmentHistory history = AppointmentHistory.builder()
                .appointment(appointment)
                .appointmentNumber(appointment.getAppointmentNumber())
                .action("RESCHEDULED")
                .previousStartTime(appointment.getStartTime())
                .previousEndTime(appointment.getEndTime())
                .previousStatus(appointment.getStatus().name())
                .newStartTime(newStartTime)
                .newEndTime(newEndTime)
                .newStatus(appointment.getStatus().name())
                .reason(reason)
                .performedBy(securityContext.getCurrentUserId())
                .build();
        historyRepository.save(history);

        appointment.setStartTime(newStartTime);
        appointment.setEndTime(newEndTime);
        appointment.setAppointmentDate(newStartTime.toLocalDate());
        appointment.setUpdatedBy(securityContext.getCurrentUserId());

        Appointment saved = appointmentRepository.save(appointment);

        auditLogger.logCustomEvent("APPOINTMENT_RESCHEDULED", Map.of(
                "appointmentNumber", saved.getAppointmentNumber(),
                "previousStart", history.getPreviousStartTime().toString(),
                "newStart", newStartTime.toString()
        ));

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
                           newStatus == AppointmentStatus.NO_SHOW);
                break;
            case CONFIRMED:
                allowed = (newStatus == AppointmentStatus.CHECKED_IN ||
                           newStatus == AppointmentStatus.CANCELLED ||
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
            default:
                allowed = false;
        }

        if (!allowed) {
            throw new IllegalArgumentException("Invalid status transition from " + currentStatus + " to " + newStatus);
        }
    }

    private void logStatusChange(Appointment appointment, AppointmentStatus previousStatus, AppointmentStatus newStatus, String reason) {
        String apptNum = appointment.getAppointmentNumber();
        switch (newStatus) {
            case CONFIRMED:
                auditLogger.logCustomEvent("APPOINTMENT_CONFIRMED", Map.of("appointmentNumber", apptNum));
                break;
            case CHECKED_IN:
                auditLogger.logCustomEvent("PATIENT_CHECKED_IN", Map.of("appointmentNumber", apptNum));
                break;
            case IN_PROGRESS:
                auditLogger.logCustomEvent("APPOINTMENT_STARTED", Map.of("appointmentNumber", apptNum));
                break;
            case COMPLETED:
                auditLogger.logCustomEvent("APPOINTMENT_COMPLETED", Map.of("appointmentNumber", apptNum));
                break;
            case CANCELLED:
                auditLogger.logCustomEvent("APPOINTMENT_CANCELLED", Map.of(
                        "appointmentNumber", apptNum,
                        "reason", reason != null ? reason : ""
                ));
                break;
            case NO_SHOW:
                auditLogger.logCustomEvent("APPOINTMENT_NO_SHOW", Map.of("appointmentNumber", apptNum));
                break;
            default:
                break;
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
                .reasonForVisit(appointment.getReasonForVisit())
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

    private AppointmentHistoryResponse toHistoryResponse(AppointmentHistory history) {
        return AppointmentHistoryResponse.builder()
                .uuid(history.getUuid())
                .appointmentNumber(history.getAppointmentNumber())
                .action(history.getAction())
                .previousStartTime(history.getPreviousStartTime())
                .previousEndTime(history.getPreviousEndTime())
                .previousStatus(history.getPreviousStatus())
                .newStartTime(history.getNewStartTime())
                .newEndTime(history.getNewEndTime())
                .newStatus(history.getNewStatus())
                .reason(history.getReason())
                .performedBy(history.getPerformedBy())
                .createdAt(history.getCreatedAt())
                .build();
    }
}
