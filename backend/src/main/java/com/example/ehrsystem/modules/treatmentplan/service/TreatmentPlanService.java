package com.example.ehrsystem.modules.treatmentplan.service;

import com.example.ehrsystem.common.security.SecurityContextAccessor;
import com.example.ehrsystem.common.util.AuditLogger;
import com.example.ehrsystem.modules.encounter.entity.Encounter;
import com.example.ehrsystem.modules.encounter.entity.EncounterStatus;
import com.example.ehrsystem.modules.encounter.repository.EncounterRepository;
import com.example.ehrsystem.modules.treatmentplan.dto.request.CreateTreatmentPlanRequest;
import com.example.ehrsystem.modules.treatmentplan.dto.request.UpdateTreatmentPlanRequest;
import com.example.ehrsystem.modules.treatmentplan.dto.response.TreatmentPlanResponse;
import com.example.ehrsystem.modules.treatmentplan.entity.TreatmentPlan;
import com.example.ehrsystem.modules.treatmentplan.entity.TreatmentPlanItem;
import com.example.ehrsystem.modules.treatmentplan.entity.TreatmentPlanStatus;
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
 * Treatment plans are a child clinical domain of Encounter.
 * Lifecycle rules (transitions, encounter modifiability, concurrency)
 * are enforced here — never in the controller.
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class TreatmentPlanService {

    private final TreatmentPlanRepository treatmentPlanRepository;
    private final TreatmentPlanItemRepository treatmentPlanItemRepository;
    private final EncounterRepository encounterRepository;
    private final SecurityContextAccessor securityContext;
    private final AuditLogger auditLogger;
    private final TreatmentPlanStatusTransitionService transitionService;

    @Transactional
    public TreatmentPlanResponse create(UUID encounterUuid, CreateTreatmentPlanRequest request) {
        Encounter encounter = requireEncounter(encounterUuid);
        assertEncounterModifiable(encounter);
        validateDates(request.getStartDate(), request.getEndDate());

        Long currentUserId = securityContext.getCurrentUserId();

        TreatmentPlan plan = TreatmentPlan.builder()
                .encounter(encounter)
                .title(request.getTitle().trim())
                .goals(normalize(request.getGoals()))
                .instructions(normalize(request.getInstructions()))
                .followUpInstructions(normalize(request.getFollowUpInstructions()))
                .notes(normalize(request.getNotes()))
                .startDate(request.getStartDate())
                .endDate(request.getEndDate())
                .createdBy(currentUserId)
                .updatedBy(currentUserId)
                .build();

        TreatmentPlan saved = treatmentPlanRepository.save(plan);

        auditLogger.logCustomEvent("TREATMENT_PLAN_CREATED", Map.of(
                "encounterNumber", encounter.getEncounterNumber(),
                "planUuid", saved.getUuid(),
                "status", saved.getStatus().name()
        ));

        return toResponse(saved);
    }

    public List<TreatmentPlanResponse> list(UUID encounterUuid) {
        Encounter encounter = requireEncounter(encounterUuid);
        return treatmentPlanRepository
                .findByEncounterIdAndDeletedAtIsNullOrderByCreatedAtDesc(encounter.getId()).stream()
                .map(this::toResponse)
                .toList();
    }

    public TreatmentPlanResponse get(UUID encounterUuid, UUID planUuid) {
        Encounter encounter = requireEncounter(encounterUuid);
        return toResponse(requirePlan(encounter, planUuid));
    }

    @Transactional
    public TreatmentPlanResponse update(UUID encounterUuid, UUID planUuid,
                                        UpdateTreatmentPlanRequest request) {
        Encounter encounter = requireEncounter(encounterUuid);
        TreatmentPlan plan = requirePlan(encounter, planUuid);
        assertEncounterModifiable(encounter);
        assertVersion(plan, request.getVersion());

        LocalDate effectiveStart = request.getStartDate() != null
                ? request.getStartDate() : plan.getStartDate();
        LocalDate effectiveEnd = request.getEndDate() != null
                ? request.getEndDate() : plan.getEndDate();
        validateDates(effectiveStart, effectiveEnd);

        if (request.getTitle() != null && !request.getTitle().isBlank()) {
            plan.setTitle(request.getTitle().trim());
        }
        if (request.getGoals() != null) {
            plan.setGoals(normalize(request.getGoals()));
        }
        if (request.getInstructions() != null) {
            plan.setInstructions(normalize(request.getInstructions()));
        }
        if (request.getFollowUpInstructions() != null) {
            plan.setFollowUpInstructions(normalize(request.getFollowUpInstructions()));
        }
        if (request.getNotes() != null) {
            plan.setNotes(normalize(request.getNotes()));
        }
        if (request.getStartDate() != null) {
            plan.setStartDate(request.getStartDate());
        }
        if (request.getEndDate() != null) {
            plan.setEndDate(request.getEndDate());
        }
        plan.setUpdatedBy(securityContext.getCurrentUserId());

        TreatmentPlan saved = treatmentPlanRepository.save(plan);

        auditLogger.logCustomEvent("TREATMENT_PLAN_UPDATED", Map.of(
                "encounterNumber", encounter.getEncounterNumber(),
                "planUuid", saved.getUuid(),
                "status", saved.getStatus().name()
        ));

        return toResponse(saved);
    }

    @Transactional
    public TreatmentPlanResponse activate(UUID encounterUuid, UUID planUuid) {
        Encounter encounter = requireEncounter(encounterUuid);
        TreatmentPlan plan = requirePlan(encounter, planUuid);
        assertEncounterModifiable(encounter);

        if (plan.getStatus() == TreatmentPlanStatus.ACTIVE) {
            return toResponse(plan);
        }
        return applyPlanTransition(encounter, plan, TreatmentPlanStatus.ACTIVE,
                "TREATMENT_PLAN_ACTIVATED");
    }

    @Transactional
    public TreatmentPlanResponse complete(UUID encounterUuid, UUID planUuid) {
        Encounter encounter = requireEncounter(encounterUuid);
        TreatmentPlan plan = requirePlan(encounter, planUuid);
        assertEncounterModifiable(encounter);

        if (plan.getStatus() == TreatmentPlanStatus.COMPLETED) {
            throw new IllegalArgumentException("Treatment plan is already completed");
        }
        return applyPlanTransition(encounter, plan, TreatmentPlanStatus.COMPLETED,
                "TREATMENT_PLAN_COMPLETED");
    }

    @Transactional
    public TreatmentPlanResponse cancel(UUID encounterUuid, UUID planUuid) {
        Encounter encounter = requireEncounter(encounterUuid);
        TreatmentPlan plan = requirePlan(encounter, planUuid);
        assertEncounterModifiable(encounter);

        if (plan.getStatus() == TreatmentPlanStatus.CANCELLED) {
            throw new IllegalArgumentException("Treatment plan is already cancelled");
        }
        return applyPlanTransition(encounter, plan, TreatmentPlanStatus.CANCELLED,
                "TREATMENT_PLAN_CANCELLED");
    }

    /**
     * Soft delete. Completed plans are preserved as clinical history.
     * Items of a deleted plan are soft deleted with it so no orphan
     * rows remain reachable through normal queries.
     */
    @Transactional
    public void delete(UUID encounterUuid, UUID planUuid) {
        Encounter encounter = requireEncounter(encounterUuid);
        TreatmentPlan plan = requirePlan(encounter, planUuid);
        assertEncounterModifiable(encounter);

        if (plan.getStatus() == TreatmentPlanStatus.COMPLETED) {
            throw new IllegalArgumentException("Cannot delete a completed treatment plan");
        }

        LocalDateTime now = LocalDateTime.now();
        Long currentUserId = securityContext.getCurrentUserId();

        List<TreatmentPlanItem> items =
                treatmentPlanItemRepository.findByTreatmentPlanIdAndDeletedAtIsNull(plan.getId());
        for (TreatmentPlanItem item : items) {
            item.setDeletedAt(now);
            item.setUpdatedBy(currentUserId);
        }
        treatmentPlanItemRepository.saveAll(items);

        plan.setDeletedAt(now);
        plan.setUpdatedBy(currentUserId);
        treatmentPlanRepository.save(plan);

        auditLogger.logCustomEvent("TREATMENT_PLAN_DELETED", Map.of(
                "encounterNumber", encounter.getEncounterNumber(),
                "planUuid", plan.getUuid(),
                "status", plan.getStatus().name()
        ));
    }

    private TreatmentPlanResponse applyPlanTransition(Encounter encounter, TreatmentPlan plan,
                                                      TreatmentPlanStatus target,
                                                      String auditEvent) {
        transitionService.validatePlan(plan.getStatus(), target);
        plan.setStatus(target);
        plan.setUpdatedBy(securityContext.getCurrentUserId());

        TreatmentPlan saved = treatmentPlanRepository.save(plan);

        auditLogger.logCustomEvent(auditEvent, Map.of(
                "encounterNumber", encounter.getEncounterNumber(),
                "planUuid", saved.getUuid(),
                "status", saved.getStatus().name()
        ));

        return toResponse(saved);
    }

    private Encounter requireEncounter(UUID uuid) {
        return encounterRepository.findByUuidAndDeletedAtIsNull(uuid)
                .orElseThrow(() -> new EntityNotFoundException("Encounter not found with UUID: " + uuid));
    }

    private TreatmentPlan requirePlan(Encounter encounter, UUID planUuid) {
        return treatmentPlanRepository
                .findByUuidAndEncounterIdAndDeletedAtIsNull(planUuid, encounter.getId())
                .orElseThrow(() -> new EntityNotFoundException(
                        "Treatment plan not found with UUID: " + planUuid));
    }

    private void assertEncounterModifiable(Encounter encounter) {
        if (encounter.getStatus() == EncounterStatus.COMPLETED) {
            throw new IllegalArgumentException("Cannot modify treatment plans for a completed encounter");
        }
        if (encounter.getStatus() == EncounterStatus.CANCELLED) {
            throw new IllegalArgumentException("Cannot modify treatment plans for a cancelled encounter");
        }
    }

    private void assertVersion(TreatmentPlan plan, Long expectedVersion) {
        if (expectedVersion != null && !expectedVersion.equals(plan.getVersion())) {
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

    private String normalize(String value) {
        if (value == null) return null;
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    private TreatmentPlanResponse toResponse(TreatmentPlan plan) {
        return TreatmentPlanResponse.builder()
                .id(plan.getId())
                .uuid(plan.getUuid())
                .encounterUuid(plan.getEncounter().getUuid())
                .title(plan.getTitle())
                .status(plan.getStatus())
                .goals(plan.getGoals())
                .instructions(plan.getInstructions())
                .followUpInstructions(plan.getFollowUpInstructions())
                .notes(plan.getNotes())
                .startDate(plan.getStartDate())
                .endDate(plan.getEndDate())
                .createdAt(plan.getCreatedAt())
                .updatedAt(plan.getUpdatedAt())
                .version(plan.getVersion())
                .build();
    }
}
