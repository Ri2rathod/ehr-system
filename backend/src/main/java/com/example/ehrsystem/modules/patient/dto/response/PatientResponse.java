package com.example.ehrsystem.modules.patient.dto.response;

import lombok.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PatientResponse {

    private Long id;
    private UUID uuid;
    private Long userId;
    private String mrn;
    private String firstName;
    private String middleName;
    private String lastName;
    private String displayName;
    private String fullName;
    private String gender;
    private LocalDate dateOfBirth;
    private String bloodGroup;
    private String maritalStatus;
    private String profilePhotoUrl;
    private String email;
    private String phoneCountryCode;
    private String phoneNumber;
    private String emergencyContactName;
    private String emergencyContactRelationship;
    private String emergencyContactPhone;
    private LocalDateTime registeredAt;
    private String status;
    private Boolean isDeceased;
    private LocalDateTime deceasedAt;
    private String addressLine1;
    private String addressLine2;
    private String city;
    private String state;
    private String postalCode;
    private String country;
    private String insuranceProvider;
    private String insurancePolicyNumber;
    private String allergies;
    private String chronicConditions;
    private String notes;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}