package com.example.ehrsystem.modules.laborder.controller;

import com.example.ehrsystem.modules.laborder.dto.request.CreateLabOrderItemRequest;
import com.example.ehrsystem.modules.laborder.dto.request.CreateLabOrderRequest;
import com.example.ehrsystem.modules.laborder.dto.request.UpdateLabOrderItemRequest;
import com.example.ehrsystem.modules.laborder.dto.request.UpdateLabOrderRequest;
import com.example.ehrsystem.modules.laborder.dto.response.LabOrderItemResponse;
import com.example.ehrsystem.modules.laborder.dto.response.LabOrderResponse;
import com.example.ehrsystem.modules.laborder.dto.response.LabOrderSummaryResponse;
import com.example.ehrsystem.modules.laborder.service.LabOrderItemService;
import com.example.ehrsystem.modules.laborder.service.LabOrderService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

/**
 * Nested REST API: lab orders belong to an Encounter.
 * Status changes use explicit transition endpoints (order/start/complete/
 * cancel); PUT never changes status. Order creation and item edits are
 * clinical edits gated by the encounter status; lifecycle transitions are
 * part of lab processing and continue after the encounter completes.
 */
@RestController
@RequestMapping("/api/v1/encounters/{encounterUuid}/lab-orders")
@RequiredArgsConstructor
public class LabOrderController {

    private final LabOrderService labOrderService;
    private final LabOrderItemService labOrderItemService;

    @PostMapping
    @PreAuthorize("hasAuthority('PERM_LAB_ORDER_CREATE')")
    public ResponseEntity<LabOrderResponse> create(
            @PathVariable("encounterUuid") UUID encounterUuid,
            @Valid @RequestBody CreateLabOrderRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(labOrderService.create(encounterUuid, request));
    }

    @GetMapping
    @PreAuthorize("hasAuthority('PERM_LAB_ORDER_VIEW')")
    public ResponseEntity<List<LabOrderSummaryResponse>> list(
            @PathVariable("encounterUuid") UUID encounterUuid) {
        return ResponseEntity.ok(labOrderService.list(encounterUuid));
    }

    @GetMapping("/{labOrderUuid}")
    @PreAuthorize("hasAuthority('PERM_LAB_ORDER_VIEW')")
    public ResponseEntity<LabOrderResponse> get(
            @PathVariable("encounterUuid") UUID encounterUuid,
            @PathVariable("labOrderUuid") UUID labOrderUuid) {
        return ResponseEntity.ok(labOrderService.get(encounterUuid, labOrderUuid));
    }

    @PutMapping("/{labOrderUuid}")
    @PreAuthorize("hasAuthority('PERM_LAB_ORDER_UPDATE')")
    public ResponseEntity<LabOrderResponse> update(
            @PathVariable("encounterUuid") UUID encounterUuid,
            @PathVariable("labOrderUuid") UUID labOrderUuid,
            @Valid @RequestBody UpdateLabOrderRequest request) {
        return ResponseEntity.ok(
                labOrderService.update(encounterUuid, labOrderUuid, request));
    }

    @PostMapping("/{labOrderUuid}/order")
    @PreAuthorize("hasAuthority('PERM_LAB_ORDER_CREATE')")
    public ResponseEntity<LabOrderResponse> order(
            @PathVariable("encounterUuid") UUID encounterUuid,
            @PathVariable("labOrderUuid") UUID labOrderUuid) {
        return ResponseEntity.ok(labOrderService.order(encounterUuid, labOrderUuid));
    }

    @PostMapping("/{labOrderUuid}/start")
    @PreAuthorize("hasAuthority('PERM_LAB_ORDER_UPDATE')")
    public ResponseEntity<LabOrderResponse> start(
            @PathVariable("encounterUuid") UUID encounterUuid,
            @PathVariable("labOrderUuid") UUID labOrderUuid) {
        return ResponseEntity.ok(labOrderService.start(encounterUuid, labOrderUuid));
    }

    @PostMapping("/{labOrderUuid}/complete")
    @PreAuthorize("hasAuthority('PERM_LAB_ORDER_UPDATE')")
    public ResponseEntity<LabOrderResponse> complete(
            @PathVariable("encounterUuid") UUID encounterUuid,
            @PathVariable("labOrderUuid") UUID labOrderUuid) {
        return ResponseEntity.ok(labOrderService.complete(encounterUuid, labOrderUuid));
    }

    @PostMapping("/{labOrderUuid}/cancel")
    @PreAuthorize("hasAuthority('PERM_LAB_ORDER_CANCEL')")
    public ResponseEntity<LabOrderResponse> cancel(
            @PathVariable("encounterUuid") UUID encounterUuid,
            @PathVariable("labOrderUuid") UUID labOrderUuid) {
        return ResponseEntity.ok(labOrderService.cancel(encounterUuid, labOrderUuid));
    }

    @DeleteMapping("/{labOrderUuid}")
    @PreAuthorize("hasAuthority('PERM_LAB_ORDER_CANCEL')")
    public ResponseEntity<Void> delete(
            @PathVariable("encounterUuid") UUID encounterUuid,
            @PathVariable("labOrderUuid") UUID labOrderUuid) {
        labOrderService.delete(encounterUuid, labOrderUuid);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{labOrderUuid}/items")
    @PreAuthorize("hasAuthority('PERM_LAB_ORDER_CREATE')")
    public ResponseEntity<LabOrderItemResponse> addItem(
            @PathVariable("encounterUuid") UUID encounterUuid,
            @PathVariable("labOrderUuid") UUID labOrderUuid,
            @Valid @RequestBody CreateLabOrderItemRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(labOrderItemService.addItem(encounterUuid, labOrderUuid, request));
    }

    @PutMapping("/{labOrderUuid}/items/{itemUuid}")
    @PreAuthorize("hasAuthority('PERM_LAB_ORDER_UPDATE')")
    public ResponseEntity<LabOrderItemResponse> updateItem(
            @PathVariable("encounterUuid") UUID encounterUuid,
            @PathVariable("labOrderUuid") UUID labOrderUuid,
            @PathVariable("itemUuid") UUID itemUuid,
            @Valid @RequestBody UpdateLabOrderItemRequest request) {
        return ResponseEntity.ok(labOrderItemService.updateItem(
                encounterUuid, labOrderUuid, itemUuid, request));
    }

    @DeleteMapping("/{labOrderUuid}/items/{itemUuid}")
    @PreAuthorize("hasAuthority('PERM_LAB_ORDER_UPDATE')")
    public ResponseEntity<Void> deleteItem(
            @PathVariable("encounterUuid") UUID encounterUuid,
            @PathVariable("labOrderUuid") UUID labOrderUuid,
            @PathVariable("itemUuid") UUID itemUuid) {
        labOrderItemService.deleteItem(encounterUuid, labOrderUuid, itemUuid);
        return ResponseEntity.noContent().build();
    }
}
