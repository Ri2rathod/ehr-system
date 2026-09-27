package com.example.ehrsystem.modules.treatmentplan.controller;

import com.example.ehrsystem.modules.treatmentplan.dto.request.CreateTreatmentPlanItemRequest;
import com.example.ehrsystem.modules.treatmentplan.dto.request.UpdateTreatmentPlanItemRequest;
import com.example.ehrsystem.modules.treatmentplan.dto.response.TreatmentPlanItemResponse;
import com.example.ehrsystem.modules.treatmentplan.service.TreatmentPlanItemService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

/**
 * Nested REST API: treatment items belong to a Treatment Plan,
 * which belongs to an Encounter — the route always identifies both.
 * Status changes use explicit transition endpoints (start/complete/cancel);
 * PUT never changes status.
 */
@RestController
@RequestMapping("/api/v1/encounters/{encounterUuid}/treatment-plans/{planUuid}/items")
@RequiredArgsConstructor
public class TreatmentPlanItemController {

    private final TreatmentPlanItemService treatmentPlanItemService;

    @PostMapping
    @PreAuthorize("hasAuthority('PERM_TREATMENT_ITEM_CREATE')")
    public ResponseEntity<TreatmentPlanItemResponse> create(
            @PathVariable("encounterUuid") UUID encounterUuid,
            @PathVariable("planUuid") UUID planUuid,
            @Valid @RequestBody CreateTreatmentPlanItemRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(treatmentPlanItemService.create(encounterUuid, planUuid, request));
    }

    @GetMapping
    @PreAuthorize("hasAuthority('PERM_TREATMENT_ITEM_VIEW')")
    public ResponseEntity<List<TreatmentPlanItemResponse>> list(
            @PathVariable("encounterUuid") UUID encounterUuid,
            @PathVariable("planUuid") UUID planUuid) {
        return ResponseEntity.ok(treatmentPlanItemService.list(encounterUuid, planUuid));
    }

    @PutMapping("/{itemUuid}")
    @PreAuthorize("hasAuthority('PERM_TREATMENT_ITEM_UPDATE')")
    public ResponseEntity<TreatmentPlanItemResponse> update(
            @PathVariable("encounterUuid") UUID encounterUuid,
            @PathVariable("planUuid") UUID planUuid,
            @PathVariable("itemUuid") UUID itemUuid,
            @Valid @RequestBody UpdateTreatmentPlanItemRequest request) {
        return ResponseEntity.ok(
                treatmentPlanItemService.update(encounterUuid, planUuid, itemUuid, request));
    }

    @PostMapping("/{itemUuid}/start")
    @PreAuthorize("hasAuthority('PERM_TREATMENT_ITEM_START')")
    public ResponseEntity<TreatmentPlanItemResponse> start(
            @PathVariable("encounterUuid") UUID encounterUuid,
            @PathVariable("planUuid") UUID planUuid,
            @PathVariable("itemUuid") UUID itemUuid) {
        return ResponseEntity.ok(
                treatmentPlanItemService.start(encounterUuid, planUuid, itemUuid));
    }

    @PostMapping("/{itemUuid}/complete")
    @PreAuthorize("hasAuthority('PERM_TREATMENT_ITEM_COMPLETE')")
    public ResponseEntity<TreatmentPlanItemResponse> complete(
            @PathVariable("encounterUuid") UUID encounterUuid,
            @PathVariable("planUuid") UUID planUuid,
            @PathVariable("itemUuid") UUID itemUuid) {
        return ResponseEntity.ok(
                treatmentPlanItemService.complete(encounterUuid, planUuid, itemUuid));
    }

    @PostMapping("/{itemUuid}/cancel")
    @PreAuthorize("hasAuthority('PERM_TREATMENT_ITEM_CANCEL')")
    public ResponseEntity<TreatmentPlanItemResponse> cancel(
            @PathVariable("encounterUuid") UUID encounterUuid,
            @PathVariable("planUuid") UUID planUuid,
            @PathVariable("itemUuid") UUID itemUuid) {
        return ResponseEntity.ok(
                treatmentPlanItemService.cancel(encounterUuid, planUuid, itemUuid));
    }

    @DeleteMapping("/{itemUuid}")
    @PreAuthorize("hasAuthority('PERM_TREATMENT_ITEM_DELETE')")
    public ResponseEntity<Void> delete(
            @PathVariable("encounterUuid") UUID encounterUuid,
            @PathVariable("planUuid") UUID planUuid,
            @PathVariable("itemUuid") UUID itemUuid) {
        treatmentPlanItemService.delete(encounterUuid, planUuid, itemUuid);
        return ResponseEntity.noContent().build();
    }
}
