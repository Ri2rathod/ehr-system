package com.example.ehrsystem.modules.prescription.dto.request;

import jakarta.validation.constraints.Size;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CreatePrescriptionRequest {

    @Size(max = 5000, message = "Notes must be at most 5000 characters")
    private String notes;
}
