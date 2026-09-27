package com.example.ehrsystem.modules.prescription.controller;

import com.example.ehrsystem.modules.prescription.dto.request.CreatePrescriptionItemRequest;
import com.example.ehrsystem.modules.prescription.dto.request.UpdatePrescriptionItemRequest;
import com.example.ehrsystem.modules.prescription.dto.response.PrescriptionItemResponse;
import com.example.ehrsystem.modules.prescription.service.PrescriptionItemService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

/**
 * Nested REST API: prescription items belong to a Prescription,
 * which belongs to an Encounter - the route always identifies both.
 * Status changes use explicit transition endpoints
 * (complete/cancel/discontinue); PUT never changes status.
 * Completing an item reuses PRESCRIPTION_ITEM_UPDATE (the spec's
 * permission model has no separate item-complete permission).
 */
@RestController
@RequestMapping("/api/v1/encounters/{encounterUuid}/prescriptions/{prescriptionUuid}/items")
@RequiredArgsConstructor
public class PrescriptionItemController {

    private final PrescriptionItemService prescriptionItemService;

    @PostMapping
    @PreAuthorize("hasAuthority('PERM_PRESCRIPTION_ITEM_CREATE')")
    public ResponseEntity<PrescriptionItemResponse> create(
            @PathVariable("encounterUuid") UUID encounterUuid,
            @PathVariable("prescriptionUuid") UUID prescriptionUuid,
            @Valid @RequestBody CreatePrescriptionItemRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(prescriptionItemService.create(encounterUuid, prescriptionUuid, request));
    }

    @GetMapping
    @PreAuthorize("hasAuthority('PERM_PRESCRIPTION_ITEM_VIEW')")
    public ResponseEntity<List<PrescriptionItemResponse>> list(
            @PathVariable("encounterUuid") UUID encounterUuid,
            @PathVariable("prescriptionUuid") UUID prescriptionUuid) {
        return ResponseEntity.ok(
                prescriptionItemService.list(encounterUuid, prescriptionUuid));
    }

    @PutMapping("/{itemUuid}")
    @PreAuthorize("hasAuthority('PERM_PRESCRIPTION_ITEM_UPDATE')")
    public ResponseEntity<PrescriptionItemResponse> update(
            @PathVariable("encounterUuid") UUID encounterUuid,
            @PathVariable("prescriptionUuid") UUID prescriptionUuid,
            @PathVariable("itemUuid") UUID itemUuid,
            @Valid @RequestBody UpdatePrescriptionItemRequest request) {
        return ResponseEntity.ok(prescriptionItemService.update(
                encounterUuid, prescriptionUuid, itemUuid, request));
    }

    @PostMapping("/{itemUuid}/complete")
    @PreAuthorize("hasAuthority('PERM_PRESCRIPTION_ITEM_UPDATE')")
    public ResponseEntity<PrescriptionItemResponse> complete(
            @PathVariable("encounterUuid") UUID encounterUuid,
            @PathVariable("prescriptionUuid") UUID prescriptionUuid,
            @PathVariable("itemUuid") UUID itemUuid) {
        return ResponseEntity.ok(prescriptionItemService.complete(
                encounterUuid, prescriptionUuid, itemUuid));
    }

    @PostMapping("/{itemUuid}/discontinue")
    @PreAuthorize("hasAuthority('PERM_PRESCRIPTION_ITEM_DISCONTINUE')")
    public ResponseEntity<PrescriptionItemResponse> discontinue(
            @PathVariable("encounterUuid") UUID encounterUuid,
            @PathVariable("prescriptionUuid") UUID prescriptionUuid,
            @PathVariable("itemUuid") UUID itemUuid) {
        return ResponseEntity.ok(prescriptionItemService.discontinue(
                encounterUuid, prescriptionUuid, itemUuid));
    }

    @PostMapping("/{itemUuid}/cancel")
    @PreAuthorize("hasAuthority('PERM_PRESCRIPTION_ITEM_CANCEL')")
    public ResponseEntity<PrescriptionItemResponse> cancel(
            @PathVariable("encounterUuid") UUID encounterUuid,
            @PathVariable("prescriptionUuid") UUID prescriptionUuid,
            @PathVariable("itemUuid") UUID itemUuid) {
        return ResponseEntity.ok(prescriptionItemService.cancel(
                encounterUuid, prescriptionUuid, itemUuid));
    }

    @DeleteMapping("/{itemUuid}")
    @PreAuthorize("hasAuthority('PERM_PRESCRIPTION_ITEM_DELETE')")
    public ResponseEntity<Void> delete(
            @PathVariable("encounterUuid") UUID encounterUuid,
            @PathVariable("prescriptionUuid") UUID prescriptionUuid,
            @PathVariable("itemUuid") UUID itemUuid) {
        prescriptionItemService.delete(encounterUuid, prescriptionUuid, itemUuid);
        return ResponseEntity.noContent().build();
    }
}
