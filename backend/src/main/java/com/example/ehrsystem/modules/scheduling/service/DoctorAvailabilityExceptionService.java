package com.example.ehrsystem.modules.scheduling.service;

import com.example.ehrsystem.common.security.SecurityContextAccessor;
import com.example.ehrsystem.modules.doctor.entity.Doctor;
import com.example.ehrsystem.modules.doctor.repository.DoctorRepository;
import com.example.ehrsystem.modules.scheduling.dto.request.CreateExceptionRequest;
import com.example.ehrsystem.modules.scheduling.dto.request.UpdateExceptionRequest;
import com.example.ehrsystem.modules.scheduling.dto.response.AvailabilityExceptionResponse;
import com.example.ehrsystem.modules.scheduling.entity.DoctorAvailabilityException;
import com.example.ehrsystem.modules.scheduling.repository.DoctorAvailabilityExceptionRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class DoctorAvailabilityExceptionService {

    private final DoctorAvailabilityExceptionRepository exceptionRepository;
    private final DoctorRepository doctorRepository;
    private final SecurityContextAccessor securityContext;

    /**
     * Create a schedule exception for a doctor.
     */
    @Transactional
    public AvailabilityExceptionResponse create(UUID doctorUuid, CreateExceptionRequest request) {
        Doctor doctor = findDoctorOrThrow(doctorUuid);
        validateExceptionRequest(request);

        Long currentUserId = securityContext.getCurrentUserId();

        boolean isFullDay = request.getIsFullDay() != null ? request.getIsFullDay() : true;

        DoctorAvailabilityException exception = DoctorAvailabilityException.builder()
                .doctor(doctor)
                .exceptionDate(request.getExceptionDate())
                .exceptionType(request.getExceptionType())
                .startTime(isFullDay ? null : request.getStartTime())
                .endTime(isFullDay ? null : request.getEndTime())
                .isFullDay(isFullDay)
                .reason(request.getReason())
                .createdBy(currentUserId)
                .updatedBy(currentUserId)
                .build();

        DoctorAvailabilityException saved = exceptionRepository.save(exception);
        return toResponse(saved);
    }

    /**
     * Get all exceptions for a doctor.
     */
    public List<AvailabilityExceptionResponse> getAllByDoctor(UUID doctorUuid) {
        Doctor doctor = findDoctorOrThrow(doctorUuid);
        return exceptionRepository.findByDoctorIdOrderByExceptionDateDesc(doctor.getId())
                .stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    /**
     * Get exceptions within a date range (for calendar views).
     */
    public List<AvailabilityExceptionResponse> getByDateRange(UUID doctorUuid, LocalDate from, LocalDate to) {
        Doctor doctor = findDoctorOrThrow(doctorUuid);
        return exceptionRepository.findByDoctorIdAndDateRange(doctor.getId(), from, to)
                .stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    /**
     * Update an exception.
     */
    @Transactional
    public AvailabilityExceptionResponse update(UUID doctorUuid, UUID exceptionUuid, UpdateExceptionRequest request) {
        findDoctorOrThrow(doctorUuid);

        DoctorAvailabilityException exception = exceptionRepository.findByUuid(exceptionUuid)
                .orElseThrow(() -> new EntityNotFoundException(
                        "Exception not found with UUID: " + exceptionUuid));

        if (!exception.getDoctor().getUuid().equals(doctorUuid)) {
            throw new IllegalArgumentException("Exception does not belong to the specified doctor");
        }

        if (request.getExceptionDate() != null) exception.setExceptionDate(request.getExceptionDate());
        if (request.getExceptionType() != null) exception.setExceptionType(request.getExceptionType());
        if (request.getIsFullDay() != null) {
            exception.setIsFullDay(request.getIsFullDay());
            if (request.getIsFullDay()) {
                exception.setStartTime(null);
                exception.setEndTime(null);
            }
        }
        if (request.getStartTime() != null) exception.setStartTime(request.getStartTime());
        if (request.getEndTime() != null) exception.setEndTime(request.getEndTime());
        if (request.getReason() != null) exception.setReason(request.getReason());

        exception.setUpdatedBy(securityContext.getCurrentUserId());

        DoctorAvailabilityException updated = exceptionRepository.save(exception);
        return toResponse(updated);
    }

    /**
     * Delete an exception (hard delete is acceptable for schedule configuration).
     */
    @Transactional
    public void delete(UUID doctorUuid, UUID exceptionUuid) {
        findDoctorOrThrow(doctorUuid);

        DoctorAvailabilityException exception = exceptionRepository.findByUuid(exceptionUuid)
                .orElseThrow(() -> new EntityNotFoundException(
                        "Exception not found with UUID: " + exceptionUuid));

        if (!exception.getDoctor().getUuid().equals(doctorUuid)) {
            throw new IllegalArgumentException("Exception does not belong to the specified doctor");
        }

        exceptionRepository.delete(exception);
    }

    // ── Helpers ──────────────────────────────────────────────

    private Doctor findDoctorOrThrow(UUID doctorUuid) {
        return doctorRepository.findByUuidAndDeletedAtIsNull(doctorUuid)
                .orElseThrow(() -> new EntityNotFoundException(
                        "Doctor not found with UUID: " + doctorUuid));
    }

    private void validateExceptionRequest(CreateExceptionRequest request) {
        boolean isFullDay = request.getIsFullDay() != null ? request.getIsFullDay() : true;
        if (!isFullDay) {
            if (request.getStartTime() == null || request.getEndTime() == null) {
                throw new IllegalArgumentException(
                        "Partial-day exceptions must specify both start_time and end_time");
            }
            if (!request.getStartTime().isBefore(request.getEndTime())) {
                throw new IllegalArgumentException("Start time must be before end time");
            }
        }
    }

    private AvailabilityExceptionResponse toResponse(DoctorAvailabilityException exception) {
        return AvailabilityExceptionResponse.builder()
                .id(exception.getId())
                .uuid(exception.getUuid())
                .doctorUuid(exception.getDoctor().getUuid())
                .exceptionDate(exception.getExceptionDate())
                .exceptionType(exception.getExceptionType())
                .startTime(exception.getStartTime())
                .endTime(exception.getEndTime())
                .isFullDay(exception.getIsFullDay())
                .reason(exception.getReason())
                .createdAt(exception.getCreatedAt())
                .updatedAt(exception.getUpdatedAt())
                .build();
    }
}
