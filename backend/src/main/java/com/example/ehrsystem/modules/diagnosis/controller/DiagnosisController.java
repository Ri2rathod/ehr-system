package com.example.ehrsystem.modules.diagnosis.controller;

import com.example.ehrsystem.modules.diagnosis.dto.request.CreateDiagnosisRequest;
import com.example.ehrsystem.modules.diagnosis.dto.request.UpdateDiagnosisRequest;
import com.example.ehrsystem.modules.diagnosis.dto.response.DiagnosisResponse;
import com.example.ehrsystem.modules.diagnosis.service.DiagnosisService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

/**
 * Nested REST API: diagnoses belong to an Encounter.
 * The encounter is always identified by the route — never by the request body.
 */
@RestController
@RequestMapping("/api/v1/encounters/{encounterUuid}/diagnoses")
@RequiredArgsConstructor
public class DiagnosisController {

    private final DiagnosisService diagnosisService;

    @PostMapping
    @PreAuthorize("hasAuthority('PERM_DIAGNOSIS_CREATE')")
    public ResponseEntity<DiagnosisResponse> create(
            @PathVariable("encounterUuid") UUID encounterUuid,
            @Valid @RequestBody CreateDiagnosisRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(diagnosisService.create(encounterUuid, request));
    }

    @GetMapping
    @PreAuthorize("hasAuthority('PERM_DIAGNOSIS_VIEW')")
    public ResponseEntity<List<DiagnosisResponse>> list(
            @PathVariable("encounterUuid") UUID encounterUuid) {
        return ResponseEntity.ok(diagnosisService.list(encounterUuid));
    }

    @GetMapping("/{diagnosisUuid}")
    @PreAuthorize("hasAuthority('PERM_DIAGNOSIS_VIEW')")
    public ResponseEntity<DiagnosisResponse> get(
            @PathVariable("encounterUuid") UUID encounterUuid,
            @PathVariable("diagnosisUuid") UUID diagnosisUuid) {
        return ResponseEntity.ok(diagnosisService.get(encounterUuid, diagnosisUuid));
    }

    @PutMapping("/{diagnosisUuid}")
    @PreAuthorize("hasAuthority('PERM_DIAGNOSIS_UPDATE')")
    public ResponseEntity<DiagnosisResponse> update(
            @PathVariable("encounterUuid") UUID encounterUuid,
            @PathVariable("diagnosisUuid") UUID diagnosisUuid,
            @Valid @RequestBody UpdateDiagnosisRequest request) {
        return ResponseEntity.ok(diagnosisService.update(encounterUuid, diagnosisUuid, request));
    }

    @DeleteMapping("/{diagnosisUuid}")
    @PreAuthorize("hasAuthority('PERM_DIAGNOSIS_DEACTIVATE')")
    public ResponseEntity<Void> deactivate(
            @PathVariable("encounterUuid") UUID encounterUuid,
            @PathVariable("diagnosisUuid") UUID diagnosisUuid) {
        diagnosisService.deactivate(encounterUuid, diagnosisUuid);
        return ResponseEntity.noContent().build();
    }
}
