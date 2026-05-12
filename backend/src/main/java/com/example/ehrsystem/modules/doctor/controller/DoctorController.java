package com.example.ehrsystem.modules.doctor.controller;

import com.example.ehrsystem.common.response.PagedResponse;
import com.example.ehrsystem.modules.doctor.dto.request.CreateDoctorRequest;
import com.example.ehrsystem.modules.doctor.dto.request.UpdateDoctorRequest;
import com.example.ehrsystem.modules.doctor.dto.response.DoctorResponse;
import com.example.ehrsystem.modules.doctor.service.DoctorService;
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

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/doctors")
@RequiredArgsConstructor
public class DoctorController {

    private static final int MAX_PAGE_SIZE = 100;
    private static final int DEFAULT_PAGE_SIZE = 20;

    private final DoctorService doctorService;

    private Pageable createPageable(int page, int size, String sortBy, String sortDir) {
        int actualSize = Math.min(size > 0 ? size : DEFAULT_PAGE_SIZE, MAX_PAGE_SIZE);
        int actualPage = Math.max(page, 0);
        Sort sort = sortDir.equalsIgnoreCase("asc")
                ? Sort.by(sortBy).ascending()
                : Sort.by(sortBy).descending();
        return PageRequest.of(actualPage, actualSize, sort);
    }

    @PostMapping
    @PreAuthorize("hasAuthority('PERM_DOCTOR_CREATE')")
    public ResponseEntity<DoctorResponse> create(@Valid @RequestBody CreateDoctorRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(doctorService.create(request));
    }

    @GetMapping
    @PreAuthorize("hasAuthority('PERM_DOCTOR_READ')")
    public ResponseEntity<PagedResponse<DoctorResponse>> getAll(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "createdAt") String sortBy,
            @RequestParam(defaultValue = "desc") String sortDir) {
        Pageable pageable = createPageable(page, size, sortBy, sortDir);
        Page<DoctorResponse> result = doctorService.getAll(pageable);
        return ResponseEntity.ok(PagedResponse.of(
                result.getContent(), result.getNumber(), result.getSize(), result.getTotalElements()));
    }

    @GetMapping("/{uuid}")
    @PreAuthorize("hasAuthority('PERM_DOCTOR_READ')")
    public ResponseEntity<DoctorResponse> getByUuid(@PathVariable UUID uuid) {
        return ResponseEntity.ok(doctorService.getByUuid(uuid));
    }

    @GetMapping("/code/{doctorCode}")
    @PreAuthorize("hasAuthority('PERM_DOCTOR_READ')")
    public ResponseEntity<DoctorResponse> getByDoctorCode(@PathVariable String doctorCode) {
        return ResponseEntity.ok(doctorService.getByDoctorCode(doctorCode));
    }

    @GetMapping("/search")
    @PreAuthorize("hasAuthority('PERM_DOCTOR_READ')")
    public ResponseEntity<PagedResponse<DoctorResponse>> search(
            @RequestParam String q,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        Pageable pageable = createPageable(page, size, "createdAt", "desc");
        Page<DoctorResponse> result = doctorService.search(q, pageable);
        return ResponseEntity.ok(PagedResponse.of(
                result.getContent(), result.getNumber(), result.getSize(), result.getTotalElements()));
    }

    @GetMapping("/status/{status}")
    @PreAuthorize("hasAuthority('PERM_DOCTOR_READ')")
    public ResponseEntity<PagedResponse<DoctorResponse>> getByStatus(
            @PathVariable String status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        Pageable pageable = createPageable(page, size, "createdAt", "desc");
        Page<DoctorResponse> result = doctorService.getByStatus(status, pageable);
        return ResponseEntity.ok(PagedResponse.of(
                result.getContent(), result.getNumber(), result.getSize(), result.getTotalElements()));
    }

    @GetMapping("/specialization/{specialization}")
    @PreAuthorize("hasAuthority('PERM_DOCTOR_READ')")
    public ResponseEntity<PagedResponse<DoctorResponse>> getBySpecialization(
            @PathVariable String specialization,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        Pageable pageable = createPageable(page, size, "createdAt", "desc");
        Page<DoctorResponse> result = doctorService.getBySpecialization(specialization, pageable);
        return ResponseEntity.ok(PagedResponse.of(
                result.getContent(), result.getNumber(), result.getSize(), result.getTotalElements()));
    }

    @GetMapping("/available")
    @PreAuthorize("hasAuthority('PERM_DOCTOR_READ')")
    public ResponseEntity<PagedResponse<DoctorResponse>> getAvailable(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        Pageable pageable = createPageable(page, size, "createdAt", "desc");
        Page<DoctorResponse> result = doctorService.getAvailable(pageable);
        return ResponseEntity.ok(PagedResponse.of(
                result.getContent(), result.getNumber(), result.getSize(), result.getTotalElements()));
    }

    @PutMapping("/{uuid}")
    @PreAuthorize("hasAuthority('PERM_DOCTOR_UPDATE')")
    public ResponseEntity<DoctorResponse> update(
            @PathVariable UUID uuid,
            @Valid @RequestBody UpdateDoctorRequest request) {
        return ResponseEntity.ok(doctorService.update(uuid, request));
    }

    @DeleteMapping("/{uuid}")
    @PreAuthorize("hasAuthority('PERM_DOCTOR_DELETE')")
    public ResponseEntity<Void> delete(@PathVariable UUID uuid) {
        doctorService.delete(uuid);
        return ResponseEntity.noContent().build();
    }
}