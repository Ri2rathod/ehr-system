package com.example.ehrsystem.modules.specimen.controller;

import com.example.ehrsystem.modules.specimen.dto.request.CollectSpecimenRequest;
import com.example.ehrsystem.modules.specimen.dto.request.CreateSpecimenRequest;
import com.example.ehrsystem.modules.specimen.dto.request.ReceiveSpecimenRequest;
import com.example.ehrsystem.modules.specimen.dto.request.RejectSpecimenRequest;
import com.example.ehrsystem.modules.specimen.dto.response.SpecimenResponse;
import com.example.ehrsystem.modules.specimen.service.SpecimenService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

/**
 * Specimen workflow nested under a lab order (not the encounter): lab
 * processing continues regardless of encounter status.
 * Explicit transition endpoints (collect/receive/reject/cancel);
 * create records the pending specimen.
 */
@RestController
@RequestMapping("/api/v1/lab-orders/{labOrderUuid}/specimens")
@RequiredArgsConstructor
public class SpecimenController {

    private final SpecimenService specimenService;

    @PostMapping
    @PreAuthorize("hasAuthority('PERM_SPECIMEN_COLLECT')")
    public ResponseEntity<SpecimenResponse> create(
            @PathVariable("labOrderUuid") UUID labOrderUuid,
            @Valid @RequestBody CreateSpecimenRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(specimenService.create(labOrderUuid, request));
    }

    @GetMapping
    @PreAuthorize("hasAuthority('PERM_SPECIMEN_VIEW')")
    public ResponseEntity<List<SpecimenResponse>> list(
            @PathVariable("labOrderUuid") UUID labOrderUuid) {
        return ResponseEntity.ok(specimenService.list(labOrderUuid));
    }

    @GetMapping("/{specimenUuid}")
    @PreAuthorize("hasAuthority('PERM_SPECIMEN_VIEW')")
    public ResponseEntity<SpecimenResponse> get(
            @PathVariable("labOrderUuid") UUID labOrderUuid,
            @PathVariable("specimenUuid") UUID specimenUuid) {
        return ResponseEntity.ok(specimenService.get(labOrderUuid, specimenUuid));
    }

    @PostMapping("/{specimenUuid}/collect")
    @PreAuthorize("hasAuthority('PERM_SPECIMEN_COLLECT')")
    public ResponseEntity<SpecimenResponse> collect(
            @PathVariable("labOrderUuid") UUID labOrderUuid,
            @PathVariable("specimenUuid") UUID specimenUuid,
            @RequestBody(required = false) @Valid CollectSpecimenRequest request) {
        return ResponseEntity.ok(specimenService.collect(
                labOrderUuid, specimenUuid, request != null ? request : new CollectSpecimenRequest()));
    }

    @PostMapping("/{specimenUuid}/receive")
    @PreAuthorize("hasAuthority('PERM_SPECIMEN_RECEIVE')")
    public ResponseEntity<SpecimenResponse> receive(
            @PathVariable("labOrderUuid") UUID labOrderUuid,
            @PathVariable("specimenUuid") UUID specimenUuid,
            @RequestBody(required = false) @Valid ReceiveSpecimenRequest request) {
        return ResponseEntity.ok(specimenService.receive(
                labOrderUuid, specimenUuid, request != null ? request : new ReceiveSpecimenRequest()));
    }

    @PostMapping("/{specimenUuid}/reject")
    @PreAuthorize("hasAuthority('PERM_SPECIMEN_REJECT')")
    public ResponseEntity<SpecimenResponse> reject(
            @PathVariable("labOrderUuid") UUID labOrderUuid,
            @PathVariable("specimenUuid") UUID specimenUuid,
            @Valid @RequestBody RejectSpecimenRequest request) {
        return ResponseEntity.ok(specimenService.reject(labOrderUuid, specimenUuid, request));
    }

    @PostMapping("/{specimenUuid}/cancel")
    @PreAuthorize("hasAuthority('PERM_SPECIMEN_COLLECT')")
    public ResponseEntity<SpecimenResponse> cancel(
            @PathVariable("labOrderUuid") UUID labOrderUuid,
            @PathVariable("specimenUuid") UUID specimenUuid) {
        return ResponseEntity.ok(specimenService.cancel(labOrderUuid, specimenUuid));
    }
}
