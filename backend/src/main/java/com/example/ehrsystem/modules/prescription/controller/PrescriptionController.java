package com.example.ehrsystem.modules.prescription.controller;

import com.example.ehrsystem.modules.prescription.dto.request.CreatePrescriptionRequest;
import com.example.ehrsystem.modules.prescription.dto.request.UpdatePrescriptionRequest;
import com.example.ehrsystem.modules.prescription.dto.response.PrescriptionResponse;
import com.example.ehrsystem.modules.prescription.service.PrescriptionService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

/**
 * Nested REST API: prescriptions belong to an Encounter.
 * Status changes use explicit transition endpoints
 * (activate/complete/cancel/void); PUT never changes status.
 * patient and doctor are always derived from the Encounter server-side.
 */
@RestController
@RequestMapping("/api/v1/encounters/{encounterUuid}/prescriptions")
@RequiredArgsConstructor
public class PrescriptionController {

    private final PrescriptionService prescriptionService;

    @PostMapping
    @PreAuthorize("hasAuthority('PERM_PRESCRIPTION_CREATE')")
    public ResponseEntity<PrescriptionResponse> create(
            @PathVariable("encounterUuid") UUID encounterUuid,
            @Valid @RequestBody CreatePrescriptionRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(prescriptionService.create(encounterUuid, request));
    }

    @GetMapping
    @PreAuthorize("hasAuthority('PERM_PRESCRIPTION_VIEW')")
    public ResponseEntity<List<PrescriptionResponse>> list(
            @PathVariable("encounterUuid") UUID encounterUuid) {
        return ResponseEntity.ok(prescriptionService.list(encounterUuid));
    }

    @GetMapping("/{prescriptionUuid}")
    @PreAuthorize("hasAuthority('PERM_PRESCRIPTION_VIEW')")
    public ResponseEntity<PrescriptionResponse> get(
            @PathVariable("encounterUuid") UUID encounterUuid,
            @PathVariable("prescriptionUuid") UUID prescriptionUuid) {
        return ResponseEntity.ok(prescriptionService.get(encounterUuid, prescriptionUuid));
    }

    @PutMapping("/{prescriptionUuid}")
    @PreAuthorize("hasAuthority('PERM_PRESCRIPTION_UPDATE')")
    public ResponseEntity<PrescriptionResponse> update(
            @PathVariable("encounterUuid") UUID encounterUuid,
            @PathVariable("prescriptionUuid") UUID prescriptionUuid,
            @Valid @RequestBody UpdatePrescriptionRequest request) {
        return ResponseEntity.ok(
                prescriptionService.update(encounterUuid, prescriptionUuid, request));
    }

    @PostMapping("/{prescriptionUuid}/activate")
    @PreAuthorize("hasAuthority('PERM_PRESCRIPTION_ACTIVATE')")
    public ResponseEntity<PrescriptionResponse> activate(
            @PathVariable("encounterUuid") UUID encounterUuid,
            @PathVariable("prescriptionUuid") UUID prescriptionUuid) {
        return ResponseEntity.ok(prescriptionService.activate(encounterUuid, prescriptionUuid));
    }

    @PostMapping("/{prescriptionUuid}/complete")
    @PreAuthorize("hasAuthority('PERM_PRESCRIPTION_COMPLETE')")
    public ResponseEntity<PrescriptionResponse> complete(
            @PathVariable("encounterUuid") UUID encounterUuid,
            @PathVariable("prescriptionUuid") UUID prescriptionUuid) {
        return ResponseEntity.ok(prescriptionService.complete(encounterUuid, prescriptionUuid));
    }

    @PostMapping("/{prescriptionUuid}/cancel")
    @PreAuthorize("hasAuthority('PERM_PRESCRIPTION_CANCEL')")
    public ResponseEntity<PrescriptionResponse> cancel(
            @PathVariable("encounterUuid") UUID encounterUuid,
            @PathVariable("prescriptionUuid") UUID prescriptionUuid) {
        return ResponseEntity.ok(prescriptionService.cancel(encounterUuid, prescriptionUuid));
    }

    @PostMapping("/{prescriptionUuid}/void")
    @PreAuthorize("hasAuthority('PERM_PRESCRIPTION_VOID')")
    public ResponseEntity<PrescriptionResponse> voidPrescription(
            @PathVariable("encounterUuid") UUID encounterUuid,
            @PathVariable("prescriptionUuid") UUID prescriptionUuid) {
        return ResponseEntity.ok(prescriptionService.voidPrescription(encounterUuid, prescriptionUuid));
    }

    @DeleteMapping("/{prescriptionUuid}")
    @PreAuthorize("hasAuthority('PERM_PRESCRIPTION_DELETE')")
    public ResponseEntity<Void> delete(
            @PathVariable("encounterUuid") UUID encounterUuid,
            @PathVariable("prescriptionUuid") UUID prescriptionUuid) {
        prescriptionService.delete(encounterUuid, prescriptionUuid);
        return ResponseEntity.noContent().build();
    }
}
