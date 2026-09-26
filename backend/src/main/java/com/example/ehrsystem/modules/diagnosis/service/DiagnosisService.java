package com.example.ehrsystem.modules.diagnosis.service;

import com.example.ehrsystem.common.security.SecurityContextAccessor;
import com.example.ehrsystem.common.util.AuditLogger;
import com.example.ehrsystem.modules.diagnosis.dto.request.CreateDiagnosisRequest;
import com.example.ehrsystem.modules.diagnosis.dto.request.UpdateDiagnosisRequest;
import com.example.ehrsystem.modules.diagnosis.dto.response.DiagnosisResponse;
import com.example.ehrsystem.modules.diagnosis.entity.Diagnosis;
import com.example.ehrsystem.modules.diagnosis.entity.DiagnosisStatus;
import com.example.ehrsystem.modules.diagnosis.entity.DiagnosisType;
import com.example.ehrsystem.modules.diagnosis.repository.DiagnosisRepository;
import com.example.ehrsystem.modules.encounter.entity.Encounter;
import com.example.ehrsystem.modules.encounter.entity.EncounterStatus;
import com.example.ehrsystem.modules.encounter.repository.EncounterRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

/**
 * Diagnoses are a child clinical domain of Encounter.
 * All rules (encounter lifecycle, primary invariant, dates, concurrency)
 * are enforced here — never in the controller.
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class DiagnosisService {

    private final DiagnosisRepository diagnosisRepository;
    private final EncounterRepository encounterRepository;
    private final SecurityContextAccessor securityContext;
    private final AuditLogger auditLogger;

    @Transactional
    public DiagnosisResponse create(UUID encounterUuid, CreateDiagnosisRequest request) {
        Encounter encounter = requireEncounter(encounterUuid);
        assertEncounterModifiable(encounter);

        DiagnosisType diagnosisType = request.getDiagnosisType() != null
                ? request.getDiagnosisType() : DiagnosisType.SECONDARY;
        DiagnosisStatus clinicalStatus = request.getClinicalStatus() != null
                ? request.getClinicalStatus() : DiagnosisStatus.ACTIVE;

        validateDates(request.getOnsetDate(), null);

        Long currentUserId = securityContext.getCurrentUserId();

        if (diagnosisType == DiagnosisType.PRIMARY) {
            Optional<Diagnosis> existingPrimary = diagnosisRepository
                    .findFirstByEncounterIdAndDiagnosisTypeAndDeletedAtIsNull(
                            encounter.getId(), DiagnosisType.PRIMARY);

            if (existingPrimary.isPresent()) {
                if (Boolean.TRUE.equals(request.getReplacePrimary())) {
                    Diagnosis previous = existingPrimary.get();
                    previous.setDiagnosisType(DiagnosisType.SECONDARY);
                    previous.setUpdatedBy(currentUserId);
                    // Flush the demotion first: Hibernate flushes inserts before
                    // updates, and the new PRIMARY would otherwise hit the
                    // partial unique index while the old primary still exists.
                    diagnosisRepository.saveAndFlush(previous);

                    auditLogger.logCustomEvent("PRIMARY_DIAGNOSIS_CHANGED", Map.of(
                            "encounterNumber", encounter.getEncounterNumber(),
                            "action", "REPLACED"
                    ));
                } else {
                    throw new ResponseStatusException(
                            HttpStatus.CONFLICT,
                            "A primary diagnosis already exists for this encounter.");
                }
            }
        }

        Diagnosis diagnosis = Diagnosis.builder()
                .encounter(encounter)
                .code(normalize(request.getCode()))
                .codeSystem(normalize(request.getCodeSystem()))
                .name(request.getName().trim())
                .diagnosisType(diagnosisType)
                .clinicalStatus(clinicalStatus)
                .onsetDate(request.getOnsetDate())
                .notes(normalize(request.getNotes()))
                .createdBy(currentUserId)
                .updatedBy(currentUserId)
                .build();

        Diagnosis saved = diagnosisRepository.save(diagnosis);

        auditLogger.logCustomEvent("DIAGNOSIS_CREATED", Map.of(
                "encounterNumber", encounter.getEncounterNumber(),
                "type", saved.getDiagnosisType().name(),
                "clinicalStatus", saved.getClinicalStatus().name(),
                "codeSystem", saved.getCodeSystem() != null ? saved.getCodeSystem() : "MANUAL"
        ));

        return toResponse(saved);
    }

    public List<DiagnosisResponse> list(UUID encounterUuid) {
        Encounter encounter = requireEncounter(encounterUuid);
        return diagnosisRepository.findByEncounterIdAndDeletedAtIsNull(encounter.getId()).stream()
                .sorted(Comparator
                        .comparing((Diagnosis d) ->
                                d.getDiagnosisType() == DiagnosisType.PRIMARY ? 0 : 1)
                        .thenComparing(Diagnosis::getCreatedAt))
                .map(this::toResponse)
                .toList();
    }

    public DiagnosisResponse get(UUID encounterUuid, UUID diagnosisUuid) {
        Encounter encounter = requireEncounter(encounterUuid);
        return toResponse(requireDiagnosis(encounter, diagnosisUuid));
    }

    @Transactional
    public DiagnosisResponse update(UUID encounterUuid, UUID diagnosisUuid, UpdateDiagnosisRequest request) {
        Encounter encounter = requireEncounter(encounterUuid);
        Diagnosis diagnosis = requireDiagnosis(encounter, diagnosisUuid);
        assertEncounterModifiable(encounter);
        assertVersion(diagnosis, request.getVersion());

        LocalDate effectiveOnset = request.getOnsetDate() != null
                ? request.getOnsetDate() : diagnosis.getOnsetDate();
        LocalDate effectiveResolved = request.getResolvedDate() != null
                ? request.getResolvedDate() : diagnosis.getResolvedDate();
        validateDates(effectiveOnset, effectiveResolved);

        if (request.getName() != null && !request.getName().isBlank()) {
            diagnosis.setName(request.getName().trim());
        }
        if (request.getClinicalStatus() != null) {
            diagnosis.setClinicalStatus(request.getClinicalStatus());
        }
        if (request.getOnsetDate() != null) {
            diagnosis.setOnsetDate(request.getOnsetDate());
        }
        if (request.getResolvedDate() != null) {
            diagnosis.setResolvedDate(request.getResolvedDate());
        }
        if (request.getNotes() != null) {
            diagnosis.setNotes(normalize(request.getNotes()));
        }
        diagnosis.setUpdatedBy(securityContext.getCurrentUserId());

        Diagnosis saved = diagnosisRepository.save(diagnosis);

        auditLogger.logCustomEvent("DIAGNOSIS_UPDATED", Map.of(
                "encounterNumber", encounter.getEncounterNumber(),
                "type", saved.getDiagnosisType().name(),
                "clinicalStatus", saved.getClinicalStatus().name()
        ));

        return toResponse(saved);
    }

    @Transactional
    public void deactivate(UUID encounterUuid, UUID diagnosisUuid) {
        Encounter encounter = requireEncounter(encounterUuid);
        Diagnosis diagnosis = requireDiagnosis(encounter, diagnosisUuid);
        assertEncounterModifiable(encounter);

        diagnosis.setDeletedAt(LocalDateTime.now());
        diagnosis.setUpdatedBy(securityContext.getCurrentUserId());
        diagnosisRepository.save(diagnosis);

        auditLogger.logCustomEvent("DIAGNOSIS_DEACTIVATED", Map.of(
                "encounterNumber", encounter.getEncounterNumber(),
                "type", diagnosis.getDiagnosisType().name()
        ));
    }

    private Encounter requireEncounter(UUID uuid) {
        return encounterRepository.findByUuidAndDeletedAtIsNull(uuid)
                .orElseThrow(() -> new EntityNotFoundException("Encounter not found with UUID: " + uuid));
    }

    private Diagnosis requireDiagnosis(Encounter encounter, UUID diagnosisUuid) {
        return diagnosisRepository
                .findByUuidAndEncounterIdAndDeletedAtIsNull(diagnosisUuid, encounter.getId())
                .orElseThrow(() -> new EntityNotFoundException(
                        "Diagnosis not found with UUID: " + diagnosisUuid));
    }

    private void assertEncounterModifiable(Encounter encounter) {
        if (encounter.getStatus() == EncounterStatus.COMPLETED) {
            throw new IllegalArgumentException("Cannot modify diagnoses for a completed encounter");
        }
        if (encounter.getStatus() == EncounterStatus.CANCELLED) {
            throw new IllegalArgumentException("Cannot modify diagnoses for a cancelled encounter");
        }
    }

    private void assertVersion(Diagnosis diagnosis, Long expectedVersion) {
        if (expectedVersion != null && !expectedVersion.equals(diagnosis.getVersion())) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "This clinical record was updated by another user. Reload to continue.");
        }
    }

    private void validateDates(LocalDate onsetDate, LocalDate resolvedDate) {
        if (onsetDate != null && resolvedDate != null && resolvedDate.isBefore(onsetDate)) {
            throw new IllegalArgumentException("Resolved date must be on or after onset date");
        }
    }

    private String normalize(String value) {
        if (value == null) return null;
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    private DiagnosisResponse toResponse(Diagnosis diagnosis) {
        return DiagnosisResponse.builder()
                .id(diagnosis.getId())
                .uuid(diagnosis.getUuid())
                .encounterUuid(diagnosis.getEncounter().getUuid())
                .code(diagnosis.getCode())
                .codeSystem(diagnosis.getCodeSystem())
                .name(diagnosis.getName())
                .diagnosisType(diagnosis.getDiagnosisType())
                .clinicalStatus(diagnosis.getClinicalStatus())
                .onsetDate(diagnosis.getOnsetDate())
                .resolvedDate(diagnosis.getResolvedDate())
                .notes(diagnosis.getNotes())
                .createdAt(diagnosis.getCreatedAt())
                .updatedAt(diagnosis.getUpdatedAt())
                .version(diagnosis.getVersion())
                .build();
    }
}
