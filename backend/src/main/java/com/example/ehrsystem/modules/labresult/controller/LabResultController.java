package com.example.ehrsystem.modules.labresult.controller;

import com.example.ehrsystem.modules.labresult.dto.request.CorrectLabResultRequest;
import com.example.ehrsystem.modules.labresult.dto.request.CreateLabResultRequest;
import com.example.ehrsystem.modules.labresult.dto.request.UpdateLabResultRequest;
import com.example.ehrsystem.modules.labresult.dto.response.LabResultResponse;
import com.example.ehrsystem.modules.labresult.service.LabResultService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

/**
 * Lab results nested under a lab order (not the encounter): result entry
 * and finalization are lab processing and continue regardless of the
 * encounter status. Status changes use explicit transition endpoints
 * (finalize/correct/cancel); PUT only edits a preliminary result.
 */
@RestController
@RequestMapping("/api/v1/lab-orders/{labOrderUuid}/results")
@RequiredArgsConstructor
public class LabResultController {

    private final LabResultService labResultService;

    @GetMapping
    @PreAuthorize("hasAuthority('PERM_LAB_RESULT_VIEW')")
    public ResponseEntity<List<LabResultResponse>> list(
            @PathVariable("labOrderUuid") UUID labOrderUuid) {
        return ResponseEntity.ok(labResultService.list(labOrderUuid));
    }

    @GetMapping("/{resultUuid}")
    @PreAuthorize("hasAuthority('PERM_LAB_RESULT_VIEW')")
    public ResponseEntity<LabResultResponse> get(
            @PathVariable("labOrderUuid") UUID labOrderUuid,
            @PathVariable("resultUuid") UUID resultUuid) {
        return ResponseEntity.ok(labResultService.get(labOrderUuid, resultUuid));
    }

    @PostMapping
    @PreAuthorize("hasAuthority('PERM_LAB_RESULT_CREATE')")
    public ResponseEntity<LabResultResponse> create(
            @PathVariable("labOrderUuid") UUID labOrderUuid,
            @Valid @RequestBody CreateLabResultRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(labResultService.create(labOrderUuid, request));
    }

    @PutMapping("/{resultUuid}")
    @PreAuthorize("hasAuthority('PERM_LAB_RESULT_UPDATE')")
    public ResponseEntity<LabResultResponse> update(
            @PathVariable("labOrderUuid") UUID labOrderUuid,
            @PathVariable("resultUuid") UUID resultUuid,
            @Valid @RequestBody UpdateLabResultRequest request) {
        return ResponseEntity.ok(
                labResultService.update(labOrderUuid, resultUuid, request));
    }

    @PostMapping("/{resultUuid}/finalize")
    @PreAuthorize("hasAuthority('PERM_LAB_RESULT_FINALIZE')")
    public ResponseEntity<LabResultResponse> finalizeResult(
            @PathVariable("labOrderUuid") UUID labOrderUuid,
            @PathVariable("resultUuid") UUID resultUuid) {
        return ResponseEntity.ok(labResultService.finalizeResult(labOrderUuid, resultUuid));
    }

    @PostMapping("/{resultUuid}/correct")
    @PreAuthorize("hasAuthority('PERM_LAB_RESULT_CORRECT')")
    public ResponseEntity<LabResultResponse> correct(
            @PathVariable("labOrderUuid") UUID labOrderUuid,
            @PathVariable("resultUuid") UUID resultUuid,
            @Valid @RequestBody CorrectLabResultRequest request) {
        return ResponseEntity.ok(labResultService.correct(labOrderUuid, resultUuid, request));
    }

    @PostMapping("/{resultUuid}/cancel")
    @PreAuthorize("hasAuthority('PERM_LAB_RESULT_CANCEL')")
    public ResponseEntity<LabResultResponse> cancel(
            @PathVariable("labOrderUuid") UUID labOrderUuid,
            @PathVariable("resultUuid") UUID resultUuid) {
        return ResponseEntity.ok(labResultService.cancel(labOrderUuid, resultUuid));
    }
}
