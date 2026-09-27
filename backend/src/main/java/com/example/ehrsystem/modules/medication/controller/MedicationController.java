package com.example.ehrsystem.modules.medication.controller;

import com.example.ehrsystem.common.response.PagedResponse;
import com.example.ehrsystem.modules.medication.dto.request.CreateMedicationRequest;
import com.example.ehrsystem.modules.medication.dto.request.UpdateMedicationRequest;
import com.example.ehrsystem.modules.medication.dto.response.MedicationResponse;
import com.example.ehrsystem.modules.medication.entity.DosageForm;
import com.example.ehrsystem.modules.medication.service.MedicationService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

/**
 * Medication catalog: server-side paginated search for prescribing
 * autocomplete + admin-only catalog management.
 */
@RestController
@RequestMapping("/api/v1/medications")
@RequiredArgsConstructor
public class MedicationController {

    private static final int MAX_PAGE_SIZE = 100;
    private static final int DEFAULT_PAGE_SIZE = 20;

    private final MedicationService medicationService;

    private Pageable createPageable(int page, int size, String sortBy, String sortDir) {
        int actualSize = Math.min(size > 0 ? size : DEFAULT_PAGE_SIZE, MAX_PAGE_SIZE);
        int actualPage = Math.max(page, 0);
        Sort sort = sortDir.equalsIgnoreCase("asc")
                ? Sort.by(sortBy).ascending()
                : Sort.by(sortBy).descending();
        return PageRequest.of(actualPage, actualSize, sort);
    }

    @GetMapping
    @PreAuthorize("hasAuthority('PERM_MEDICATION_VIEW')")
    public ResponseEntity<PagedResponse<MedicationResponse>> search(
            @RequestParam(required = false) String query,
            @RequestParam(required = false) DosageForm dosageForm,
            @RequestParam(defaultValue = "false") boolean includeInactive,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "genericName") String sortBy,
            @RequestParam(defaultValue = "asc") String sortDir) {
        Pageable pageable = createPageable(page, size, sortBy, sortDir);
        var result = medicationService.search(query, dosageForm, includeInactive, pageable);
        return ResponseEntity.ok(PagedResponse.of(
                result.getContent(), result.getNumber(), result.getSize(), result.getTotalElements()));
    }

    @GetMapping("/{medicationUuid}")
    @PreAuthorize("hasAuthority('PERM_MEDICATION_VIEW')")
    public ResponseEntity<MedicationResponse> get(
            @PathVariable("medicationUuid") UUID medicationUuid) {
        return ResponseEntity.ok(medicationService.get(medicationUuid));
    }

    @PostMapping
    @PreAuthorize("hasAuthority('PERM_MEDICATION_CREATE')")
    public ResponseEntity<MedicationResponse> create(
            @Valid @RequestBody CreateMedicationRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(medicationService.create(request));
    }

    @PutMapping("/{medicationUuid}")
    @PreAuthorize("hasAuthority('PERM_MEDICATION_UPDATE')")
    public ResponseEntity<MedicationResponse> update(
            @PathVariable("medicationUuid") UUID medicationUuid,
            @Valid @RequestBody UpdateMedicationRequest request) {
        return ResponseEntity.ok(medicationService.update(medicationUuid, request));
    }

    @DeleteMapping("/{medicationUuid}")
    @PreAuthorize("hasAuthority('PERM_MEDICATION_DEACTIVATE')")
    public ResponseEntity<Void> deactivate(
            @PathVariable("medicationUuid") UUID medicationUuid) {
        medicationService.deactivate(medicationUuid);
        return ResponseEntity.noContent().build();
    }
}
