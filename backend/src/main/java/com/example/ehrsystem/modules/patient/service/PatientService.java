package com.example.ehrsystem.modules.patient.service;

import com.example.ehrsystem.common.security.SecurityContextAccessor;
import com.example.ehrsystem.common.util.AuditLogger;
import com.example.ehrsystem.modules.common.service.MrnService;
import com.example.ehrsystem.modules.patient.dto.request.CreatePatientRequest;
import com.example.ehrsystem.modules.patient.dto.request.UpdatePatientRequest;
import com.example.ehrsystem.modules.patient.dto.response.PatientResponse;
import com.example.ehrsystem.modules.patient.entity.Patient;
import com.example.ehrsystem.modules.patient.repository.PatientRepository;
import com.example.ehrsystem.modules.user.entity.User;
import com.example.ehrsystem.modules.user.service.UserService;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class PatientService {

    private final PatientRepository patientRepository;
    private final UserService userService;
    private final MrnService mrnService;
    private final AuditLogger auditLogger;
    private final SecurityContextAccessor securityContext;

    @Transactional
    public PatientResponse create(CreatePatientRequest request) {
        if (request.getEmail() != null && !request.getEmail().isBlank()
                && patientRepository.existsByEmailAndDeletedAtIsNull(request.getEmail())) {
            throw new IllegalArgumentException("Email already exists for another patient");
        }

        if (request.getPhoneNumber() != null && !request.getPhoneNumber().isBlank()
                && patientRepository.existsByPhoneNumberAndDeletedAtIsNull(request.getPhoneNumber())) {
            throw new IllegalArgumentException("Phone number already exists for another patient");
        }

        Patient patient = Patient.builder()
                .firstName(request.getFirstName().trim())
                .middleName(request.getMiddleName() != null ? request.getMiddleName().trim() : null)
                .lastName(request.getLastName().trim())
                .displayName(buildDisplayName(request.getFirstName(), request.getMiddleName(), request.getLastName()))
                .gender(request.getGender())
                .dateOfBirth(request.getDateOfBirth())
                .bloodGroup(request.getBloodGroup())
                .maritalStatus(request.getMaritalStatus())
                .profilePhotoUrl(request.getProfilePhotoUrl())
                .email(request.getEmail() != null ? request.getEmail().trim().toLowerCase() : null)
                .phoneCountryCode(request.getPhoneCountryCode())
                .phoneNumber(request.getPhoneNumber())
                .emergencyContactName(request.getEmergencyContactName())
                .emergencyContactRelationship(request.getEmergencyContactRelationship())
                .emergencyContactPhone(request.getEmergencyContactPhone())
                .addressLine1(request.getAddressLine1())
                .addressLine2(request.getAddressLine2())
                .city(request.getCity())
                .state(request.getState())
                .postalCode(request.getPostalCode())
                .country(request.getCountry())
                .insuranceProvider(request.getInsuranceProvider())
                .insurancePolicyNumber(request.getInsurancePolicyNumber())
                .allergies(request.getAllergies())
                .chronicConditions(request.getChronicConditions())
                .notes(request.getNotes())
                .status(request.getStatus() != null ? request.getStatus() : "ACTIVE")
                .build();

        if (request.getUserId() != null) {
            User user = userService.getById(request.getUserId());
            patient.setUser(user);
        }

        User currentUser = securityContext.getCurrentUser();
        patient.setCreatedBy(currentUser);

        patient.setMrn(mrnService.generateMrn());

        Patient saved = patientRepository.save(patient);

        Map<String, Object> auditDetails = new HashMap<>();
        auditDetails.put("patientId", saved.getId());
        auditDetails.put("patientUuid", saved.getUuid());
        auditDetails.put("mrn", saved.getMrn());
        auditDetails.put("firstName", saved.getFirstName());
        auditDetails.put("lastName", saved.getLastName());
        auditLogger.logCustomEvent("PATIENT_CREATED", auditDetails);

        return toResponse(saved);
    }

    public PatientResponse getById(Long id) {
        Patient patient = patientRepository.findByIdAndDeletedAtIsNull(id)
                .orElseThrow(() -> new EntityNotFoundException("Patient not found with id: " + id));
        logPatientViewed(patient, "BY_ID");
        return toResponse(patient);
    }

    public PatientResponse getByUuid(UUID uuid) {
        Patient patient = patientRepository.findByUuidAndDeletedAtIsNull(uuid)
                .orElseThrow(() -> new EntityNotFoundException("Patient not found with uuid: " + uuid));
        logPatientViewed(patient, "BY_UUID");
        return toResponse(patient);
    }

    public PatientResponse getByUserId(Long userId) {
        Patient patient = patientRepository.findByUserIdAndDeletedAtIsNull(userId)
                .orElseThrow(() -> new EntityNotFoundException("Patient not found for user id: " + userId));
        logPatientViewed(patient, "BY_USER_ID");
        return toResponse(patient);
    }

    public PatientResponse getByMrn(String mrn) {
        Patient patient = patientRepository.findByMrnAndDeletedAtIsNull(mrn)
                .orElseThrow(() -> new EntityNotFoundException("Patient not found with MRN: " + mrn));
        logPatientViewed(patient, "BY_MRN");
        return toResponse(patient);
    }

    public Page<PatientResponse> search(String query, Pageable pageable) {
        return patientRepository.searchPatients(query, pageable)
                .map(this::toResponse);
    }

    public Page<PatientResponse> getAll(Pageable pageable) {
        return patientRepository.findAllActive(pageable)
                .map(this::toResponse);
    }

    public Page<PatientResponse> getByStatus(String status, Pageable pageable) {
        return patientRepository.findByStatus(status, pageable)
                .map(this::toResponse);
    }

    public List<PatientResponse> findDuplicates(String firstName, String lastName, LocalDate dateOfBirth, String phoneNumber) {
        return patientRepository.findDuplicates(firstName, lastName, dateOfBirth, phoneNumber)
                .stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    @Transactional
    public PatientResponse update(Long id, UpdatePatientRequest request) {
        Patient patient = patientRepository.findByIdAndDeletedAtIsNull(id)
                .orElseThrow(() -> new EntityNotFoundException("Patient not found with id: " + id));

        if (request.getFirstName() != null) patient.setFirstName(request.getFirstName().trim());
        if (request.getMiddleName() != null) patient.setMiddleName(request.getMiddleName().trim());
        if (request.getLastName() != null) patient.setLastName(request.getLastName().trim());
        if (request.getFirstName() != null || request.getMiddleName() != null || request.getLastName() != null) {
            patient.setDisplayName(buildDisplayName(
                    request.getFirstName() != null ? request.getFirstName() : patient.getFirstName(),
                    request.getMiddleName() != null ? request.getMiddleName() : patient.getMiddleName(),
                    request.getLastName() != null ? request.getLastName() : patient.getLastName()
            ));
        }

        if (request.getGender() != null) patient.setGender(request.getGender());
        if (request.getDateOfBirth() != null) patient.setDateOfBirth(request.getDateOfBirth());
        if (request.getBloodGroup() != null) patient.setBloodGroup(request.getBloodGroup());
        if (request.getMaritalStatus() != null) patient.setMaritalStatus(request.getMaritalStatus());
        if (request.getProfilePhotoUrl() != null) patient.setProfilePhotoUrl(request.getProfilePhotoUrl());
        if (request.getEmail() != null) patient.setEmail(request.getEmail().trim().toLowerCase());
        if (request.getPhoneCountryCode() != null) patient.setPhoneCountryCode(request.getPhoneCountryCode());
        if (request.getPhoneNumber() != null) patient.setPhoneNumber(request.getPhoneNumber());
        if (request.getEmergencyContactName() != null) patient.setEmergencyContactName(request.getEmergencyContactName());
        if (request.getEmergencyContactRelationship() != null) patient.setEmergencyContactRelationship(request.getEmergencyContactRelationship());
        if (request.getEmergencyContactPhone() != null) patient.setEmergencyContactPhone(request.getEmergencyContactPhone());
        if (request.getAddressLine1() != null) patient.setAddressLine1(request.getAddressLine1());
        if (request.getAddressLine2() != null) patient.setAddressLine2(request.getAddressLine2());
        if (request.getCity() != null) patient.setCity(request.getCity());
        if (request.getState() != null) patient.setState(request.getState());
        if (request.getPostalCode() != null) patient.setPostalCode(request.getPostalCode());
        if (request.getCountry() != null) patient.setCountry(request.getCountry());
        if (request.getInsuranceProvider() != null) patient.setInsuranceProvider(request.getInsuranceProvider());
        if (request.getInsurancePolicyNumber() != null) patient.setInsurancePolicyNumber(request.getInsurancePolicyNumber());
        if (request.getAllergies() != null) patient.setAllergies(request.getAllergies());
        if (request.getChronicConditions() != null) patient.setChronicConditions(request.getChronicConditions());
        if (request.getNotes() != null) patient.setNotes(request.getNotes());
        if (request.getStatus() != null) patient.setStatus(request.getStatus());

        User currentUser = securityContext.getCurrentUser();
        patient.setUpdatedBy(currentUser);

        Patient updated = patientRepository.save(patient);
        return toResponse(updated);
    }

    @Transactional
    public void delete(Long id) {
        Patient patient = patientRepository.findByIdAndDeletedAtIsNull(id)
                .orElseThrow(() -> new EntityNotFoundException("Patient not found with id: " + id));

        User currentUser = securityContext.getCurrentUser();
        patient.setUpdatedBy(currentUser);
        patient.setDeletedAt(LocalDateTime.now());
        patientRepository.save(patient);

        Map<String, Object> auditDetails = new HashMap<>();
        auditDetails.put("patientId", id);
        auditDetails.put("deletedBy", currentUser != null ? currentUser.getId() : "SYSTEM");
        auditLogger.logCustomEvent("PATIENT_DELETED", auditDetails);
    }

    private void logPatientViewed(Patient patient, String viewType) {
        auditLogger.logPatientRecordViewed(
                "SYSTEM",
                patient.getId(),
                viewType
        );
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

    private PatientResponse toResponse(Patient patient) {
        return PatientResponse.builder()
                .id(patient.getId())
                .uuid(patient.getUuid())
                .userId(patient.getUser() != null ? patient.getUser().getId() : null)
                .mrn(patient.getMrn())
                .firstName(patient.getFirstName())
                .middleName(patient.getMiddleName())
                .lastName(patient.getLastName())
                .displayName(patient.getDisplayName())
                .fullName(patient.getFullName())
                .gender(patient.getGender())
                .dateOfBirth(patient.getDateOfBirth())
                .bloodGroup(patient.getBloodGroup())
                .maritalStatus(patient.getMaritalStatus())
                .profilePhotoUrl(patient.getProfilePhotoUrl())
                .email(patient.getEmail())
                .phoneCountryCode(patient.getPhoneCountryCode())
                .phoneNumber(patient.getPhoneNumber())
                .emergencyContactName(patient.getEmergencyContactName())
                .emergencyContactRelationship(patient.getEmergencyContactRelationship())
                .emergencyContactPhone(patient.getEmergencyContactPhone())
                .registeredAt(patient.getRegisteredAt())
                .status(patient.getStatus())
                .isDeceased(patient.getIsDeceased())
                .deceasedAt(patient.getDeceasedAt())
                .addressLine1(patient.getAddressLine1())
                .addressLine2(patient.getAddressLine2())
                .city(patient.getCity())
                .state(patient.getState())
                .postalCode(patient.getPostalCode())
                .country(patient.getCountry())
                .insuranceProvider(patient.getInsuranceProvider())
                .insurancePolicyNumber(patient.getInsurancePolicyNumber())
                .allergies(patient.getAllergies())
                .chronicConditions(patient.getChronicConditions())
                .notes(patient.getNotes())
                .createdAt(patient.getCreatedAt())
                .updatedAt(patient.getUpdatedAt())
                .build();
    }
}