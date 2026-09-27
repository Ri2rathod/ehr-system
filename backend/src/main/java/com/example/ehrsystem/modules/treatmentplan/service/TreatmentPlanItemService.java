package com.example.ehrsystem.modules.treatmentplan.service;

import com.example.ehrsystem.common.security.SecurityContextAccessor;
import com.example.ehrsystem.common.util.AuditLogger;
import com.example.ehrsystem.modules.diagnosis.entity.Diagnosis;
import com.example.ehrsystem.modules.diagnosis.repository.DiagnosisRepository;
import com.example.ehrsystem.modules.encounter.entity.Encounter;
import com.example.ehrsystem.modules.encounter.entity.EncounterStatus;
import com.example.ehrsystem.modules.encounter.repository.EncounterRepository;
import com.example.ehrsystem.modules.treatmentplan.dto.request.CreateTreatmentPlanItemRequest;
import com.example.ehrsystem.modules.treatmentplan.dto.request.UpdateTreatmentPlanItemRequest;
import com.example.ehrsystem.modules.treatmentplan.dto.response.TreatmentPlanItemResponse;
import com.example.ehrsystem.modules.treatmentplan.entity.DurationUnit;
import com.example.ehrsystem.modules.treatmentplan.entity.TreatmentItemStatus;
import com.example.ehrsystem.modules.treatmentplan.entity.TreatmentPlan;
import com.example.ehrsystem.modules.treatmentplan.entity.TreatmentPlanItem;
import com.example.ehrsystem.modules.treatmentplan.entity.TreatmentPlanStatus;
import com.example.ehrsystem.modules.treatmentplan.entity.TreatmentPriority;
import com.example.ehrsystem.modules.treatmentplan.repository.TreatmentPlanItemRepository;
import com.example.ehrsystem.modules.treatmentplan.repository.TreatmentPlanRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Treatment items are a child domain of Treatment Plan (itself a child of Encounter).
 * An item may optionally reference a diagnosis of the same encounter.
 * Medication orders are intentionally out of scope — the Prescription module
 * will own medications, dosage, and routes.
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class TreatmentPlanItemService {

    private final TreatmentPlanItemRepository itemRepository;
    private final TreatmentPlanRepository planRepository;
    private final EncounterRepository encounterRepository;
    private final DiagnosisRepository diagnosisRepository;
    private final SecurityContextAccessor securityContext;
    private final AuditLogger auditLogger;
    private final TreatmentPlanStatusTransitionService transitionService;

    @Transactional
    public TreatmentPlanItemResponse create(UUID encounterUuid, UUID planUuid,
                                            CreateTreatmentPlanItemRequest request) {
        Encounter encounter = requireEncounter(encounterUuid);
        TreatmentPlan plan = requirePlan(encounter, planUuid);
        assertEncounterModifiable(encounter);
        assertPlanOpen(plan);

        Diagnosis diagnosis = resolveDiagnosis(encounter, request.getDiagnosisUuid());
        validateDates(request.getStartDate(), request.getEndDate());
        validateAgainstPlanDates(plan, request.getStartDate(), request.getEndDate());
        validateDurationPair(request.getDuration(), request.getDurationUnit());

        Long currentUserId = securityContext.getCurrentUserId();
        TreatmentPriority priority = request.getPriority() != null
                ? request.getPriority() : TreatmentPriority.MEDIUM;

        TreatmentPlanItem item = TreatmentPlanItem.builder()
                .treatmentPlan(plan)
                .diagnosis(diagnosis)
                .treatmentType(request.getTreatmentType())
                .name(request.getName().trim())
                .description(normalize(request.getDescription()))
                .instructions(normalize(request.getInstructions()))
                .frequency(normalize(request.getFrequency()))
                .duration(request.getDuration())
                .durationUnit(request.getDurationUnit())
                .priority(priority)
                .startDate(request.getStartDate())
                .endDate(request.getEndDate())
                .createdBy(currentUserId)
                .updatedBy(currentUserId)
                .build();

        TreatmentPlanItem saved = itemRepository.save(item);

        auditLogger.logCustomEvent("TREATMENT_ITEM_CREATED", Map.of(
                "encounterNumber", encounter.getEncounterNumber(),
                "itemUuid", saved.getUuid(),
                "treatmentType", saved.getTreatmentType().name(),
                "priority", saved.getPriority().name()
        ));

        return toResponse(saved);
    }

    public List<TreatmentPlanItemResponse> list(UUID encounterUuid, UUID planUuid) {
        Encounter encounter = requireEncounter(encounterUuid);
        TreatmentPlan plan = requirePlan(encounter, planUuid);
        return itemRepository.findByTreatmentPlanIdAndDeletedAtIsNullOrderByCreatedAtAsc(plan.getId())
                .stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional
    public TreatmentPlanItemResponse update(UUID encounterUuid, UUID planUuid, UUID itemUuid,
                                            UpdateTreatmentPlanItemRequest request) {
        Encounter encounter = requireEncounter(encounterUuid);
        TreatmentPlan plan = requirePlan(encounter, planUuid);
        TreatmentPlanItem item = requireItem(plan, itemUuid);
        assertEncounterModifiable(encounter);
        assertPlanOpen(plan);
        assertVersion(item, request.getVersion());

        LocalDate effectiveStart = request.getStartDate() != null
                ? request.getStartDate() : item.getStartDate();
        LocalDate effectiveEnd = request.getEndDate() != null
                ? request.getEndDate() : item.getEndDate();
        validateDates(effectiveStart, effectiveEnd);
        validateAgainstPlanDates(plan, effectiveStart, effectiveEnd);

        if (request.getTreatmentType() != null) {
            item.setTreatmentType(request.getTreatmentType());
        }
        if (request.getName() != null && !request.getName().isBlank()) {
            item.setName(request.getName().trim());
        }
        if (request.getDescription() != null) {
            item.setDescription(normalize(request.getDescription()));
        }
        if (request.getInstructions() != null) {
            item.setInstructions(normalize(request.getInstructions()));
        }
        if (request.getFrequency() != null) {
            item.setFrequency(normalize(request.getFrequency()));
        }
        if (request.getDuration() != null || request.getDurationUnit() != null) {
            Integer effectiveDuration = request.getDuration() != null
                    ? request.getDuration() : item.getDuration();
            var effectiveUnit = request.getDurationUnit() != null
                    ? request.getDurationUnit() : item.getDurationUnit();
            validateDurationPair(effectiveDuration, effectiveUnit);
            item.setDuration(request.getDuration() != null ? request.getDuration() : item.getDuration());
            item.setDurationUnit(request.getDurationUnit() != null
                    ? request.getDurationUnit() : item.getDurationUnit());
        }
        if (request.getPriority() != null) {
            item.setPriority(request.getPriority());
        }
        if (request.getStartDate() != null) {
            item.setStartDate(request.getStartDate());
        }
        if (request.getEndDate() != null) {
            item.setEndDate(request.getEndDate());
        }

        if (Boolean.TRUE.equals(request.getUnlinkDiagnosis())) {
            item.setDiagnosis(null);
        } else if (request.getDiagnosisUuid() != null) {
            item.setDiagnosis(resolveDiagnosis(encounter, request.getDiagnosisUuid()));
        }

        item.setUpdatedBy(securityContext.getCurrentUserId());
        TreatmentPlanItem saved = itemRepository.save(item);

        auditLogger.logCustomEvent("TREATMENT_ITEM_UPDATED", Map.of(
                "encounterNumber", encounter.getEncounterNumber(),
                "itemUuid", saved.getUuid(),
                "treatmentType", saved.getTreatmentType().name(),
                "status", saved.getStatus().name()
        ));

        return toResponse(saved);
    }

    @Transactional
    public TreatmentPlanItemResponse start(UUID encounterUuid, UUID planUuid, UUID itemUuid) {
        TreatmentPlanItem item = requireItemContext(encounterUuid, planUuid, itemUuid);

        if (item.getStatus() == TreatmentItemStatus.IN_PROGRESS) {
            return toResponse(item);
        }
        return applyItemTransition(item, TreatmentItemStatus.IN_PROGRESS, "TREATMENT_ITEM_STARTED");
    }

    @Transactional
    public TreatmentPlanItemResponse complete(UUID encounterUuid, UUID planUuid, UUID itemUuid) {
        TreatmentPlanItem item = requireItemContext(encounterUuid, planUuid, itemUuid);

        if (item.getStatus() == TreatmentItemStatus.COMPLETED) {
            throw new IllegalArgumentException("Treatment item is already completed");
        }
        return applyItemTransition(item, TreatmentItemStatus.COMPLETED, "TREATMENT_ITEM_COMPLETED");
    }

    @Transactional
    public TreatmentPlanItemResponse cancel(UUID encounterUuid, UUID planUuid, UUID itemUuid) {
        TreatmentPlanItem item = requireItemContext(encounterUuid, planUuid, itemUuid);

        if (item.getStatus() == TreatmentItemStatus.CANCELLED) {
            throw new IllegalArgumentException("Treatment item is already cancelled");
        }
        return applyItemTransition(item, TreatmentItemStatus.CANCELLED, "TREATMENT_ITEM_CANCELLED");
    }

    @Transactional
    public void delete(UUID encounterUuid, UUID planUuid, UUID itemUuid) {
        TreatmentPlanItem item = requireItemContext(encounterUuid, planUuid, itemUuid);

        if (item.getStatus() == TreatmentItemStatus.COMPLETED) {
            throw new IllegalArgumentException("Cannot delete a completed treatment item");
        }

        item.setDeletedAt(LocalDateTime.now());
        item.setUpdatedBy(securityContext.getCurrentUserId());
        itemRepository.save(item);

        auditLogger.logCustomEvent("TREATMENT_ITEM_DELETED", Map.of(
                "encounterNumber", item.getTreatmentPlan().getEncounter().getEncounterNumber(),
                "itemUuid", item.getUuid(),
                "treatmentType", item.getTreatmentType().name()
        ));
    }

    private TreatmentPlanItem requireItemContext(UUID encounterUuid, UUID planUuid, UUID itemUuid) {
        Encounter encounter = requireEncounter(encounterUuid);
        TreatmentPlan plan = requirePlan(encounter, planUuid);
        TreatmentPlanItem item = requireItem(plan, itemUuid);
        assertEncounterModifiable(encounter);
        assertPlanOpen(plan);
        return item;
    }

    private TreatmentPlanItemResponse applyItemTransition(TreatmentPlanItem item,
                                                  TreatmentItemStatus target,
                                                  String auditEvent) {
        transitionService.validateItem(item.getStatus(), target);
        item.setStatus(target);
        item.setUpdatedBy(securityContext.getCurrentUserId());

        TreatmentPlanItem saved = itemRepository.save(item);

        auditLogger.logCustomEvent(auditEvent, Map.of(
                "encounterNumber", saved.getTreatmentPlan().getEncounter().getEncounterNumber(),
                "itemUuid", saved.getUuid(),
                "treatmentType", saved.getTreatmentType().name(),
                "status", saved.getStatus().name()
        ));

        return toResponse(saved);
    }

    private Encounter requireEncounter(UUID uuid) {
        return encounterRepository.findByUuidAndDeletedAtIsNull(uuid)
                .orElseThrow(() -> new EntityNotFoundException("Encounter not found with UUID: " + uuid));
    }

    private TreatmentPlan requirePlan(Encounter encounter, UUID planUuid) {
        return planRepository
                .findByUuidAndEncounterIdAndDeletedAtIsNull(planUuid, encounter.getId())
                .orElseThrow(() -> new EntityNotFoundException(
                        "Treatment plan not found with UUID: " + planUuid));
    }

    private TreatmentPlanItem requireItem(TreatmentPlan plan, UUID itemUuid) {
        return itemRepository
                .findByUuidAndTreatmentPlanIdAndDeletedAtIsNull(itemUuid, plan.getId())
                .orElseThrow(() -> new EntityNotFoundException(
                        "Treatment item not found with UUID: " + itemUuid));
    }

    /**
     * An item must belong to a diagnosis of the SAME encounter.
     * Unknown or soft-deleted diagnosis -> 404.
     * Diagnosis from another encounter -> rejected (400).
     */
    private Diagnosis resolveDiagnosis(Encounter encounter, UUID diagnosisUuid) {
        if (diagnosisUuid == null) {
            return null;
        }
        Diagnosis diagnosis = diagnosisRepository.findByUuidAndDeletedAtIsNull(diagnosisUuid)
                .orElseThrow(() -> new EntityNotFoundException(
                        "Diagnosis not found with UUID: " + diagnosisUuid));
        if (!diagnosis.getEncounter().getId().equals(encounter.getId())) {
            throw new IllegalArgumentException("Diagnosis does not belong to this encounter");
        }
        return diagnosis;
    }

    private void assertEncounterModifiable(Encounter encounter) {
        if (encounter.getStatus() == EncounterStatus.COMPLETED) {
            throw new IllegalArgumentException("Cannot modify treatment items for a completed encounter");
        }
        if (encounter.getStatus() == EncounterStatus.CANCELLED) {
            throw new IllegalArgumentException("Cannot modify treatment items for a cancelled encounter");
        }
    }

    private void assertPlanOpen(TreatmentPlan plan) {
        if (plan.getStatus() == TreatmentPlanStatus.COMPLETED) {
            throw new IllegalArgumentException("Cannot modify items of a completed treatment plan");
        }
        if (plan.getStatus() == TreatmentPlanStatus.CANCELLED) {
            throw new IllegalArgumentException("Cannot modify items of a cancelled treatment plan");
        }
    }

    private void assertVersion(TreatmentPlanItem item, Long expectedVersion) {
        if (expectedVersion != null && !expectedVersion.equals(item.getVersion())) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "This clinical record was updated by another user. Reload to continue.");
        }
    }

    private void validateDates(LocalDate startDate, LocalDate endDate) {
        if (startDate != null && endDate != null && endDate.isBefore(startDate)) {
            throw new IllegalArgumentException("End date must be on or after start date");
        }
    }

    private void validateAgainstPlanDates(TreatmentPlan plan, LocalDate startDate, LocalDate endDate) {
        if (plan.getStartDate() != null && startDate != null && startDate.isBefore(plan.getStartDate())) {
            throw new IllegalArgumentException("Treatment item start date cannot be before the plan start date");
        }
        if (plan.getEndDate() != null && endDate != null && endDate.isAfter(plan.getEndDate())) {
            throw new IllegalArgumentException("Treatment item end date cannot be after the plan end date");
        }
    }

    private void validateDurationPair(Integer duration, DurationUnit unit) {
        if (duration != null && unit == null) {
            throw new IllegalArgumentException("Duration unit is required when duration is provided");
        }
        if (unit != null && duration == null) {
            throw new IllegalArgumentException("Duration is required when duration unit is provided");
        }
    }

    private String normalize(String value) {
        if (value == null) return null;
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    private TreatmentPlanItemResponse toResponse(TreatmentPlanItem item) {
        TreatmentPlanItemResponse.TreatmentPlanItemResponseBuilder builder =
                TreatmentPlanItemResponse.builder()
                        .id(item.getId())
                        .uuid(item.getUuid())
                        .treatmentPlanUuid(item.getTreatmentPlan().getUuid())
                        .treatmentType(item.getTreatmentType())
                        .name(item.getName())
                        .description(item.getDescription())
                        .instructions(item.getInstructions())
                        .frequency(item.getFrequency())
                        .duration(item.getDuration())
                        .durationUnit(item.getDurationUnit())
                        .priority(item.getPriority())
                        .status(item.getStatus())
                        .startDate(item.getStartDate())
                        .endDate(item.getEndDate())
                        .createdAt(item.getCreatedAt())
                        .updatedAt(item.getUpdatedAt())
                        .version(item.getVersion());

        if (item.getDiagnosis() != null) {
            builder.diagnosisUuid(item.getDiagnosis().getUuid())
                    .diagnosisCode(item.getDiagnosis().getCode())
                    .diagnosisName(item.getDiagnosis().getName());
        }
        return builder.build();
    }
}
