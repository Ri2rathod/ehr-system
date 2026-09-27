package com.example.ehrsystem.modules.treatmentplan.dto.request;

import jakarta.validation.constraints.Size;
import lombok.*;

import java.time.LocalDate;

/**
 * All fields are optional: null means "leave unchanged".
 * Status is never updated through this DTO — only via explicit transition endpoints.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UpdateTreatmentPlanRequest {

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

    /** Optimistic-locking token; when supplied and stale, the update is rejected with 409. */
    private Long version;
}
