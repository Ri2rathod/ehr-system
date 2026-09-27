package com.example.ehrsystem.modules.specimen.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RejectSpecimenRequest {

    @NotBlank(message = "Rejection reason is required")
    @Size(max = 500, message = "Rejection reason must be at most 500 characters")
    private String rejectionReason;

    @Size(max = 2000, message = "Notes must be at most 2000 characters")
    private String notes;
}
