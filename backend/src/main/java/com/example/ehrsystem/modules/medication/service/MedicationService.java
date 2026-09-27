package com.example.ehrsystem.modules.medication.service;

import com.example.ehrsystem.common.security.SecurityContextAccessor;
import com.example.ehrsystem.common.util.AuditLogger;
import com.example.ehrsystem.modules.medication.dto.request.CreateMedicationRequest;
import com.example.ehrsystem.modules.medication.dto.request.UpdateMedicationRequest;
import com.example.ehrsystem.modules.medication.dto.response.MedicationResponse;
import com.example.ehrsystem.modules.medication.entity.DosageForm;
import com.example.ehrsystem.modules.medication.entity.Medication;
import com.example.ehrsystem.modules.medication.repository.MedicationRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.UUID;

/**
 * Medication catalog administration (admin-only mutations).
 * The catalog is patient/encounter independent reference data and must
 * stay terminology-ready (code + codeSystem).
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class MedicationService {

    private final MedicationRepository medicationRepository;
    private final SecurityContextAccessor securityContext;
    private final AuditLogger auditLogger;

    @Transactional
    public MedicationResponse create(CreateMedicationRequest request) {
        String code = normalize(request.getCode());
        String codeSystem = normalize(request.getCodeSystem());

        if (code != null && codeSystem != null
                && medicationRepository.existsByCodeAndCodeSystemAndDeletedAtIsNull(code, codeSystem)) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "A medication with this code already exists in the catalog.");
        }

        Long currentUserId = securityContext.getCurrentUserId();

        Medication medication = Medication.builder()
                .code(code)
                .codeSystem(codeSystem)
                .genericName(request.getGenericName().trim())
                .brandName(normalize(request.getBrandName()))
                .strength(request.getStrength())
                .strengthUnit(request.getStrengthUnit())
                .dosageForm(request.getDosageForm())
                .route(request.getRoute())
                .createdBy(currentUserId)
                .updatedBy(currentUserId)
                .build();

        Medication saved = medicationRepository.save(medication);

        auditLogger.logCustomEvent("MEDICATION_CREATED", Map.of(
                "code", saved.getCode() != null ? saved.getCode() : "NONE",
                "codeSystem", saved.getCodeSystem() != null ? saved.getCodeSystem() : "LOCAL",
                "dosageForm", saved.getDosageForm() != null ? saved.getDosageForm().name() : "NONE",
                "route", saved.getRoute() != null ? saved.getRoute().name() : "NONE",
                "active", saved.getIsActive()
        ));

        return toResponse(saved);
    }

    public MedicationResponse get(UUID medicationUuid) {
        return toResponse(requireMedication(medicationUuid));
    }

    /**
     * Paged catalog access. Active-only search is what prescribing uses;
     * includeInactive exposes the full non-deleted catalog for administration.
     */
    public Page<MedicationResponse> search(String query, DosageForm dosageForm,
                                           boolean includeInactive, Pageable pageable) {
        String term = query != null ? query.trim() : "";
        if (term.isEmpty()) {
            return medicationRepository.findAllNotDeleted(dosageForm, pageable).map(this::toResponse);
        }
        if (includeInactive) {
            return medicationRepository.searchMedications(term, pageable).map(this::toResponse);
        }
        return medicationRepository.searchActiveMedications(term, dosageForm, pageable)
                .map(this::toResponse);
    }

    @Transactional
    public MedicationResponse update(UUID medicationUuid, UpdateMedicationRequest request) {
        Medication medication = requireMedication(medicationUuid);
        assertVersion(medication, request.getVersion());

        if (request.getGenericName() != null && !request.getGenericName().isBlank()) {
            medication.setGenericName(request.getGenericName().trim());
        }
        if (request.getBrandName() != null) {
            medication.setBrandName(normalize(request.getBrandName()));
        }
        if (request.getStrength() != null) {
            medication.setStrength(request.getStrength());
        }
        if (request.getStrengthUnit() != null) {
            medication.setStrengthUnit(request.getStrengthUnit());
        }
        if (request.getDosageForm() != null) {
            medication.setDosageForm(request.getDosageForm());
        }
        if (request.getRoute() != null) {
            medication.setRoute(request.getRoute());
        }
        boolean deactivating = false;
        if (request.getIsActive() != null) {
            deactivating = Boolean.FALSE.equals(request.getIsActive())
                    && !Boolean.FALSE.equals(medication.getIsActive());
            medication.setIsActive(request.getIsActive());
        }
        medication.setUpdatedBy(securityContext.getCurrentUserId());

        Medication saved = medicationRepository.save(medication);

        auditLogger.logCustomEvent(deactivating ? "MEDICATION_DEACTIVATED" : "MEDICATION_UPDATED",
                Map.of(
                        "code", saved.getCode() != null ? saved.getCode() : "NONE",
                        "active", saved.getIsActive(),
                        "version", saved.getVersion()
                ));

        return toResponse(saved);
    }

    @Transactional
    public void deactivate(UUID medicationUuid) {
        Medication medication = requireMedication(medicationUuid);

        medication.setDeletedAt(LocalDateTime.now());
        medication.setUpdatedBy(securityContext.getCurrentUserId());
        medicationRepository.save(medication);

        auditLogger.logCustomEvent("MEDICATION_DEACTIVATED", Map.of(
                "code", medication.getCode() != null ? medication.getCode() : "NONE",
                "active", Boolean.FALSE
        ));
    }

    private Medication requireMedication(UUID uuid) {
        return medicationRepository.findByUuidAndDeletedAtIsNull(uuid)
                .orElseThrow(() -> new EntityNotFoundException(
                        "Medication not found with UUID: " + uuid));
    }

    private void assertVersion(Medication medication, Long expectedVersion) {
        if (expectedVersion != null && !expectedVersion.equals(medication.getVersion())) {
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

    private MedicationResponse toResponse(Medication medication) {
        return MedicationResponse.builder()
                .id(medication.getId())
                .uuid(medication.getUuid())
                .code(medication.getCode())
                .codeSystem(medication.getCodeSystem())
                .genericName(medication.getGenericName())
                .brandName(medication.getBrandName())
                .strength(medication.getStrength())
                .strengthUnit(medication.getStrengthUnit())
                .dosageForm(medication.getDosageForm())
                .route(medication.getRoute())
                .isActive(medication.getIsActive())
                .createdAt(medication.getCreatedAt())
                .updatedAt(medication.getUpdatedAt())
                .version(medication.getVersion())
                .build();
    }
}
