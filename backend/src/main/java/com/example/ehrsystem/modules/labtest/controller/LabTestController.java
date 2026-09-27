package com.example.ehrsystem.modules.labtest.controller;

import com.example.ehrsystem.common.response.PagedResponse;
import com.example.ehrsystem.modules.labtest.dto.request.CreateLabTestRequest;
import com.example.ehrsystem.modules.labtest.dto.request.UpdateLabTestRequest;
import com.example.ehrsystem.modules.labtest.dto.response.LabTestResponse;
import com.example.ehrsystem.modules.labtest.dto.response.LabTestSummaryResponse;
import com.example.ehrsystem.modules.labtest.service.LabTestService;
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
 * Lab test catalog: server-side paginated search for order autocomplete
 * + catalog management.
 */
@RestController
@RequestMapping("/api/v1/lab-tests")
@RequiredArgsConstructor
public class LabTestController {

    private static final int MAX_PAGE_SIZE = 100;
    private static final int DEFAULT_PAGE_SIZE = 20;

    private final LabTestService labTestService;

    private Pageable createPageable(int page, int size, String sortBy, String sortDir) {
        int actualSize = Math.min(size > 0 ? size : DEFAULT_PAGE_SIZE, MAX_PAGE_SIZE);
        int actualPage = Math.max(page, 0);
        Sort sort = sortDir.equalsIgnoreCase("asc")
                ? Sort.by(sortBy).ascending()
                : Sort.by(sortBy).descending();
        return PageRequest.of(actualPage, actualSize, sort);
    }

    @GetMapping
    @PreAuthorize("hasAuthority('PERM_LAB_TEST_VIEW')")
    public ResponseEntity<PagedResponse<LabTestSummaryResponse>> search(
            @RequestParam(required = false) String query,
            @RequestParam(required = false) String code,
            @RequestParam(required = false) String category,
            @RequestParam(required = false) Boolean active,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "name") String sortBy,
            @RequestParam(defaultValue = "asc") String sortDir) {
        Pageable pageable = createPageable(page, size, sortBy, sortDir);
        var result = labTestService.search(query, code, category, active, pageable);
        return ResponseEntity.ok(PagedResponse.of(
                result.getContent(), result.getNumber(), result.getSize(), result.getTotalElements()));
    }

    @GetMapping("/{labTestUuid}")
    @PreAuthorize("hasAuthority('PERM_LAB_TEST_VIEW')")
    public ResponseEntity<LabTestResponse> get(
            @PathVariable("labTestUuid") UUID labTestUuid) {
        return ResponseEntity.ok(labTestService.get(labTestUuid));
    }

    @PostMapping
    @PreAuthorize("hasAuthority('PERM_LAB_TEST_MANAGE')")
    public ResponseEntity<LabTestResponse> create(
            @Valid @RequestBody CreateLabTestRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(labTestService.create(request));
    }

    @PutMapping("/{labTestUuid}")
    @PreAuthorize("hasAuthority('PERM_LAB_TEST_MANAGE')")
    public ResponseEntity<LabTestResponse> update(
            @PathVariable("labTestUuid") UUID labTestUuid,
            @Valid @RequestBody UpdateLabTestRequest request) {
        return ResponseEntity.ok(labTestService.update(labTestUuid, request));
    }

    @DeleteMapping("/{labTestUuid}")
    @PreAuthorize("hasAuthority('PERM_LAB_TEST_MANAGE')")
    public ResponseEntity<Void> delete(
            @PathVariable("labTestUuid") UUID labTestUuid) {
        labTestService.delete(labTestUuid);
        return ResponseEntity.noContent().build();
    }
}
