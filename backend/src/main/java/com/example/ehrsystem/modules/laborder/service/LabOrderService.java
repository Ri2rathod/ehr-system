package com.example.ehrsystem.modules.laborder.service;

import com.example.ehrsystem.common.security.SecurityContextAccessor;
import com.example.ehrsystem.common.util.AuditLogger;
import com.example.ehrsystem.modules.encounter.entity.Encounter;
import com.example.ehrsystem.modules.encounter.entity.EncounterStatus;
import com.example.ehrsystem.modules.encounter.repository.EncounterRepository;
import com.example.ehrsystem.modules.laborder.dto.request.CreateLabOrderItemRequest;
import com.example.ehrsystem.modules.laborder.dto.request.CreateLabOrderRequest;
import com.example.ehrsystem.modules.laborder.dto.request.UpdateLabOrderRequest;
import com.example.ehrsystem.modules.laborder.dto.response.LabOrderItemResponse;
import com.example.ehrsystem.modules.laborder.dto.response.LabOrderResponse;
import com.example.ehrsystem.modules.laborder.dto.response.LabOrderSummaryResponse;
import com.example.ehrsystem.modules.laborder.entity.LabOrder;
import com.example.ehrsystem.modules.laborder.entity.LabOrderItem;
import com.example.ehrsystem.modules.laborder.entity.LabOrderItemStatus;
import com.example.ehrsystem.modules.laborder.entity.LabOrderPriority;
import com.example.ehrsystem.modules.laborder.entity.LabOrderStatus;
import com.example.ehrsystem.modules.laborder.repository.LabOrderItemRepository;
import com.example.ehrsystem.modules.laborder.repository.LabOrderRepository;
import com.example.ehrsystem.modules.labresult.entity.LabResultStatus;
import com.example.ehrsystem.modules.labresult.repository.LabResultRepository;
import com.example.ehrsystem.modules.labresult.repository.LabResultValueRepository;
import com.example.ehrsystem.modules.specimen.entity.Specimen;
import com.example.ehrsystem.modules.specimen.entity.SpecimenStatus;
import com.example.ehrsystem.modules.specimen.repository.SpecimenRepository;
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
 * Lab orders are a child clinical domain of Encounter. The encounter gates
 * clinical edits (create/update/delete/item changes), but lab processing
 * (order/start/complete/cancel, specimens, results) continues regardless of
 * encounter status once the order exists - this is the lab module's
 * explicit carve-out from the prescription pattern.
 * patient and doctor are always derived through the Encounter.
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class LabOrderService {

    private final LabOrderRepository labOrderRepository;
    private final LabOrderItemRepository labOrderItemRepository;
    private final EncounterRepository encounterRepository;
    private final SpecimenRepository specimenRepository;
    private final LabResultRepository labResultRepository;
    private final LabResultValueRepository labResultValueRepository;
    private final LabOrderNumberService labOrderNumberService;
    private final LabOrderItemService labOrderItemService;
    private final LabOrderStatusTransitionService transitionService;
    private final SecurityContextAccessor securityContext;
    private final AuditLogger auditLogger;

    @Transactional
    public LabOrderResponse create(UUID encounterUuid, CreateLabOrderRequest request) {
        Encounter encounter = requireEncounter(encounterUuid);
        assertEncounterModifiable(encounter);

        Long currentUserId = securityContext.getCurrentUserId();

        LabOrder order = LabOrder.builder()
                .orderNumber(labOrderNumberService.generateLabOrderNumber())
                .encounter(encounter)
                .priority(request.getPriority() != null
                        ? request.getPriority() : LabOrderPriority.ROUTINE)
                .instructions(normalize(request.getInstructions()))
                .createdBy(currentUserId)
                .updatedBy(currentUserId)
                .build();

        LabOrder saved = labOrderRepository.save(order);
        List<LabOrderItemResponse> items = labOrderItemService.createItems(
                saved, request.getItems(), currentUserId);
        saved = labOrderRepository.save(saved);

        auditLogger.logCustomEvent("LAB_ORDER_CREATED", Map.of(
                "encounterNumber", encounter.getEncounterNumber(),
                "orderNumber", saved.getOrderNumber(),
                "status", saved.getStatus().name(),
                "priority", saved.getPriority().name(),
                "testCount", String.valueOf(items.size())
        ));

        return toResponse(saved, items);
    }

    public List<LabOrderSummaryResponse> list(UUID encounterUuid) {
        Encounter encounter = requireEncounter(encounterUuid);
        return labOrderRepository
                .findByEncounterIdAndDeletedAtIsNullOrderByCreatedAtAsc(encounter.getId())
                .stream()
                .map(this::toSummary)
                .toList();
    }

    public LabOrderResponse get(UUID encounterUuid, UUID labOrderUuid) {
        Encounter encounter = requireEncounter(encounterUuid);
        LabOrder order = requireOrder(encounter, labOrderUuid);
        List<LabOrderItemResponse> items = labOrderItemRepository
                .findByLabOrderIdAndDeletedAtIsNullOrderByCreatedAtAsc(order.getId())
                .stream()
                .map(this::toItemResponse)
                .toList();
        return toResponse(order, items);
    }

    @Transactional
    public LabOrderResponse update(UUID encounterUuid, UUID labOrderUuid,
                                   UpdateLabOrderRequest request) {
        Encounter encounter = requireEncounter(encounterUuid);
        LabOrder order = requireOrder(encounter, labOrderUuid);
        assertEncounterModifiable(encounter);
        assertOrderEditable(order);
        assertVersion(order, request.getVersion());

        if (request.getPriority() != null) {
            order.setPriority(request.getPriority());
        }
        if (request.getInstructions() != null) {
            order.setInstructions(normalize(request.getInstructions()));
        }
        order.setUpdatedBy(securityContext.getCurrentUserId());

        LabOrder saved = labOrderRepository.save(order);

        auditLogger.logCustomEvent("LAB_ORDER_UPDATED", Map.of(
                "encounterNumber", encounter.getEncounterNumber(),
                "orderNumber", saved.getOrderNumber(),
                "status", saved.getStatus().name(),
                "priority", saved.getPriority().name()
        ));

        return getResponseWithItems(saved);
    }

    /**
     * DRAFT -> ORDERED. Independent of the encounter status: lab processing
     * of an existing order may continue after the encounter completes.
     * Idempotent when the order is already placed.
     */
    @Transactional
    public LabOrderResponse order(UUID encounterUuid, UUID labOrderUuid) {
        Encounter encounter = requireEncounter(encounterUuid);
        LabOrder order = requireOrder(encounter, labOrderUuid);

        if (order.getStatus() == LabOrderStatus.ORDERED) {
            return getResponseWithItems(order);
        }
        // Terminal/invalid states win over the empty-items guard so callers
        // always see the real status error first.
        transitionService.validate(order.getStatus(), LabOrderStatus.ORDERED);
        if (labOrderItemRepository.countByLabOrderIdAndDeletedAtIsNull(order.getId()) < 1) {
            throw new IllegalArgumentException(
                    "Cannot order a lab order without lab tests");
        }

        order.setStatus(LabOrderStatus.ORDERED);
        order.setOrderedAt(LocalDateTime.now());
        order.setUpdatedBy(securityContext.getCurrentUserId());
        LabOrder saved = labOrderRepository.save(order);

        auditLogger.logCustomEvent("LAB_ORDER_ORDERED", Map.of(
                "encounterNumber", encounter.getEncounterNumber(),
                "orderNumber", saved.getOrderNumber(),
                "status", saved.getStatus().name()
        ));

        return getResponseWithItems(saved);
    }

    /** ORDERED -> IN_PROGRESS; items advance with the order. */
    @Transactional
    public LabOrderResponse start(UUID encounterUuid, UUID labOrderUuid) {
        Encounter encounter = requireEncounter(encounterUuid);
        LabOrder order = requireOrder(encounter, labOrderUuid);

        if (order.getStatus() == LabOrderStatus.IN_PROGRESS) {
            return getResponseWithItems(order);
        }
        transitionService.validate(order.getStatus(), LabOrderStatus.IN_PROGRESS);

        order.setStatus(LabOrderStatus.IN_PROGRESS);
        order.setUpdatedBy(securityContext.getCurrentUserId());
        LabOrder saved = labOrderRepository.save(order);
        cascadeItems(saved, LabOrderItemStatus.ORDERED, LabOrderItemStatus.IN_PROGRESS);

        auditLogger.logCustomEvent("LAB_ORDER_STARTED", Map.of(
                "encounterNumber", encounter.getEncounterNumber(),
                "orderNumber", saved.getOrderNumber(),
                "status", saved.getStatus().name()
        ));

        return getResponseWithItems(saved);
    }

    /** IN_PROGRESS -> COMPLETED; remaining items complete with the order. */
    @Transactional
    public LabOrderResponse complete(UUID encounterUuid, UUID labOrderUuid) {
        Encounter encounter = requireEncounter(encounterUuid);
        LabOrder order = requireOrder(encounter, labOrderUuid);

        if (order.getStatus() == LabOrderStatus.COMPLETED) {
            throw new IllegalArgumentException("Lab order is already completed");
        }
        transitionService.validate(order.getStatus(), LabOrderStatus.COMPLETED);

        order.setStatus(LabOrderStatus.COMPLETED);
        order.setUpdatedBy(securityContext.getCurrentUserId());
        LabOrder saved = labOrderRepository.save(order);
        cascadeItems(saved, LabOrderItemStatus.ORDERED, LabOrderItemStatus.COMPLETED);
        cascadeItems(saved, LabOrderItemStatus.IN_PROGRESS, LabOrderItemStatus.COMPLETED);

        auditLogger.logCustomEvent("LAB_ORDER_COMPLETED", Map.of(
                "encounterNumber", encounter.getEncounterNumber(),
                "orderNumber", saved.getOrderNumber(),
                "status", saved.getStatus().name()
        ));

        return getResponseWithItems(saved);
    }

    /** Any non-terminal state -> CANCELLED; active items cancel with it. */
    @Transactional
    public LabOrderResponse cancel(UUID encounterUuid, UUID labOrderUuid) {
        Encounter encounter = requireEncounter(encounterUuid);
        LabOrder order = requireOrder(encounter, labOrderUuid);

        if (order.getStatus() == LabOrderStatus.CANCELLED) {
            throw new IllegalArgumentException("Lab order is already cancelled");
        }
        transitionService.validate(order.getStatus(), LabOrderStatus.CANCELLED);

        order.setStatus(LabOrderStatus.CANCELLED);
        order.setUpdatedBy(securityContext.getCurrentUserId());
        LabOrder saved = labOrderRepository.save(order);
        cascadeItems(saved, LabOrderItemStatus.ORDERED, LabOrderItemStatus.CANCELLED);
        cascadeItems(saved, LabOrderItemStatus.IN_PROGRESS, LabOrderItemStatus.CANCELLED);

        auditLogger.logCustomEvent("LAB_ORDER_CANCELLED", Map.of(
                "encounterNumber", encounter.getEncounterNumber(),
                "orderNumber", saved.getOrderNumber(),
                "status", saved.getStatus().name()
        ));

        return getResponseWithItems(saved);
    }

    /**
     * Soft delete with cascade (items, specimens, non-final results and
     * their values). Completed orders and orders with finalized results are
     * preserved as clinical history.
     */
    @Transactional
    public void delete(UUID encounterUuid, UUID labOrderUuid) {
        Encounter encounter = requireEncounter(encounterUuid);
        LabOrder order = requireOrder(encounter, labOrderUuid);
        assertEncounterModifiable(encounter);

        long finalizedResults = labResultRepository
                .countByLabOrderIdAndStatusInAndDeletedAtIsNull(
                        order.getId(),
                        List.of(LabResultStatus.FINAL, LabResultStatus.CORRECTED));
        if (finalizedResults > 0) {
            throw new IllegalArgumentException(
                    "Cannot delete a lab order with finalized lab results");
        }
        if (order.getStatus() == LabOrderStatus.COMPLETED) {
            throw new IllegalArgumentException("Cannot delete a completed lab order");
        }

        Long currentUserId = securityContext.getCurrentUserId();
        LocalDateTime now = LocalDateTime.now();

        List<LabOrderItem> items = labOrderItemRepository
                .findByLabOrderIdAndDeletedAtIsNullOrderByCreatedAtAsc(order.getId());
        for (LabOrderItem item : items) {
            item.setDeletedAt(now);
            item.setUpdatedBy(currentUserId);
        }
        labOrderItemRepository.saveAll(items);

        for (Specimen specimen : specimenRepository
                .findByLabOrderIdAndDeletedAtIsNullOrderByCreatedAtAsc(order.getId())) {
            specimen.setDeletedAt(now);
            specimen.setUpdatedBy(currentUserId);
            specimenRepository.save(specimen);
        }

        for (var result : labResultRepository.findByLabOrderIdAndDeletedAtIsNull(order.getId())) {
            for (var value : labResultValueRepository
                    .findByLabResultIdAndDeletedAtIsNullOrderByCreatedAtAsc(result.getId())) {
                value.setDeletedAt(now);
                value.setUpdatedBy(currentUserId);
                labResultValueRepository.save(value);
            }
            result.setDeletedAt(now);
            result.setUpdatedBy(currentUserId);
            labResultRepository.save(result);
        }

        order.setDeletedAt(now);
        order.setUpdatedBy(currentUserId);
        labOrderRepository.save(order);

        auditLogger.logCustomEvent("LAB_ORDER_DELETED", Map.of(
                "encounterNumber", encounter.getEncounterNumber(),
                "orderNumber", order.getOrderNumber(),
                "status", order.getStatus().name()
        ));
    }

    private LabOrderResponse getResponseWithItems(LabOrder order) {
        List<LabOrderItemResponse> items = labOrderItemRepository
                .findByLabOrderIdAndDeletedAtIsNullOrderByCreatedAtAsc(order.getId())
                .stream()
                .map(this::toItemResponse)
                .toList();
        return toResponse(order, items);
    }

    private void cascadeItems(LabOrder order, LabOrderItemStatus from,
                              LabOrderItemStatus to) {
        if (from == to) {
            return;
        }
        Long currentUserId = securityContext.getCurrentUserId();
        for (LabOrderItem item : labOrderItemRepository
                .findByLabOrderIdAndDeletedAtIsNullOrderByCreatedAtAsc(order.getId())) {
            if (item.getStatus() == from) {
                item.setStatus(to);
                item.setUpdatedBy(currentUserId);
                labOrderItemRepository.save(item);
            }
        }
    }

    private Encounter requireEncounter(UUID uuid) {
        return encounterRepository.findByUuidAndDeletedAtIsNull(uuid)
                .orElseThrow(() -> new EntityNotFoundException(
                        "Encounter not found with UUID: " + uuid));
    }

    private LabOrder requireOrder(Encounter encounter, UUID labOrderUuid) {
        return labOrderRepository
                .findByUuidAndEncounterIdAndDeletedAtIsNull(labOrderUuid, encounter.getId())
                .orElseThrow(() -> new EntityNotFoundException(
                        "Lab order not found with UUID: " + labOrderUuid));
    }

    private void assertEncounterModifiable(Encounter encounter) {
        if (encounter.getStatus() == EncounterStatus.COMPLETED) {
            throw new IllegalArgumentException(
                    "Cannot modify lab orders for a completed encounter");
        }
        if (encounter.getStatus() == EncounterStatus.CANCELLED) {
            throw new IllegalArgumentException(
                    "Cannot modify lab orders for a cancelled encounter");
        }
    }

    private void assertOrderEditable(LabOrder order) {
        if (order.getStatus() == LabOrderStatus.COMPLETED) {
            throw new IllegalArgumentException("Cannot modify a completed lab order");
        }
        if (order.getStatus() == LabOrderStatus.CANCELLED) {
            throw new IllegalArgumentException("Cannot modify a cancelled lab order");
        }
    }

    private void assertVersion(LabOrder order, Long expectedVersion) {
        if (expectedVersion != null && !expectedVersion.equals(order.getVersion())) {
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

    private LabOrderSummaryResponse toSummary(LabOrder order) {
        Counts counts = loadCounts(order);
        return LabOrderSummaryResponse.builder()
                .id(order.getId())
                .uuid(order.getUuid())
                .encounterUuid(order.getEncounter().getUuid())
                .orderNumber(order.getOrderNumber())
                .priority(order.getPriority())
                .status(order.getStatus())
                .instructions(order.getInstructions())
                .orderedAt(order.getOrderedAt())
                .itemCount(counts.itemCount)
                .specimenCount(counts.specimenCount)
                .specimenStatus(counts.specimenStatus)
                .resultCount(counts.resultCount)
                .finalizedResultCount(counts.finalizedResultCount)
                .createdAt(order.getCreatedAt())
                .updatedAt(order.getUpdatedAt())
                .version(order.getVersion())
                .build();
    }

    private LabOrderResponse toResponse(LabOrder order, List<LabOrderItemResponse> items) {
        Counts counts = loadCounts(order);
        return LabOrderResponse.builder()
                .id(order.getId())
                .uuid(order.getUuid())
                .encounterUuid(order.getEncounter().getUuid())
                .orderNumber(order.getOrderNumber())
                .priority(order.getPriority())
                .status(order.getStatus())
                .instructions(order.getInstructions())
                .orderedAt(order.getOrderedAt())
                .itemCount(counts.itemCount)
                .specimenCount(counts.specimenCount)
                .specimenStatus(counts.specimenStatus)
                .resultCount(counts.resultCount)
                .finalizedResultCount(counts.finalizedResultCount)
                .items(items)
                .createdAt(order.getCreatedAt())
                .updatedAt(order.getUpdatedAt())
                .version(order.getVersion())
                .build();
    }

    private LabOrderItemResponse toItemResponse(LabOrderItem item) {
        return LabOrderItemResponse.builder()
                .id(item.getId())
                .uuid(item.getUuid())
                .labOrderUuid(item.getLabOrder().getUuid())
                .labTestUuid(item.getLabTest().getUuid())
                .labTestCode(item.getLabTest().getCode())
                .labTestName(item.getLabTest().getName())
                .instructions(item.getInstructions())
                .status(item.getStatus())
                .createdAt(item.getCreatedAt())
                .updatedAt(item.getUpdatedAt())
                .version(item.getVersion())
                .build();
    }

    private static class Counts {
        long itemCount;
        long specimenCount;
        String specimenStatus;
        long resultCount;
        long finalizedResultCount;
    }

    private Counts loadCounts(LabOrder order) {
        Counts counts = new Counts();
        counts.itemCount = labOrderItemRepository.countByLabOrderIdAndDeletedAtIsNull(order.getId());

        List<Specimen> specimens = specimenRepository
                .findByLabOrderIdAndDeletedAtIsNullOrderByCreatedAtAsc(order.getId());
        counts.specimenCount = specimens.size();
        counts.specimenStatus = aggregateSpecimenStatus(specimens);

        counts.resultCount = labResultRepository.countByLabOrderIdAndDeletedAtIsNull(order.getId());
        counts.finalizedResultCount = labResultRepository
                .countByLabOrderIdAndStatusInAndDeletedAtIsNull(
                        order.getId(),
                        List.of(LabResultStatus.FINAL, LabResultStatus.CORRECTED));
        return counts;
    }

    /**
     * Overall specimen progress for summaries: the least advanced active
     * status wins (awaiting collection beats collected beats received);
     * when everything is rejected/cancelled the first specimen's status is
     * surfaced. Null when no specimen exists yet.
     */
    private String aggregateSpecimenStatus(List<Specimen> specimens) {
        if (specimens.isEmpty()) {
            return null;
        }
        boolean anyPending = specimens.stream()
                .anyMatch(s -> s.getStatus() == SpecimenStatus.PENDING_COLLECTION);
        if (anyPending) {
            return SpecimenStatus.PENDING_COLLECTION.name();
        }
        boolean anyCollected = specimens.stream()
                .anyMatch(s -> s.getStatus() == SpecimenStatus.COLLECTED);
        if (anyCollected) {
            return SpecimenStatus.COLLECTED.name();
        }
        boolean anyReceived = specimens.stream()
                .anyMatch(s -> s.getStatus() == SpecimenStatus.RECEIVED);
        if (anyReceived) {
            return SpecimenStatus.RECEIVED.name();
        }
        return specimens.get(0).getStatus().name();
    }
}
