package com.example.ehrsystem.modules.scheduling.controller;

import com.example.ehrsystem.modules.scheduling.dto.request.CreateAvailabilityRequest;
import com.example.ehrsystem.modules.scheduling.dto.request.CreateExceptionRequest;
import com.example.ehrsystem.modules.scheduling.dto.request.UpdateAvailabilityRequest;
import com.example.ehrsystem.modules.scheduling.dto.request.UpdateExceptionRequest;
import com.example.ehrsystem.modules.scheduling.dto.response.AvailabilityExceptionResponse;
import com.example.ehrsystem.modules.scheduling.dto.response.AvailabilityResponse;
import com.example.ehrsystem.modules.scheduling.dto.response.AvailableSlotsResponse;
import com.example.ehrsystem.modules.scheduling.service.DoctorAvailabilityExceptionService;
import com.example.ehrsystem.modules.scheduling.service.DoctorAvailabilityService;
import com.example.ehrsystem.modules.scheduling.service.SlotGenerationService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/doctors/{doctorUuid}")
@RequiredArgsConstructor
public class DoctorSchedulingController {

    private final DoctorAvailabilityService availabilityService;
    private final DoctorAvailabilityExceptionService exceptionService;
    private final SlotGenerationService slotGenerationService;

    // ═══════════════════════════════════════════════════════════
    // Weekly Availability Schedule APIs
    // ═══════════════════════════════════════════════════════════

    /**
     * GET /api/v1/doctors/{doctorUuid}/availability
     * Returns the complete weekly schedule (active blocks only).
     */
    @GetMapping("/availability")
    @PreAuthorize("hasAuthority('PERM_DOCTOR_READ')")
    public ResponseEntity<List<AvailabilityResponse>> getWeeklySchedule(
            @PathVariable UUID doctorUuid) {
        return ResponseEntity.ok(availabilityService.getWeeklySchedule(doctorUuid));
    }

    /**
     * POST /api/v1/doctors/{doctorUuid}/availability
     * Create a single availability block.
     */
    @PostMapping("/availability")
    @PreAuthorize("hasAuthority('PERM_DOCTOR_UPDATE')")
    public ResponseEntity<AvailabilityResponse> createAvailability(
            @PathVariable UUID doctorUuid,
            @Valid @RequestBody CreateAvailabilityRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(availabilityService.create(doctorUuid, request));
    }

    /**
     * POST /api/v1/doctors/{doctorUuid}/availability/bulk
     * Create multiple availability blocks at once.
     */
    @PostMapping("/availability/bulk")
    @PreAuthorize("hasAuthority('PERM_DOCTOR_UPDATE')")
    public ResponseEntity<List<AvailabilityResponse>> createBulkAvailability(
            @PathVariable UUID doctorUuid,
            @Valid @RequestBody List<CreateAvailabilityRequest> requests) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(availabilityService.createBulk(doctorUuid, requests));
    }

    /**
     * PUT /api/v1/doctors/{doctorUuid}/availability/{uuid}
     * Update an availability block.
     */
    @PutMapping("/availability/{uuid}")
    @PreAuthorize("hasAuthority('PERM_DOCTOR_UPDATE')")
    public ResponseEntity<AvailabilityResponse> updateAvailability(
            @PathVariable UUID doctorUuid,
            @PathVariable UUID uuid,
            @Valid @RequestBody UpdateAvailabilityRequest request) {
        return ResponseEntity.ok(availabilityService.update(doctorUuid, uuid, request));
    }

    /**
     * PATCH /api/v1/doctors/{doctorUuid}/availability/{uuid}/status
     * Deactivate an availability block (soft-delete).
     */
    @PatchMapping("/availability/{uuid}/status")
    @PreAuthorize("hasAuthority('PERM_DOCTOR_UPDATE')")
    public ResponseEntity<AvailabilityResponse> deactivateAvailability(
            @PathVariable UUID doctorUuid,
            @PathVariable UUID uuid) {
        return ResponseEntity.ok(availabilityService.deactivate(doctorUuid, uuid));
    }

    // ═══════════════════════════════════════════════════════════
    // Availability Exception APIs
    // ═══════════════════════════════════════════════════════════

    /**
     * GET /api/v1/doctors/{doctorUuid}/availability/exceptions
     * Get all exceptions for a doctor.
     */
    @GetMapping("/availability/exceptions")
    @PreAuthorize("hasAuthority('PERM_DOCTOR_READ')")
    public ResponseEntity<List<AvailabilityExceptionResponse>> getExceptions(
            @PathVariable UUID doctorUuid,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {

        if (from != null && to != null) {
            return ResponseEntity.ok(exceptionService.getByDateRange(doctorUuid, from, to));
        }
        return ResponseEntity.ok(exceptionService.getAllByDoctor(doctorUuid));
    }

    /**
     * POST /api/v1/doctors/{doctorUuid}/availability/exceptions
     * Create a schedule exception.
     */
    @PostMapping("/availability/exceptions")
    @PreAuthorize("hasAuthority('PERM_DOCTOR_UPDATE')")
    public ResponseEntity<AvailabilityExceptionResponse> createException(
            @PathVariable UUID doctorUuid,
            @Valid @RequestBody CreateExceptionRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(exceptionService.create(doctorUuid, request));
    }

    /**
     * PUT /api/v1/doctors/{doctorUuid}/availability/exceptions/{uuid}
     * Update a schedule exception.
     */
    @PutMapping("/availability/exceptions/{uuid}")
    @PreAuthorize("hasAuthority('PERM_DOCTOR_UPDATE')")
    public ResponseEntity<AvailabilityExceptionResponse> updateException(
            @PathVariable UUID doctorUuid,
            @PathVariable UUID uuid,
            @Valid @RequestBody UpdateExceptionRequest request) {
        return ResponseEntity.ok(exceptionService.update(doctorUuid, uuid, request));
    }

    /**
     * DELETE /api/v1/doctors/{doctorUuid}/availability/exceptions/{uuid}
     * Delete a schedule exception.
     */
    @DeleteMapping("/availability/exceptions/{uuid}")
    @PreAuthorize("hasAuthority('PERM_DOCTOR_UPDATE')")
    public ResponseEntity<Void> deleteException(
            @PathVariable UUID doctorUuid,
            @PathVariable UUID uuid) {
        exceptionService.delete(doctorUuid, uuid);
        return ResponseEntity.noContent().build();
    }

    // ═══════════════════════════════════════════════════════════
    // Available Slots API
    // ═══════════════════════════════════════════════════════════

    /**
     * GET /api/v1/doctors/{doctorUuid}/available-slots?date=2026-08-24
     * Generate available appointment slots for a specific date.
     */
    @GetMapping("/available-slots")
    @PreAuthorize("hasAuthority('PERM_APPOINTMENT_READ')")
    public ResponseEntity<AvailableSlotsResponse> getAvailableSlots(
            @PathVariable UUID doctorUuid,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        return ResponseEntity.ok(slotGenerationService.generateSlots(doctorUuid, date));
    }
}
