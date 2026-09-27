package com.example.ehrsystem.modules.treatmentplan.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.*;

import java.time.LocalDate;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CreateTreatmentPlanRequest {

    @NotBlank(message = "Title is required")
    @Size(max = 500, message = "Title must be at most 500 characters")
    private String title;

    @Size(max = 5000, message = "Goals must be at most 5000 characters")
    private String goals;

    @Size(max = 5000, message = "Instructions must be at most 5000 characters")
    private String instructions;

    @Size(max = 5000, message = "Follow-up instructions must be at most 5000 characters")
    private String followUpInstructions;

    @Size(max = 5000, message = "Notes must be at most 5000 characters")
    private String notes;

    private LocalDate startDate;

    private LocalDate endDate;
}
