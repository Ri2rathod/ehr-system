package com.example.ehrsystem.modules.encounter.controller;

import com.example.ehrsystem.common.response.PagedResponse;
import com.example.ehrsystem.modules.appointment.entity.VisitType;
import com.example.ehrsystem.modules.encounter.dto.request.CancelEncounterRequest;
import com.example.ehrsystem.modules.encounter.dto.request.CreateEncounterRequest;
import com.example.ehrsystem.modules.encounter.dto.request.RecordVitalsRequest;
import com.example.ehrsystem.modules.encounter.dto.request.UpdateEncounterRequest;
import com.example.ehrsystem.modules.encounter.dto.request.UpdateVitalsRequest;
import com.example.ehrsystem.modules.encounter.dto.response.EncounterResponse;
import com.example.ehrsystem.modules.encounter.dto.response.EncounterVitalsResponse;
import com.example.ehrsystem.modules.encounter.entity.EncounterStatus;
import com.example.ehrsystem.modules.encounter.service.EncounterService;
import com.example.ehrsystem.modules.encounter.service.EncounterVitalsService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/encounters")
@RequiredArgsConstructor
public class EncounterController {

    private static final int MAX_PAGE_SIZE = 100;
    private static final int DEFAULT_PAGE_SIZE = 20;

    private final EncounterService encounterService;
    private final EncounterVitalsService encounterVitalsService;

    private Pageable createPageable(int page, int size, String sortBy, String sortDir) {
        int actualSize = Math.min(size > 0 ? size : DEFAULT_PAGE_SIZE, MAX_PAGE_SIZE);
        int actualPage = Math.max(page, 0);
        Sort sort = sortDir.equalsIgnoreCase("asc")
                ? Sort.by(sortBy).ascending()
                : Sort.by(sortBy).descending();
        return PageRequest.of(actualPage, actualSize, sort);
    }

    @PostMapping
    @PreAuthorize("hasAuthority('PERM_ENCOUNTER_CREATE')")
    public ResponseEntity<EncounterResponse> create(@Valid @RequestBody CreateEncounterRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(encounterService.create(request));
    }

    @GetMapping("/{uuid}")
    @PreAuthorize("hasAnyAuthority('PERM_ENCOUNTER_VIEW', 'PERM_ENCOUNTER_VIEW_ALL')")
    public ResponseEntity<EncounterResponse> getByUuid(@PathVariable UUID uuid) {
        return ResponseEntity.ok(encounterService.getByUuid(uuid));
    }

    @GetMapping("/number/{encounterNumber}")
    @PreAuthorize("hasAnyAuthority('PERM_ENCOUNTER_VIEW', 'PERM_ENCOUNTER_VIEW_ALL')")
    public ResponseEntity<EncounterResponse> getByEncounterNumber(@PathVariable String encounterNumber) {
        return ResponseEntity.ok(encounterService.getByEncounterNumber(encounterNumber));
    }

    @GetMapping
    @PreAuthorize("hasAnyAuthority('PERM_ENCOUNTER_VIEW', 'PERM_ENCOUNTER_VIEW_ALL')")
    public ResponseEntity<PagedResponse<EncounterResponse>> search(
            @RequestParam(required = false) UUID patientUuid,
            @RequestParam(required = false) UUID doctorUuid,
            @RequestParam(required = false) UUID appointmentUuid,
            @RequestParam(required = false) EncounterStatus status,
            @RequestParam(required = false) VisitType encounterType,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dateFrom,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dateTo,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "startedAt") String sortBy,
            @RequestParam(defaultValue = "desc") String sortDir) {
        Pageable pageable = createPageable(page, size, sortBy, sortDir);
        Page<EncounterResponse> result = encounterService.search(
                patientUuid, doctorUuid, appointmentUuid, status, encounterType, dateFrom, dateTo, pageable);
        return ResponseEntity.ok(PagedResponse.of(
                result.getContent(), result.getNumber(), result.getSize(), result.getTotalElements()));
    }

    @PutMapping("/{uuid}")
    @PreAuthorize("hasAuthority('PERM_ENCOUNTER_UPDATE')")
    public ResponseEntity<EncounterResponse> update(
            @PathVariable UUID uuid,
            @Valid @RequestBody UpdateEncounterRequest request) {
        return ResponseEntity.ok(encounterService.update(uuid, request));
    }

    @PostMapping("/{uuid}/start")
    @PreAuthorize("hasAuthority('PERM_ENCOUNTER_START')")
    public ResponseEntity<EncounterResponse> start(@PathVariable UUID uuid) {
        return ResponseEntity.ok(encounterService.start(uuid));
    }

    @PostMapping("/{uuid}/complete")
    @PreAuthorize("hasAuthority('PERM_ENCOUNTER_COMPLETE')")
    public ResponseEntity<EncounterResponse> complete(
            @PathVariable UUID uuid,
            @Valid @RequestBody(required = false) UpdateEncounterRequest request) {
        return ResponseEntity.ok(encounterService.complete(uuid, request));
    }

    @PostMapping("/{uuid}/cancel")
    @PreAuthorize("hasAuthority('PERM_ENCOUNTER_CANCEL')")
    public ResponseEntity<EncounterResponse> cancel(
            @PathVariable UUID uuid,
            @Valid @RequestBody CancelEncounterRequest request) {
        return ResponseEntity.ok(encounterService.cancel(uuid, request));
    }

    @PostMapping("/{uuid}/vitals")
    @PreAuthorize("hasAuthority('PERM_VITALS_CREATE')")
    public ResponseEntity<EncounterVitalsResponse> recordVitals(
            @PathVariable UUID uuid,
            @Valid @RequestBody RecordVitalsRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(encounterVitalsService.record(uuid, request));
    }

    @GetMapping("/{uuid}/vitals")
    @PreAuthorize("hasAnyAuthority('PERM_VITALS_VIEW', 'PERM_ENCOUNTER_VIEW', 'PERM_ENCOUNTER_VIEW_ALL')")
    public ResponseEntity<List<EncounterVitalsResponse>> getVitals(@PathVariable UUID uuid) {
        return ResponseEntity.ok(encounterVitalsService.list(uuid));
    }

    @PutMapping("/{uuid}/vitals/{vitalsUuid}")
    @PreAuthorize("hasAuthority('PERM_VITALS_UPDATE')")
    public ResponseEntity<EncounterVitalsResponse> updateVitals(
            @PathVariable UUID uuid,
            @PathVariable UUID vitalsUuid,
            @Valid @RequestBody UpdateVitalsRequest request) {
        return ResponseEntity.ok(encounterVitalsService.update(uuid, vitalsUuid, request));
    }
}
