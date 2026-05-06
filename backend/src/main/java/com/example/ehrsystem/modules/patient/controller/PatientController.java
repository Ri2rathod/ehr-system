package com.example.ehrsystem.modules.patient.controller;

import com.example.ehrsystem.common.response.PagedResponse;
import com.example.ehrsystem.modules.patient.dto.request.CreatePatientRequest;
import com.example.ehrsystem.modules.patient.dto.response.PatientResponse;
import com.example.ehrsystem.modules.patient.service.PatientService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/patients")
@RequiredArgsConstructor
public class PatientController {

    private final PatientService patientService;

    @PostMapping
    @PreAuthorize("hasAuthority('PERM_PATIENT_CREATE')")
    public ResponseEntity<PatientResponse> create(@Valid @RequestBody CreatePatientRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(patientService.create(request));
    }

    @GetMapping
    @PreAuthorize("hasAuthority('PERM_PATIENT_READ')")
    public ResponseEntity<PagedResponse<PatientResponse>> getAll(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "createdAt") String sortBy,
            @RequestParam(defaultValue = "desc") String sortDir) {
        Sort sort = sortDir.equalsIgnoreCase("asc") 
                ? Sort.by(sortBy).ascending() 
                : Sort.by(sortBy).descending();
        Pageable pageable = PageRequest.of(page, size, sort);
        Page<PatientResponse> result = patientService.getAll(pageable);
        return ResponseEntity.ok(PagedResponse.of(
                result.getContent(), 
                result.getNumber(), 
                result.getSize(), 
                result.getTotalElements()));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('PERM_PATIENT_READ')")
    public ResponseEntity<PatientResponse> getById(@PathVariable Long id) {
        return ResponseEntity.ok(patientService.getById(id));
    }

    @GetMapping("/uuid/{uuid}")
    @PreAuthorize("hasAuthority('PERM_PATIENT_READ')")
    public ResponseEntity<PatientResponse> getByUuid(@PathVariable UUID uuid) {
        return ResponseEntity.ok(patientService.getByUuid(uuid));
    }

    @GetMapping("/mrn/{mrn}")
    @PreAuthorize("hasAuthority('PERM_PATIENT_READ')")
    public ResponseEntity<PatientResponse> getByMrn(@PathVariable String mrn) {
        return ResponseEntity.ok(patientService.getByMrn(mrn));
    }

    @GetMapping("/user/{userId}")
    @PreAuthorize("hasAuthority('PERM_PATIENT_READ')")
    public ResponseEntity<PatientResponse> getByUserId(@PathVariable Long userId) {
        return ResponseEntity.ok(patientService.getByUserId(userId));
    }

    @GetMapping("/search")
    @PreAuthorize("hasAuthority('PERM_PATIENT_READ')")
    public ResponseEntity<PagedResponse<PatientResponse>> search(
            @RequestParam String q,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        Pageable pageable = PageRequest.of(page, size);
        Page<PatientResponse> result = patientService.search(q, pageable);
        return ResponseEntity.ok(PagedResponse.of(
                result.getContent(), 
                result.getNumber(), 
                result.getSize(), 
                result.getTotalElements()));
    }

    @GetMapping("/status/{status}")
    @PreAuthorize("hasAuthority('PERM_PATIENT_READ')")
    public ResponseEntity<List<PatientResponse>> getByStatus(@PathVariable String status) {
        return ResponseEntity.ok(patientService.getByStatus(status));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('PERM_PATIENT_UPDATE')")
    public ResponseEntity<PatientResponse> update(@PathVariable Long id,
                                                   @Valid @RequestBody CreatePatientRequest request) {
        return ResponseEntity.ok(patientService.update(id, request));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('PERM_PATIENT_DELETE')")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        patientService.delete(id);
        return ResponseEntity.noContent().build();
    }
}