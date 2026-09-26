package com.example.ehrsystem.modules.encounter.service;

import com.example.ehrsystem.common.security.SecurityContextAccessor;
import com.example.ehrsystem.common.util.AuditLogger;
import com.example.ehrsystem.modules.appointment.entity.Appointment;
import com.example.ehrsystem.modules.appointment.entity.AppointmentStatus;
import com.example.ehrsystem.modules.appointment.entity.VisitType;
import com.example.ehrsystem.modules.appointment.repository.AppointmentRepository;
import com.example.ehrsystem.modules.appointment.service.AppointmentService;
import com.example.ehrsystem.modules.doctor.entity.Doctor;
import com.example.ehrsystem.modules.doctor.repository.DoctorRepository;
import com.example.ehrsystem.modules.encounter.dto.request.CancelEncounterRequest;
import com.example.ehrsystem.modules.encounter.dto.request.CreateEncounterRequest;
import com.example.ehrsystem.modules.encounter.dto.request.UpdateEncounterRequest;
import com.example.ehrsystem.modules.encounter.dto.response.EncounterResponse;
import com.example.ehrsystem.modules.encounter.entity.Encounter;
import com.example.ehrsystem.modules.encounter.entity.EncounterStatus;
import com.example.ehrsystem.modules.encounter.repository.EncounterRepository;
import com.example.ehrsystem.modules.patient.entity.Patient;
import com.example.ehrsystem.modules.patient.repository.PatientRepository;
import jakarta.persistence.EntityNotFoundException;
import jakarta.persistence.criteria.Predicate;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class EncounterService {

    private final EncounterRepository encounterRepository;
    private final AppointmentRepository appointmentRepository;
    private final AppointmentService appointmentService;
    private final PatientRepository patientRepository;
    private final DoctorRepository doctorRepository;
    private final EncounterNumberService encounterNumberService;
    private final EncounterStatusTransitionService statusTransitionService;
    private final SecurityContextAccessor securityContext;
    private final AuditLogger auditLogger;

    @Transactional
    public EncounterResponse create(CreateEncounterRequest request) {
        Appointment appointment = appointmentRepository.findByUuidAndDeletedAtIsNull(request.getAppointmentUuid())
                .orElseThrow(() -> new EntityNotFoundException("Appointment not found with UUID: " + request.getAppointmentUuid()));

        if (appointment.getStatus() != AppointmentStatus.CHECKED_IN &&
            appointment.getStatus() != AppointmentStatus.IN_PROGRESS) {
            throw new IllegalArgumentException("Appointment is not eligible for encounter creation");
        }

        Patient patient = appointment.getPatient();
        if (patient == null || patient.getDeletedAt() != null || !"ACTIVE".equals(patient.getStatus())) {
            throw new IllegalArgumentException("Patient is not active");
        }

        Doctor doctor = appointment.getDoctor();
        if (doctor == null || doctor.getDeletedAt() != null || !"ACTIVE".equals(doctor.getStatus())) {
            throw new IllegalArgumentException("Doctor is not active");
        }

        if (encounterRepository.existsByAppointmentIdAndDeletedAtIsNullAndStatusNot(appointment.getId(), EncounterStatus.CANCELLED)) {
            throw new IllegalArgumentException("Encounter already exists for this appointment");
        }

        Long currentUserId = securityContext.getCurrentUserId();
        LocalDateTime now = LocalDateTime.now();

        Encounter encounter = Encounter.builder()
                .patient(patient)
                .doctor(doctor)
                .appointment(appointment)
                .encounterType(request.getEncounterType() != null ? request.getEncounterType() : appointment.getVisitType())
                .status(EncounterStatus.DRAFT)
                .chiefComplaint(normalize(request.getChiefComplaint()))
                .historyOfPresentIllness(normalize(request.getHistoryOfPresentIllness()))
                .createdBy(currentUserId)
                .updatedBy(currentUserId)
                .build();

        encounter.setEncounterNumber(encounterNumberService.generateEncounterNumber());

        statusTransitionService.validate(encounter.getStatus(), EncounterStatus.IN_PROGRESS);
        encounter.setStatus(EncounterStatus.IN_PROGRESS);
        encounter.setStartedAt(now);

        Encounter saved = encounterRepository.save(encounter);

        if (appointment.getStatus() == AppointmentStatus.CHECKED_IN) {
            appointmentService.updateStatus(appointment.getUuid(), AppointmentStatus.IN_PROGRESS, null);
        }

        auditLogger.logCustomEvent("ENCOUNTER_CREATED", Map.of(
                "encounterNumber", saved.getEncounterNumber(),
                "appointmentNumber", appointment.getAppointmentNumber(),
                "encounterType", saved.getEncounterType().name()
        ));
        auditLogger.logCustomEvent("ENCOUNTER_STARTED", Map.of(
                "encounterNumber", saved.getEncounterNumber()
        ));

        return toResponse(saved);
    }

    public EncounterResponse getByUuid(UUID uuid) {
        return toResponse(requireEncounter(uuid));
    }

    public EncounterResponse getByEncounterNumber(String encounterNumber) {
        Encounter encounter = encounterRepository.findByEncounterNumberAndDeletedAtIsNull(encounterNumber)
                .orElseThrow(() -> new EntityNotFoundException(
                        "Encounter not found with encounter number: " + encounterNumber));
        return toResponse(encounter);
    }

    public Page<EncounterResponse> search(
            UUID patientUuid, UUID doctorUuid, UUID appointmentUuid,
            EncounterStatus status, VisitType encounterType,
            LocalDate dateFrom, LocalDate dateTo,
            Pageable pageable) {

        Long filterDoctorId = null;
        Long filterPatientId = null;
        Long scopeDoctorId = null;
        Long scopePatientId = null;

        if (doctorUuid != null) {
            filterDoctorId = doctorRepository.findByUuidAndDeletedAtIsNull(doctorUuid)
                    .orElseThrow(() -> new EntityNotFoundException("Doctor not found with UUID: " + doctorUuid))
                    .getId();
        }
        if (patientUuid != null) {
            filterPatientId = patientRepository.findByUuidAndDeletedAtIsNull(patientUuid)
                    .orElseThrow(() -> new EntityNotFoundException("Patient not found with UUID: " + patientUuid))
                    .getId();
        }

        boolean hasViewAll = securityContext.hasAuthority("PERM_ENCOUNTER_VIEW_ALL");
        if (!hasViewAll) {
            if (securityContext.hasAuthority("PERM_DOCTOR_VIEW_OWN")) {
                scopeDoctorId = doctorRepository.findByUserIdAndDeletedAtIsNull(securityContext.getCurrentUserId())
                        .map(Doctor::getId)
                        .orElse(null);
            } else if (securityContext.hasAuthority("PERM_PATIENT_VIEW_OWN")) {
                scopePatientId = patientRepository.findByUserIdAndDeletedAtIsNull(securityContext.getCurrentUserId())
                        .map(Patient::getId)
                        .orElse(null);
            }
        }

        final Long doctorId = filterDoctorId;
        final Long patientId = filterPatientId;
        final Long doctorScopeId = scopeDoctorId;
        final Long patientScopeId = scopePatientId;
        final LocalDateTime from = dateFrom != null ? dateFrom.atStartOfDay() : null;
        final LocalDateTime to = dateTo != null ? dateTo.atTime(23, 59, 59) : null;

        Specification<Encounter> spec = (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            predicates.add(cb.isNull(root.get("deletedAt")));
            if (doctorId != null) {
                predicates.add(cb.equal(root.get("doctor").get("id"), doctorId));
            }
            if (patientId != null) {
                predicates.add(cb.equal(root.get("patient").get("id"), patientId));
            }
            if (doctorScopeId != null) {
                predicates.add(cb.equal(root.get("doctor").get("id"), doctorScopeId));
            }
            if (patientScopeId != null) {
                predicates.add(cb.equal(root.get("patient").get("id"), patientScopeId));
            }
            if (appointmentUuid != null) {
                predicates.add(cb.equal(root.get("appointment").get("uuid"), appointmentUuid));
            }
            if (status != null) {
                predicates.add(cb.equal(root.get("status"), status));
            }
            if (encounterType != null) {
                predicates.add(cb.equal(root.get("encounterType"), encounterType));
            }
            if (from != null) {
                predicates.add(cb.greaterThanOrEqualTo(root.get("startedAt"), from));
            }
            if (to != null) {
                predicates.add(cb.lessThanOrEqualTo(root.get("startedAt"), to));
            }
            return cb.and(predicates.toArray(new Predicate[0]));
        };

        return encounterRepository.findAll(spec, pageable).map(this::toResponse);
    }

    @Transactional
    public EncounterResponse start(UUID uuid) {
        Encounter encounter = requireEncounter(uuid);

        if (encounter.getStatus() == EncounterStatus.IN_PROGRESS) {
            return toResponse(encounter);
        }

        statusTransitionService.validate(encounter.getStatus(), EncounterStatus.IN_PROGRESS);

        Long currentUserId = securityContext.getCurrentUserId();
        encounter.setStatus(EncounterStatus.IN_PROGRESS);
        encounter.setStartedAt(encounter.getStartedAt() != null ? encounter.getStartedAt() : LocalDateTime.now());
        encounter.setUpdatedBy(currentUserId);

        Encounter saved = encounterRepository.save(encounter);

        Appointment appointment = saved.getAppointment();
        if (appointment.getStatus() == AppointmentStatus.CHECKED_IN) {
            appointmentService.updateStatus(appointment.getUuid(), AppointmentStatus.IN_PROGRESS, null);
        }

        auditLogger.logCustomEvent("ENCOUNTER_STARTED", Map.of(
                "encounterNumber", saved.getEncounterNumber()
        ));

        return toResponse(saved);
    }

    @Transactional
    public EncounterResponse update(UUID uuid, UpdateEncounterRequest request) {
        Encounter encounter = requireEncounter(uuid);
        assertModifiable(encounter);
        assertVersion(encounter, request.getVersion());

        applyDocumentation(encounter, request);
        encounter.setUpdatedBy(securityContext.getCurrentUserId());

        Encounter saved = encounterRepository.save(encounter);

        auditLogger.logCustomEvent("ENCOUNTER_UPDATED", Map.of(
                "encounterNumber", saved.getEncounterNumber()
        ));

        return toResponse(saved);
    }

    @Transactional
    public EncounterResponse complete(UUID uuid, UpdateEncounterRequest request) {
        Encounter encounter = requireEncounter(uuid);

        if (encounter.getStatus() == EncounterStatus.COMPLETED) {
            throw new IllegalArgumentException("Encounter is already completed");
        }
        if (request != null) {
            assertVersion(encounter, request.getVersion());
            applyDocumentation(encounter, request);
        }

        if (encounter.getChiefComplaint() == null || encounter.getChiefComplaint().isBlank()) {
            throw new IllegalArgumentException("Chief complaint is required to complete the encounter");
        }

        statusTransitionService.validate(encounter.getStatus(), EncounterStatus.COMPLETED);

        LocalDateTime now = LocalDateTime.now();
        encounter.setStatus(EncounterStatus.COMPLETED);
        encounter.setEndedAt(now);
        encounter.setUpdatedBy(securityContext.getCurrentUserId());

        Encounter saved = encounterRepository.save(encounter);

        Appointment appointment = saved.getAppointment();
        if (appointment.getStatus() == AppointmentStatus.IN_PROGRESS) {
            appointmentService.updateStatus(appointment.getUuid(), AppointmentStatus.COMPLETED, null);
        }

        auditLogger.logCustomEvent("ENCOUNTER_COMPLETED", Map.of(
                "encounterNumber", saved.getEncounterNumber()
        ));

        return toResponse(saved);
    }

    @Transactional
    public EncounterResponse cancel(UUID uuid, CancelEncounterRequest request) {
        Encounter encounter = requireEncounter(uuid);

        if (encounter.getStatus() == EncounterStatus.CANCELLED) {
            throw new IllegalArgumentException("Encounter is already cancelled");
        }

        statusTransitionService.validate(encounter.getStatus(), EncounterStatus.CANCELLED);

        encounter.setStatus(EncounterStatus.CANCELLED);
        encounter.setCancellationReason(request.getReason().trim());
        encounter.setUpdatedBy(securityContext.getCurrentUserId());

        Encounter saved = encounterRepository.save(encounter);

        auditLogger.logCustomEvent("ENCOUNTER_CANCELLED", Map.of(
                "encounterNumber", saved.getEncounterNumber()
        ));

        return toResponse(saved);
    }

    private Encounter requireEncounter(UUID uuid) {
        return encounterRepository.findByUuidAndDeletedAtIsNull(uuid)
                .orElseThrow(() -> new EntityNotFoundException("Encounter not found with UUID: " + uuid));
    }

    private void assertModifiable(Encounter encounter) {
        if (encounter.getStatus() == EncounterStatus.COMPLETED) {
            throw new IllegalArgumentException("Cannot modify completed encounter");
        }
        if (encounter.getStatus() == EncounterStatus.CANCELLED) {
            throw new IllegalArgumentException("Cannot modify cancelled encounter");
        }
    }

    private void assertVersion(Encounter encounter, Long expectedVersion) {
        if (expectedVersion != null && !expectedVersion.equals(encounter.getVersion())) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "Encounter has been modified by another user. Please refresh and try again.");
        }
    }

    private void applyDocumentation(Encounter encounter, UpdateEncounterRequest request) {
        if (request.getChiefComplaint() != null) {
            encounter.setChiefComplaint(normalize(request.getChiefComplaint()));
        }
        if (request.getHistoryOfPresentIllness() != null) {
            encounter.setHistoryOfPresentIllness(normalize(request.getHistoryOfPresentIllness()));
        }
        if (request.getClinicalNotes() != null) {
            encounter.setClinicalNotes(normalize(request.getClinicalNotes()));
        }
        if (request.getAssessment() != null) {
            encounter.setAssessment(normalize(request.getAssessment()));
        }
        if (request.getTreatmentPlan() != null) {
            encounter.setTreatmentPlan(normalize(request.getTreatmentPlan()));
        }
        if (request.getFollowUpNotes() != null) {
            encounter.setFollowUpNotes(normalize(request.getFollowUpNotes()));
        }
    }

    private String normalize(String value) {
        if (value == null) return null;
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    private EncounterResponse toResponse(Encounter encounter) {
        Patient patient = encounter.getPatient();
        Doctor doctor = encounter.getDoctor();
        Appointment appointment = encounter.getAppointment();

        return EncounterResponse.builder()
                .id(encounter.getId())
                .uuid(encounter.getUuid())
                .encounterNumber(encounter.getEncounterNumber())
                .patientUuid(patient.getUuid())
                .patientName(patient.getFullName())
                .patientMrn(patient.getMrn())
                .patientGender(patient.getGender())
                .patientDateOfBirth(patient.getDateOfBirth())
                .doctorUuid(doctor.getUuid())
                .doctorName(doctor.getFullName())
                .doctorCode(doctor.getDoctorCode())
                .appointmentUuid(appointment.getUuid())
                .appointmentNumber(appointment.getAppointmentNumber())
                .encounterType(encounter.getEncounterType())
                .status(encounter.getStatus())
                .startedAt(encounter.getStartedAt())
                .endedAt(encounter.getEndedAt())
                .chiefComplaint(encounter.getChiefComplaint())
                .historyOfPresentIllness(encounter.getHistoryOfPresentIllness())
                .clinicalNotes(encounter.getClinicalNotes())
                .assessment(encounter.getAssessment())
                .treatmentPlan(encounter.getTreatmentPlan())
                .followUpNotes(encounter.getFollowUpNotes())
                .cancellationReason(encounter.getCancellationReason())
                .createdAt(encounter.getCreatedAt())
                .updatedAt(encounter.getUpdatedAt())
                .version(encounter.getVersion())
                .build();
    }
}
