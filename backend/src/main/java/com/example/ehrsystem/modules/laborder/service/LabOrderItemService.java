package com.example.ehrsystem.modules.laborder.service;

import com.example.ehrsystem.common.security.SecurityContextAccessor;
import com.example.ehrsystem.common.util.AuditLogger;
import com.example.ehrsystem.modules.encounter.entity.Encounter;
import com.example.ehrsystem.modules.encounter.entity.EncounterStatus;
import com.example.ehrsystem.modules.encounter.repository.EncounterRepository;
import com.example.ehrsystem.modules.laborder.dto.request.CreateLabOrderItemRequest;
import com.example.ehrsystem.modules.laborder.dto.request.UpdateLabOrderItemRequest;
import com.example.ehrsystem.modules.laborder.dto.response.LabOrderItemResponse;
import com.example.ehrsystem.modules.laborder.entity.LabOrder;
import com.example.ehrsystem.modules.laborder.entity.LabOrderItem;
import com.example.ehrsystem.modules.laborder.entity.LabOrderStatus;
import com.example.ehrsystem.modules.laborder.repository.LabOrderItemRepository;
import com.example.ehrsystem.modules.laborder.repository.LabOrderRepository;
import com.example.ehrsystem.modules.labtest.entity.LabTest;
import com.example.ehrsystem.modules.labtest.repository.LabTestRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Lab order items (ordered tests). Items can only be edited while the
 * order is a draft and the encounter is modifiable - once the order is
 * placed, item changes go through the order lifecycle. Adding/removing
 * tests is a clinical edit of the encounter.
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class LabOrderItemService {

    private final LabOrderItemRepository labOrderItemRepository;
    private final LabOrderRepository labOrderRepository;
    private final EncounterRepository encounterRepository;
    private final LabTestRepository labTestRepository;
    private final SecurityContextAccessor securityContext;
    private final AuditLogger auditLogger;

    /**
     * Used by order creation: validates and persists initial items.
     * Caller has already asserted the encounter is modifiable.
     */
    @Transactional
    public List<LabOrderItemResponse> createItems(LabOrder order,
                                                  List<CreateLabOrderItemRequest> requests,
                                                  Long currentUserId) {
        if (requests == null || requests.isEmpty()) {
            return List.of();
        }
        return requests.stream()
                .map(request -> {
                    LabTest test = requireOrderableTest(request.getLabTestUuid());
                    assertNotDuplicate(order, test);

                    LabOrderItem item = LabOrderItem.builder()
                            .labOrder(order)
                            .labTest(test)
                            .instructions(normalize(request.getInstructions()))
                            .createdBy(currentUserId)
                            .updatedBy(currentUserId)
                            .build();
                    LabOrderItem saved = labOrderItemRepository.save(item);
                    return toResponse(saved);
                })
                .toList();
    }

    @Transactional
    public LabOrderItemResponse addItem(UUID encounterUuid, UUID labOrderUuid,
                                        CreateLabOrderItemRequest request) {
        Encounter encounter = requireEncounter(encounterUuid);
        LabOrder order = requireOrder(encounter, labOrderUuid);
        assertEncounterModifiable(encounter);
        assertOrderDraft(order);

        LabTest test = requireOrderableTest(request.getLabTestUuid());
        assertNotDuplicate(order, test);

        Long currentUserId = securityContext.getCurrentUserId();
        LabOrderItem item = LabOrderItem.builder()
                .labOrder(order)
                .labTest(test)
                .instructions(normalize(request.getInstructions()))
                .createdBy(currentUserId)
                .updatedBy(currentUserId)
                .build();

        LabOrderItem saved = labOrderItemRepository.save(item);

        auditLogger.logCustomEvent("LAB_ORDER_UPDATED", Map.of(
                "encounterNumber", encounter.getEncounterNumber(),
                "orderNumber", order.getOrderNumber(),
                "testCode", test.getCode()
        ));

        return toResponse(saved);
    }

    @Transactional
    public LabOrderItemResponse updateItem(UUID encounterUuid, UUID labOrderUuid,
                                           UUID itemUuid, UpdateLabOrderItemRequest request) {
        Encounter encounter = requireEncounter(encounterUuid);
        LabOrder order = requireOrder(encounter, labOrderUuid);
        assertEncounterModifiable(encounter);
        assertOrderDraft(order);
        LabOrderItem item = requireItem(order, itemUuid);
        assertVersion(item, request.getVersion());

        LabTest test = requireOrderableTest(request.getLabTestUuid());
        if (!test.getId().equals(item.getLabTest().getId())) {
            assertNotDuplicate(order, test);
            item.setLabTest(test);
        }
        if (request.getInstructions() != null) {
            item.setInstructions(normalize(request.getInstructions()));
        }
        item.setUpdatedBy(securityContext.getCurrentUserId());

        LabOrderItem saved = labOrderItemRepository.save(item);

        auditLogger.logCustomEvent("LAB_ORDER_UPDATED", Map.of(
                "encounterNumber", encounter.getEncounterNumber(),
                "orderNumber", order.getOrderNumber(),
                "testCode", saved.getLabTest().getCode()
        ));

        return toResponse(saved);
    }

    @Transactional
    public void deleteItem(UUID encounterUuid, UUID labOrderUuid, UUID itemUuid) {
        Encounter encounter = requireEncounter(encounterUuid);
        LabOrder order = requireOrder(encounter, labOrderUuid);
        assertEncounterModifiable(encounter);
        assertOrderDraft(order);
        LabOrderItem item = requireItem(order, itemUuid);

        Long currentUserId = securityContext.getCurrentUserId();
        item.setDeletedAt(java.time.LocalDateTime.now());
        item.setUpdatedBy(currentUserId);
        labOrderItemRepository.save(item);

        auditLogger.logCustomEvent("LAB_ORDER_UPDATED", Map.of(
                "encounterNumber", encounter.getEncounterNumber(),
                "orderNumber", order.getOrderNumber(),
                "testCode", item.getLabTest().getCode()
        ));
    }

    private LabTest requireOrderableTest(UUID labTestUuid) {
        LabTest test = labTestRepository.findByUuidAndDeletedAtIsNull(labTestUuid)
                .orElseThrow(() -> new EntityNotFoundException(
                        "Lab test not found with UUID: " + labTestUuid));
        if (!Boolean.TRUE.equals(test.getIsActive())) {
            throw new IllegalArgumentException(
                    "Cannot order an inactive lab test: " + test.getCode());
        }
        return test;
    }

    private void assertNotDuplicate(LabOrder order, LabTest test) {
        if (labOrderItemRepository.existsByLabOrderIdAndLabTestIdAndDeletedAtIsNull(
                order.getId(), test.getId())) {
            throw new IllegalArgumentException("This lab test is already included in the order");
        }
    }

    private void assertOrderDraft(LabOrder order) {
        if (order.getStatus() != LabOrderStatus.DRAFT) {
            throw new IllegalArgumentException(
                    "Lab order items can only be modified while the order is a draft");
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

    private LabOrderItem requireItem(LabOrder order, UUID itemUuid) {
        return labOrderItemRepository
                .findByUuidAndLabOrderIdAndDeletedAtIsNull(itemUuid, order.getId())
                .orElseThrow(() -> new EntityNotFoundException(
                        "Lab order item not found with UUID: " + itemUuid));
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

    private void assertVersion(LabOrderItem item, Long expectedVersion) {
        if (expectedVersion != null && !expectedVersion.equals(item.getVersion())) {
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

    private LabOrderItemResponse toResponse(LabOrderItem item) {
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
}
