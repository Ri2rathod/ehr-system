package com.example.ehrsystem.modules.prescription.service;

import com.example.ehrsystem.common.security.SecurityContextAccessor;
import com.example.ehrsystem.common.util.AuditLogger;
import com.example.ehrsystem.modules.encounter.entity.Encounter;
import com.example.ehrsystem.modules.encounter.entity.EncounterStatus;
import com.example.ehrsystem.modules.encounter.repository.EncounterRepository;
import com.example.ehrsystem.modules.prescription.dto.request.CreatePrescriptionRequest;
import com.example.ehrsystem.modules.prescription.dto.request.UpdatePrescriptionRequest;
import com.example.ehrsystem.modules.prescription.dto.response.PrescriptionResponse;
import com.example.ehrsystem.modules.prescription.entity.Prescription;
import com.example.ehrsystem.modules.prescription.entity.PrescriptionStatus;
import com.example.ehrsystem.modules.prescription.repository.PrescriptionItemRepository;
import com.example.ehrsystem.modules.prescription.repository.PrescriptionRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Prescriptions are a child clinical domain of Encounter.
 * patient and doctor are always derived from the Encounter - never
 * accepted from the client. Lifecycle rules (transitions, encounter
 * modifiability, concurrency) are enforced here - never in the controller.
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class PrescriptionService {

    private final PrescriptionRepository prescriptionRepository;
    private final PrescriptionItemRepository prescriptionItemRepository;
    private final EncounterRepository encounterRepository;
    private final PrescriptionNumberService prescriptionNumberService;
    private final SecurityContextAccessor securityContext;
    private final AuditLogger auditLogger;
    private final PrescriptionStatusTransitionService transitionService;

    @Transactional
    public PrescriptionResponse create(UUID encounterUuid, CreatePrescriptionRequest request) {
        Encounter encounter = requireEncounter(encounterUuid);
        assertEncounterModifiable(encounter);

        Long currentUserId = securityContext.getCurrentUserId();

        Prescription prescription = Prescription.builder()
                .prescriptionNumber(prescriptionNumberService.generatePrescriptionNumber())
                .encounter(encounter)
                .patient(encounter.getPatient())
                .doctor(encounter.getDoctor())
                .notes(normalize(request.getNotes()))
                .createdBy(currentUserId)
                .updatedBy(currentUserId)
                .build();

        Prescription saved = prescriptionRepository.save(prescription);

        auditLogger.logCustomEvent("PRESCRIPTION_CREATED", Map.of(
                "encounterNumber", encounter.getEncounterNumber(),
                "status", saved.getStatus().name()
        ));

        return toResponse(saved);
    }

    public List<PrescriptionResponse> list(UUID encounterUuid) {
        Encounter encounter = requireEncounter(encounterUuid);
        return prescriptionRepository
                .findByEncounterIdAndDeletedAtIsNullOrderByCreatedAtDesc(encounter.getId()).stream()
                .map(this::toResponse)
                .toList();
    }

    public PrescriptionResponse get(UUID encounterUuid, UUID prescriptionUuid) {
        Encounter encounter = requireEncounter(encounterUuid);
        return toResponse(requirePrescription(encounter, prescriptionUuid));
    }

    @Transactional
    public PrescriptionResponse update(UUID encounterUuid, UUID prescriptionUuid,
                                       UpdatePrescriptionRequest request) {
        Encounter encounter = requireEncounter(encounterUuid);
        Prescription prescription = requirePrescription(encounter, prescriptionUuid);
        assertEncounterModifiable(encounter);
        assertVersion(prescription, request.getVersion());

        if (request.getNotes() != null) {
            prescription.setNotes(normalize(request.getNotes()));
        }
        prescription.setUpdatedBy(securityContext.getCurrentUserId());

        Prescription saved = prescriptionRepository.save(prescription);

        auditLogger.logCustomEvent("PRESCRIPTION_UPDATED", Map.of(
                "encounterNumber", encounter.getEncounterNumber(),
                "status", saved.getStatus().name()
        ));

        return toResponse(saved);
    }

    @Transactional
    public PrescriptionResponse activate(UUID encounterUuid, UUID prescriptionUuid) {
        Encounter encounter = requireEncounter(encounterUuid);
        Prescription prescription = requirePrescription(encounter, prescriptionUuid);
        assertEncounterModifiable(encounter);

        if (prescription.getStatus() == PrescriptionStatus.ACTIVE) {
            return toResponse(prescription);
        }
        // Terminal/invalid states win over the empty-items guard so callers
        // always see the real status error first.
        transitionService.validate(prescription.getStatus(), PrescriptionStatus.ACTIVE);
        if (prescriptionItemRepository.countByPrescriptionIdAndDeletedAtIsNull(prescription.getId()) < 1) {
            throw new IllegalArgumentException(
                    "Cannot activate a prescription without medication items");
        }
        return applyTransition(encounter, prescription, PrescriptionStatus.ACTIVE,
                "PRESCRIPTION_ACTIVATED");
    }

    @Transactional
    public PrescriptionResponse complete(UUID encounterUuid, UUID prescriptionUuid) {
        Encounter encounter = requireEncounter(encounterUuid);
        Prescription prescription = requirePrescription(encounter, prescriptionUuid);
        assertEncounterModifiable(encounter);

        if (prescription.getStatus() == PrescriptionStatus.COMPLETED) {
            throw new IllegalArgumentException("Prescription is already completed");
        }
        return applyTransition(encounter, prescription, PrescriptionStatus.COMPLETED,
                "PRESCRIPTION_COMPLETED");
    }

    @Transactional
    public PrescriptionResponse cancel(UUID encounterUuid, UUID prescriptionUuid) {
        Encounter encounter = requireEncounter(encounterUuid);
        Prescription prescription = requirePrescription(encounter, prescriptionUuid);
        assertEncounterModifiable(encounter);

        if (prescription.getStatus() == PrescriptionStatus.CANCELLED) {
            throw new IllegalArgumentException("Prescription is already cancelled");
        }
        return applyTransition(encounter, prescription, PrescriptionStatus.CANCELLED,
                "PRESCRIPTION_CANCELLED");
    }

    @Transactional
    public PrescriptionResponse voidPrescription(UUID encounterUuid, UUID prescriptionUuid) {
        Encounter encounter = requireEncounter(encounterUuid);
        Prescription prescription = requirePrescription(encounter, prescriptionUuid);
        assertEncounterModifiable(encounter);

        if (prescription.getStatus() == PrescriptionStatus.VOID) {
            throw new IllegalArgumentException("Prescription is already voided");
        }
        return applyTransition(encounter, prescription, PrescriptionStatus.VOID,
                "PRESCRIPTION_VOIDED");
    }

    /**
     * Soft delete. Completed prescriptions are preserved as clinical
     * history. Items of a deleted prescription are soft deleted with it
     * so no orphan rows remain reachable through normal queries.
     */
    @Transactional
    public void delete(UUID encounterUuid, UUID prescriptionUuid) {
        Encounter encounter = requireEncounter(encounterUuid);
        Prescription prescription = requirePrescription(encounter, prescriptionUuid);
        assertEncounterModifiable(encounter);

        if (prescription.getStatus() == PrescriptionStatus.COMPLETED) {
            throw new IllegalArgumentException("Cannot delete a completed prescription");
        }

        LocalDateTime now = LocalDateTime.now();
        Long currentUserId = securityContext.getCurrentUserId();

        var items = prescriptionItemRepository
                .findByPrescriptionIdAndDeletedAtIsNullOrderByCreatedAtAsc(prescription.getId());
        for (var item : items) {
            item.setDeletedAt(now);
            item.setUpdatedBy(currentUserId);
        }
        prescriptionItemRepository.saveAll(items);

        prescription.setDeletedAt(now);
        prescription.setUpdatedBy(currentUserId);
        prescriptionRepository.save(prescription);

        auditLogger.logCustomEvent("PRESCRIPTION_DELETED", Map.of(
                "encounterNumber", encounter.getEncounterNumber(),
                "status", prescription.getStatus().name()
        ));
    }

    private PrescriptionResponse applyTransition(Encounter encounter, Prescription prescription,
                                                 PrescriptionStatus target, String auditEvent) {
        transitionService.validate(prescription.getStatus(), target);
        prescription.setStatus(target);
        prescription.setUpdatedBy(securityContext.getCurrentUserId());

        Prescription saved = prescriptionRepository.save(prescription);

        auditLogger.logCustomEvent(auditEvent, Map.of(
                "encounterNumber", encounter.getEncounterNumber(),
                "status", saved.getStatus().name()
        ));

        return toResponse(saved);
    }

    private Encounter requireEncounter(UUID uuid) {
        return encounterRepository.findByUuidAndDeletedAtIsNull(uuid)
                .orElseThrow(() -> new EntityNotFoundException("Encounter not found with UUID: " + uuid));
    }

    private Prescription requirePrescription(Encounter encounter, UUID prescriptionUuid) {
        return prescriptionRepository
                .findByUuidAndEncounterIdAndDeletedAtIsNull(prescriptionUuid, encounter.getId())
                .orElseThrow(() -> new EntityNotFoundException(
                        "Prescription not found with UUID: " + prescriptionUuid));
    }

    private void assertEncounterModifiable(Encounter encounter) {
        if (encounter.getStatus() == EncounterStatus.COMPLETED) {
            throw new IllegalArgumentException("Cannot modify prescriptions for a completed encounter");
        }
        if (encounter.getStatus() == EncounterStatus.CANCELLED) {
            throw new IllegalArgumentException("Cannot modify prescriptions for a cancelled encounter");
        }
    }

    private void assertVersion(Prescription prescription, Long expectedVersion) {
        if (expectedVersion != null && !expectedVersion.equals(prescription.getVersion())) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "This clinical record was updated by another user. Reload to continue.");
        }
    }

    private String normalize(String value) {
        if (value == null) return null;
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    private PrescriptionResponse toResponse(Prescription prescription) {
        return PrescriptionResponse.builder()
                .id(prescription.getId())
                .uuid(prescription.getUuid())
                .encounterUuid(prescription.getEncounter().getUuid())
                .patientUuid(prescription.getPatient().getUuid())
                .doctorUuid(prescription.getDoctor() != null
                        ? prescription.getDoctor().getUuid() : null)
                .prescriptionNumber(prescription.getPrescriptionNumber())
                .status(prescription.getStatus())
                .prescribedAt(prescription.getPrescribedAt())
                .notes(prescription.getNotes())
                .createdAt(prescription.getCreatedAt())
                .updatedAt(prescription.getUpdatedAt())
                .version(prescription.getVersion())
                .build();
    }
}
