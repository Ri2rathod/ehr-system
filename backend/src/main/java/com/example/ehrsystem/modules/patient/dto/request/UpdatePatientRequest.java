package com.example.ehrsystem.modules.patient.dto.request;

import jakarta.validation.constraints.*;
import lombok.*;

import java.time.LocalDate;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UpdatePatientRequest {

    @NotBlank(message = "First name is required")
    @Size(min = 1, max = 100, message = "First name must be between 1 and 100 characters")
    private String firstName;

    @Size(max = 100, message = "Middle name must be less than 100 characters")
    private String middleName;

    @NotBlank(message = "Last name is required")
    @Size(min = 1, max = 100, message = "Last name must be between 1 and 100 characters")
    private String lastName;

    @Size(max = 255, message = "Display name must be less than 255 characters")
    private String displayName;

    @Pattern(regexp = "^(MALE|FEMALE|OTHER|TRANSGENDER|UNKNOWN)$",
             message = "Gender must be one of: MALE, FEMALE, OTHER, TRANSGENDER, UNKNOWN")
    private String gender;

    @Past(message = "Date of birth must be in the past")
    private LocalDate dateOfBirth;

    @Pattern(regexp = "^(A_POSITIVE|A_NEGATIVE|B_POSITIVE|B_NEGATIVE|AB_POSITIVE|AB_NEGATIVE|O_POSITIVE|O_NEGATIVE|UNKNOWN)$",
             message = "Invalid blood group")
    private String bloodGroup;

    @Pattern(regexp = "^(SINGLE|MARRIED|DIVORCED|WIDOWED|SEPARATED|UNKNOWN)$",
             message = "Invalid marital status")
    private String maritalStatus;

    @Size(max = 500, message = "Profile photo URL must be less than 500 characters")
    private String profilePhotoUrl;

    @Email(message = "Invalid email format")
    @Size(max = 255, message = "Email must be less than 255 characters")
    private String email;

    @Size(max = 10, message = "Phone country code must be less than 10 characters")
    private String phoneCountryCode;

    @Pattern(regexp = "^[0-9+\\-\\s()]{7,20}$", message = "Invalid phone number format")
    private String phoneNumber;

    @Size(max = 255, message = "Emergency contact name must be less than 255 characters")
    private String emergencyContactName;

    @Size(max = 100, message = "Relationship must be less than 100 characters")
    private String emergencyContactRelationship;

    @Pattern(regexp = "^[0-9+\\-\\s()]{7,20}$", message = "Invalid emergency contact phone format")
    private String emergencyContactPhone;

    @Size(max = 255, message = "Address line 1 must be less than 255 characters")
    private String addressLine1;

    @Size(max = 255, message = "Address line 2 must be less than 255 characters")
    private String addressLine2;

    @Size(max = 100, message = "City must be less than 100 characters")
    private String city;

    @Size(max = 100, message = "State must be less than 100 characters")
    private String state;

    @Size(max = 20, message = "Postal code must be less than 20 characters")
    private String postalCode;

    @Size(max = 100, message = "Country must be less than 100 characters")
    private String country;

    @Size(max = 255, message = "Insurance provider must be less than 255 characters")
    private String insuranceProvider;

    @Size(max = 100, message = "Insurance policy number must be less than 100 characters")
    private String insurancePolicyNumber;

    @Size(max = 5000, message = "Allergies field must be less than 5000 characters")
    private String allergies;

    @Size(max = 5000, message = "Chronic conditions field must be less than 5000 characters")
    private String chronicConditions;

    @Size(max = 5000, message = "Notes must be less than 5000 characters")
    private String notes;

    @Pattern(regexp = "^(ACTIVE|INACTIVE|ARCHIVED|BLOCKED)$",
             message = "Status must be one of: ACTIVE, INACTIVE, ARCHIVED, BLOCKED")
    private String status;
}