package com.example.ehrsystem.modules.doctor.service;

import com.example.ehrsystem.common.security.SecurityContextAccessor;
import com.example.ehrsystem.common.util.AuditLogger;
import com.example.ehrsystem.modules.doctor.dto.request.CreateDoctorRequest;
import com.example.ehrsystem.modules.doctor.dto.request.UpdateDoctorRequest;
import com.example.ehrsystem.modules.doctor.dto.response.DoctorResponse;
import com.example.ehrsystem.modules.doctor.entity.Doctor;
import com.example.ehrsystem.modules.doctor.repository.DoctorRepository;
import com.example.ehrsystem.modules.user.service.UserService;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class DoctorService {

    private final DoctorRepository doctorRepository;
    private final DoctorCodeService doctorCodeService;
    private final UserService userService;
    private final AuditLogger auditLogger;
    private final SecurityContextAccessor securityContext;

    @Transactional
    public DoctorResponse create(CreateDoctorRequest request) {
        if (request.getEmail() != null && !request.getEmail().isBlank()
                && doctorRepository.existsByEmailAndDeletedAtIsNull(request.getEmail())) {
            throw new IllegalArgumentException("Email already exists for another doctor");
        }

        if (request.getPhoneNumber() != null && !request.getPhoneNumber().isBlank()
                && doctorRepository.existsByPhoneNumberAndDeletedAtIsNull(request.getPhoneNumber())) {
            throw new IllegalArgumentException("Phone number already exists for another doctor");
        }

        Doctor doctor = Doctor.builder()
                .firstName(request.getFirstName().trim())
                .middleName(request.getMiddleName() != null ? request.getMiddleName().trim() : null)
                .lastName(request.getLastName().trim())
                .displayName(buildDisplayName(
                        request.getFirstName(), request.getMiddleName(), request.getLastName()))
                .gender(request.getGender())
                .dateOfBirth(request.getDateOfBirth())
                .profilePhotoUrl(request.getProfilePhotoUrl())
                .email(request.getEmail() != null ? request.getEmail().trim().toLowerCase() : null)
                .phoneCountryCode(request.getPhoneCountryCode())
                .phoneNumber(request.getPhoneNumber())
                .specialization(request.getSpecialization())
                .qualification(request.getQualification())
                .yearsOfExperience(request.getYearsOfExperience())
                .consultationFee(request.getConsultationFee())
                .licenseNumber(request.getLicenseNumber())
                .licenseExpiryDate(request.getLicenseExpiryDate())
                .biography(request.getBiography())
                .consultationDurationMinutes(request.getConsultationDurationMinutes() != null
                        ? request.getConsultationDurationMinutes() : 15)
                .status(request.getStatus() != null ? request.getStatus() : "ACTIVE")
                .isAvailableForAppointments(request.getIsAvailableForAppointments() != null
                        ? request.getIsAvailableForAppointments() : true)
                .build();

        if (request.getUserId() != null) {
            userService.getById(request.getUserId());
            doctor.setUserId(request.getUserId());
        }

        doctor.setDoctorCode(doctorCodeService.generateDoctorCode());
        doctor.setCreatedBy(securityContext.getCurrentUserId());
        doctor.setUpdatedBy(securityContext.getCurrentUserId());

        Doctor saved = doctorRepository.save(doctor);

        Map<String, Object> auditDetails = new HashMap<>();
        auditDetails.put("doctorId", saved.getId());
        auditDetails.put("doctorUuid", saved.getUuid());
        auditDetails.put("doctorCode", saved.getDoctorCode());
        auditDetails.put("specialization", saved.getSpecialization());
        auditLogger.logCustomEvent("DOCTOR_CREATED", auditDetails);

        return toResponse(saved);
    }

    public DoctorResponse getById(Long id) {
        Doctor doctor = doctorRepository.findByIdAndDeletedAtIsNull(id)
                .orElseThrow(() -> new EntityNotFoundException("Doctor not found with id: " + id));
        return toResponse(doctor);
    }

    public DoctorResponse getByUuid(UUID uuid) {
        Doctor doctor = doctorRepository.findByUuidAndDeletedAtIsNull(uuid)
                .orElseThrow(() -> new EntityNotFoundException("Doctor not found with uuid: " + uuid));
        return toResponse(doctor);
    }

    public DoctorResponse getByDoctorCode(String doctorCode) {
        Doctor doctor = doctorRepository.findByDoctorCodeAndDeletedAtIsNull(doctorCode)
                .orElseThrow(() -> new EntityNotFoundException("Doctor not found with code: " + doctorCode));
        return toResponse(doctor);
    }

    public Page<DoctorResponse> getAll(Pageable pageable) {
        return doctorRepository.findAllActive(pageable).map(this::toResponse);
    }

    public Page<DoctorResponse> search(String query, Pageable pageable) {
        return doctorRepository.searchDoctors(query, pageable).map(this::toResponse);
    }

    public Page<DoctorResponse> getByStatus(String status, Pageable pageable) {
        return doctorRepository.findByStatus(status, pageable).map(this::toResponse);
    }

    public Page<DoctorResponse> getBySpecialization(String specialization, Pageable pageable) {
        return doctorRepository.findBySpecializationAndActive(specialization, pageable).map(this::toResponse);
    }

    public Page<DoctorResponse> getAvailable(Pageable pageable) {
        return doctorRepository.findAllAvailable(pageable).map(this::toResponse);
    }

    @Transactional
    public DoctorResponse update(UUID uuid, UpdateDoctorRequest request) {
        Doctor doctor = doctorRepository.findByUuidAndDeletedAtIsNull(uuid)
                .orElseThrow(() -> new EntityNotFoundException("Doctor not found with uuid: " + uuid));

        if (request.getFirstName() != null) doctor.setFirstName(request.getFirstName().trim());
        if (request.getMiddleName() != null) doctor.setMiddleName(request.getMiddleName().trim());
        if (request.getLastName() != null) doctor.setLastName(request.getLastName().trim());
        if (request.getFirstName() != null || request.getMiddleName() != null || request.getLastName() != null) {
            doctor.setDisplayName(buildDisplayName(
                    request.getFirstName() != null ? request.getFirstName() : doctor.getFirstName(),
                    request.getMiddleName() != null ? request.getMiddleName() : doctor.getMiddleName(),
                    request.getLastName() != null ? request.getLastName() : doctor.getLastName()));
        }
        if (request.getGender() != null) doctor.setGender(request.getGender());
        if (request.getDateOfBirth() != null) doctor.setDateOfBirth(request.getDateOfBirth());
        if (request.getProfilePhotoUrl() != null) doctor.setProfilePhotoUrl(request.getProfilePhotoUrl());
        if (request.getEmail() != null) doctor.setEmail(request.getEmail().trim().toLowerCase());
        if (request.getPhoneCountryCode() != null) doctor.setPhoneCountryCode(request.getPhoneCountryCode());
        if (request.getPhoneNumber() != null) doctor.setPhoneNumber(request.getPhoneNumber());
        if (request.getSpecialization() != null) doctor.setSpecialization(request.getSpecialization());
        if (request.getQualification() != null) doctor.setQualification(request.getQualification());
        if (request.getYearsOfExperience() != null) doctor.setYearsOfExperience(request.getYearsOfExperience());
        if (request.getConsultationFee() != null) doctor.setConsultationFee(request.getConsultationFee());
        if (request.getLicenseNumber() != null) doctor.setLicenseNumber(request.getLicenseNumber());
        if (request.getLicenseExpiryDate() != null) doctor.setLicenseExpiryDate(request.getLicenseExpiryDate());
        if (request.getBiography() != null) doctor.setBiography(request.getBiography());
        if (request.getConsultationDurationMinutes() != null) {
            doctor.setConsultationDurationMinutes(request.getConsultationDurationMinutes());
        }
        if (request.getStatus() != null) doctor.setStatus(request.getStatus());
        if (request.getIsAvailableForAppointments() != null) {
            doctor.setIsAvailableForAppointments(request.getIsAvailableForAppointments());
        }

        doctor.setUpdatedBy(securityContext.getCurrentUserId());
        Doctor updated = doctorRepository.save(doctor);
        return toResponse(updated);
    }

    @Transactional
    public void delete(UUID uuid) {
        Doctor doctor = doctorRepository.findByUuidAndDeletedAtIsNull(uuid)
                .orElseThrow(() -> new EntityNotFoundException("Doctor not found with uuid: " + uuid));
        doctor.setDeletedAt(java.time.LocalDateTime.now());
        doctor.setUpdatedBy(securityContext.getCurrentUserId());
        doctorRepository.save(doctor);

        Map<String, Object> auditDetails = new HashMap<>();
        auditDetails.put("doctorId", doctor.getId());
        auditDetails.put("doctorUuid", doctor.getUuid());
        auditDetails.put("doctorCode", doctor.getDoctorCode());
        auditLogger.logCustomEvent("DOCTOR_DELETED", auditDetails);
    }

    private String buildDisplayName(String firstName, String middleName, String lastName) {
        StringBuilder sb = new StringBuilder();
        if (firstName != null && !firstName.isBlank()) sb.append(firstName.trim());
        if (middleName != null && !middleName.isBlank()) {
            if (!sb.isEmpty()) sb.append(" ");
            sb.append(middleName.trim());
        }
        if (lastName != null && !lastName.isBlank()) {
            if (!sb.isEmpty()) sb.append(" ");
            sb.append(lastName.trim());
        }
        return sb.toString();
    }

    private DoctorResponse toResponse(Doctor doctor) {
        return DoctorResponse.builder()
                .id(doctor.getId())
                .uuid(doctor.getUuid())
                .userId(doctor.getUserId())
                .doctorCode(doctor.getDoctorCode())
                .firstName(doctor.getFirstName())
                .middleName(doctor.getMiddleName())
                .lastName(doctor.getLastName())
                .displayName(doctor.getDisplayName())
                .fullName(doctor.getFullName())
                .gender(doctor.getGender())
                .dateOfBirth(doctor.getDateOfBirth())
                .profilePhotoUrl(doctor.getProfilePhotoUrl())
                .email(doctor.getEmail())
                .phoneCountryCode(doctor.getPhoneCountryCode())
                .phoneNumber(doctor.getPhoneNumber())
                .specialization(doctor.getSpecialization())
                .qualification(doctor.getQualification())
                .yearsOfExperience(doctor.getYearsOfExperience())
                .consultationFee(doctor.getConsultationFee())
                .licenseNumber(doctor.getLicenseNumber())
                .licenseExpiryDate(doctor.getLicenseExpiryDate())
                .biography(doctor.getBiography())
                .consultationDurationMinutes(doctor.getConsultationDurationMinutes())
                .status(doctor.getStatus())
                .isAvailableForAppointments(doctor.getIsAvailableForAppointments())
                .createdAt(doctor.getCreatedAt())
                .updatedAt(doctor.getUpdatedAt())
                .build();
    }
}