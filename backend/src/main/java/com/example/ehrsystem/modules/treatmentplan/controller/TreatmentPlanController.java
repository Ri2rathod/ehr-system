package com.example.ehrsystem.modules.treatmentplan.controller;

import com.example.ehrsystem.modules.treatmentplan.dto.request.CreateTreatmentPlanRequest;
import com.example.ehrsystem.modules.treatmentplan.dto.request.UpdateTreatmentPlanRequest;
import com.example.ehrsystem.modules.treatmentplan.dto.response.TreatmentPlanResponse;
import com.example.ehrsystem.modules.treatmentplan.service.TreatmentPlanService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

/**
 * Nested REST API: treatment plans belong to an Encounter.
 * Status changes use explicit transition endpoints (activate/complete/cancel);
 * PUT never changes status.
 */
@RestController
@RequestMapping("/api/v1/encounters/{encounterUuid}/treatment-plans")
@RequiredArgsConstructor
public class TreatmentPlanController {

    private final TreatmentPlanService treatmentPlanService;

    @PostMapping
    @PreAuthorize("hasAuthority('PERM_TREATMENT_PLAN_CREATE')")
    public ResponseEntity<TreatmentPlanResponse> create(
            @PathVariable("encounterUuid") UUID encounterUuid,
            @Valid @RequestBody CreateTreatmentPlanRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(treatmentPlanService.create(encounterUuid, request));
    }

    @GetMapping
    @PreAuthorize("hasAuthority('PERM_TREATMENT_PLAN_VIEW')")
    public ResponseEntity<List<TreatmentPlanResponse>> list(
            @PathVariable("encounterUuid") UUID encounterUuid) {
        return ResponseEntity.ok(treatmentPlanService.list(encounterUuid));
    }

    @GetMapping("/{planUuid}")
    @PreAuthorize("hasAuthority('PERM_TREATMENT_PLAN_VIEW')")
    public ResponseEntity<TreatmentPlanResponse> get(
            @PathVariable("encounterUuid") UUID encounterUuid,
            @PathVariable("planUuid") UUID planUuid) {
        return ResponseEntity.ok(treatmentPlanService.get(encounterUuid, planUuid));
    }

    @PutMapping("/{planUuid}")
    @PreAuthorize("hasAuthority('PERM_TREATMENT_PLAN_UPDATE')")
    public ResponseEntity<TreatmentPlanResponse> update(
            @PathVariable("encounterUuid") UUID encounterUuid,
            @PathVariable("planUuid") UUID planUuid,
            @Valid @RequestBody UpdateTreatmentPlanRequest request) {
        return ResponseEntity.ok(treatmentPlanService.update(encounterUuid, planUuid, request));
    }

    @PostMapping("/{planUuid}/activate")
    @PreAuthorize("hasAuthority('PERM_TREATMENT_PLAN_ACTIVATE')")
    public ResponseEntity<TreatmentPlanResponse> activate(
            @PathVariable("encounterUuid") UUID encounterUuid,
            @PathVariable("planUuid") UUID planUuid) {
        return ResponseEntity.ok(treatmentPlanService.activate(encounterUuid, planUuid));
    }

    @PostMapping("/{planUuid}/complete")
    @PreAuthorize("hasAuthority('PERM_TREATMENT_PLAN_COMPLETE')")
    public ResponseEntity<TreatmentPlanResponse> complete(
            @PathVariable("encounterUuid") UUID encounterUuid,
            @PathVariable("planUuid") UUID planUuid) {
        return ResponseEntity.ok(treatmentPlanService.complete(encounterUuid, planUuid));
    }

    @PostMapping("/{planUuid}/cancel")
    @PreAuthorize("hasAuthority('PERM_TREATMENT_PLAN_CANCEL')")
    public ResponseEntity<TreatmentPlanResponse> cancel(
            @PathVariable("encounterUuid") UUID encounterUuid,
            @PathVariable("planUuid") UUID planUuid) {
        return ResponseEntity.ok(treatmentPlanService.cancel(encounterUuid, planUuid));
    }

    @DeleteMapping("/{planUuid}")
    @PreAuthorize("hasAuthority('PERM_TREATMENT_PLAN_DELETE')")
    public ResponseEntity<Void> delete(
            @PathVariable("encounterUuid") UUID encounterUuid,
            @PathVariable("planUuid") UUID planUuid) {
        treatmentPlanService.delete(encounterUuid, planUuid);
        return ResponseEntity.noContent().build();
    }
}
