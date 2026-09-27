package com.example.ehrsystem.modules.treatmentplan.dto.request;

import com.example.ehrsystem.modules.treatmentplan.entity.DurationUnit;
import com.example.ehrsystem.modules.treatmentplan.entity.TreatmentPriority;
import com.example.ehrsystem.modules.treatmentplan.entity.TreatmentType;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.*;

import java.time.LocalDate;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CreateTreatmentPlanItemRequest {

    /**
     * Optional link to a diagnosis belonging to the SAME encounter.
     * Cross-encounter or unknown diagnosis references are rejected.
     */
    private UUID diagnosisUuid;

    @NotNull(message = "Treatment type is required")
    private TreatmentType treatmentType;

    @NotBlank(message = "Name is required")
    @Size(max = 500, message = "Name must be at most 500 characters")
    private String name;

    @Size(max = 5000, message = "Description must be at most 5000 characters")
    private String description;

    @Size(max = 5000, message = "Instructions must be at most 5000 characters")
    private String instructions;

    @Size(max = 200, message = "Frequency must be at most 200 characters")
    private String frequency;

    @Min(value = 1, message = "Duration must be at least 1")
    private Integer duration;

    private DurationUnit durationUnit;

    private TreatmentPriority priority;

    private LocalDate startDate;

    private LocalDate endDate;
}
