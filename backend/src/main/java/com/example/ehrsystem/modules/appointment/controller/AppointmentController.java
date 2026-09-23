package com.example.ehrsystem.modules.appointment.controller;

import com.example.ehrsystem.common.response.PagedResponse;
import com.example.ehrsystem.modules.appointment.dto.request.CreateAppointmentRequest;
import com.example.ehrsystem.modules.appointment.dto.request.RescheduleAppointmentRequest;
import com.example.ehrsystem.modules.appointment.dto.request.UpdateAppointmentStatusRequest;
import com.example.ehrsystem.modules.appointment.dto.response.AppointmentHistoryResponse;
import com.example.ehrsystem.modules.appointment.dto.response.AppointmentResponse;
import com.example.ehrsystem.modules.appointment.entity.AppointmentStatus;
import com.example.ehrsystem.modules.appointment.entity.VisitType;
import com.example.ehrsystem.modules.appointment.service.AppointmentService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/appointments")
@RequiredArgsConstructor
public class AppointmentController {

    private static final int MAX_PAGE_SIZE = 100;
    private static final int DEFAULT_PAGE_SIZE = 20;

    private final AppointmentService appointmentService;

    private Pageable createPageable(int page, int size, String sortBy, String sortDir) {
        int actualSize = Math.min(size > 0 ? size : DEFAULT_PAGE_SIZE, MAX_PAGE_SIZE);
        int actualPage = Math.max(page, 0);
        Sort sort = sortDir.equalsIgnoreCase("asc")
                ? Sort.by(sortBy).ascending()
                : Sort.by(sortBy).descending();
        return PageRequest.of(actualPage, actualSize, sort);
    }

    @PostMapping
    @PreAuthorize("hasAuthority('PERM_APPOINTMENT_CREATE')")
    public ResponseEntity<AppointmentResponse> create(@Valid @RequestBody CreateAppointmentRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(appointmentService.create(request));
    }

    @GetMapping("/{uuid}")
    @PreAuthorize("hasAuthority('PERM_APPOINTMENT_READ')")
    public ResponseEntity<AppointmentResponse> getByUuid(@PathVariable UUID uuid) {
        return ResponseEntity.ok(appointmentService.getByUuid(uuid));
    }

    @GetMapping("/number/{appointmentNumber}")
    @PreAuthorize("hasAuthority('PERM_APPOINTMENT_READ')")
    public ResponseEntity<AppointmentResponse> getByAppointmentNumber(@PathVariable String appointmentNumber) {
        return ResponseEntity.ok(appointmentService.getByAppointmentNumber(appointmentNumber));
    }

    @GetMapping
    @PreAuthorize("hasAnyAuthority('PERM_APPOINTMENT_READ', 'PERM_APPOINTMENT_VIEW_ALL')")
    public ResponseEntity<PagedResponse<AppointmentResponse>> search(
            @RequestParam(required = false) UUID doctorUuid,
            @RequestParam(required = false) UUID patientUuid,
            @RequestParam(required = false) AppointmentStatus status,
            @RequestParam(required = false) VisitType visitType,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dateFrom,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dateTo,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "startTime") String sortBy,
            @RequestParam(defaultValue = "asc") String sortDir) {
        Pageable pageable = createPageable(page, size, sortBy, sortDir);
        Page<AppointmentResponse> result = appointmentService.search(
                doctorUuid, patientUuid, status, visitType, dateFrom, dateTo, pageable);
        return ResponseEntity.ok(PagedResponse.of(
                result.getContent(), result.getNumber(), result.getSize(), result.getTotalElements()));
    }

    @GetMapping("/today")
    @PreAuthorize("hasAnyAuthority('PERM_APPOINTMENT_READ', 'PERM_APPOINTMENT_VIEW_ALL')")
    public ResponseEntity<List<AppointmentResponse>> getTodayAppointments() {
        return ResponseEntity.ok(appointmentService.getByDate(LocalDate.now()));
    }

    @GetMapping("/doctor/{doctorUuid}")
    @PreAuthorize("hasAnyAuthority('PERM_APPOINTMENT_READ', 'PERM_APPOINTMENT_VIEW_ALL')")
    public ResponseEntity<List<AppointmentResponse>> getByDoctor(
            @PathVariable UUID doctorUuid,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
        return ResponseEntity.ok(appointmentService.getByDoctor(doctorUuid, from, to));
    }

    @GetMapping("/patient/{patientUuid}")
    @PreAuthorize("hasAnyAuthority('PERM_APPOINTMENT_READ', 'PERM_APPOINTMENT_VIEW_OWN')")
    public ResponseEntity<List<AppointmentResponse>> getByPatient(
            @PathVariable UUID patientUuid,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
        return ResponseEntity.ok(appointmentService.getByPatient(patientUuid, from, to));
    }

    @GetMapping("/{uuid}/history")
    @PreAuthorize("hasAuthority('PERM_APPOINTMENT_READ')")
    public ResponseEntity<List<AppointmentHistoryResponse>> getHistory(@PathVariable UUID uuid) {
        return ResponseEntity.ok(appointmentService.getHistory(uuid));
    }

    @PutMapping("/{uuid}/status")
    @PreAuthorize("hasAnyAuthority('PERM_APPOINTMENT_CHECK_IN', 'PERM_APPOINTMENT_STATUS_UPDATE', 'PERM_APPOINTMENT_CANCEL')")
    public ResponseEntity<AppointmentResponse> updateStatus(
            @PathVariable UUID uuid,
            @Valid @RequestBody UpdateAppointmentStatusRequest request) {
        return ResponseEntity.ok(appointmentService.updateStatus(uuid, request.getStatus(), request.getCancellationReason()));
    }

    @PutMapping("/{uuid}/reschedule")
    @PreAuthorize("hasAuthority('PERM_APPOINTMENT_RESCHEDULE')")
    public ResponseEntity<AppointmentResponse> reschedule(
            @PathVariable UUID uuid,
            @Valid @RequestBody RescheduleAppointmentRequest request) {
        return ResponseEntity.ok(appointmentService.reschedule(uuid, request.getStartTime(), request.getEndTime(), request.getReason()));
    }
}
