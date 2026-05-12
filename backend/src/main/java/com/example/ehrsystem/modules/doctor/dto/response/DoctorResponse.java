package com.example.ehrsystem.modules.doctor.dto.response;

import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DoctorResponse {

    private Long id;
    private UUID uuid;
    private Long userId;
    private String doctorCode;
    private String firstName;
    private String middleName;
    private String lastName;
    private String displayName;
    private String fullName;
    private String gender;
    private LocalDate dateOfBirth;
    private String profilePhotoUrl;
    private String email;
    private String phoneCountryCode;
    private String phoneNumber;
    private String specialization;
    private String qualification;
    private Integer yearsOfExperience;
    private BigDecimal consultationFee;
    private String licenseNumber;
    private LocalDate licenseExpiryDate;
    private String biography;
    private Integer consultationDurationMinutes;
    private String status;
    private Boolean isAvailableForAppointments;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}