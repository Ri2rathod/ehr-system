package com.example.ehrsystem.modules.doctor.dto.request;

import jakarta.validation.constraints.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UpdateDoctorRequest {

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

    @Size(max = 500, message = "Profile photo URL must be less than 500 characters")
    private String profilePhotoUrl;

    @Email(message = "Invalid email format")
    @Size(max = 255, message = "Email must be less than 255 characters")
    private String email;

    @Size(max = 10, message = "Phone country code must be less than 10 characters")
    private String phoneCountryCode;

    @Pattern(regexp = "^[0-9+\\-\\s()]{7,20}$", message = "Invalid phone number format")
    private String phoneNumber;

    @Size(max = 100, message = "Specialization must be less than 100 characters")
    private String specialization;

    @Size(max = 255, message = "Qualification must be less than 255 characters")
    private String qualification;

    @Min(value = 0, message = "Years of experience cannot be negative")
    @Max(value = 80, message = "Years of experience cannot exceed 80")
    private Integer yearsOfExperience;

    @DecimalMin(value = "0.00", message = "Consultation fee cannot be negative")
    @Digits(integer = 8, fraction = 2, message = "Consultation fee format is invalid")
    private BigDecimal consultationFee;

    @Size(max = 100, message = "License number must be less than 100 characters")
    private String licenseNumber;

    @Future(message = "License expiry date must be in the future")
    private LocalDate licenseExpiryDate;

    @Size(max = 5000, message = "Biography must be less than 5000 characters")
    private String biography;

    @Min(value = 5, message = "Consultation duration must be at least 5 minutes")
    @Max(value = 480, message = "Consultation duration cannot exceed 480 minutes")
    private Integer consultationDurationMinutes;

    @Pattern(regexp = "^(ACTIVE|INACTIVE|SUSPENDED|ON_LEAVE|RETIRED)$",
             message = "Status must be one of: ACTIVE, INACTIVE, SUSPENDED, ON_LEAVE, RETIRED")
    private String status;

    private Boolean isAvailableForAppointments;
}