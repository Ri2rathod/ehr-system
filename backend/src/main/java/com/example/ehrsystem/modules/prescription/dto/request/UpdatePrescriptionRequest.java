package com.example.ehrsystem.modules.prescription.dto.request;

import jakarta.validation.constraints.Size;
import lombok.*;

/**
 * All fields optional: null means "leave unchanged".
 * Status is never updated through this DTO - only via explicit
 * transition endpoints (activate/complete/cancel/void).
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UpdatePrescriptionRequest {

    @Size(max = 5000, message = "Notes must be at most 5000 characters")
    private String notes;

    private Long version;
}
