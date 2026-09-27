package com.example.ehrsystem.modules.treatmentplan.dto.request;

import com.example.ehrsystem.modules.treatmentplan.entity.DurationUnit;
import com.example.ehrsystem.modules.treatmentplan.entity.TreatmentPriority;
import com.example.ehrsystem.modules.treatmentplan.entity.TreatmentType;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;
import lombok.*;

import java.time.LocalDate;
import java.util.UUID;

/**
 * All fields are optional: null means "leave unchanged".
 * Status is never updated through this DTO — only via explicit transition endpoints.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UpdateTreatmentPlanItemRequest {

    private UUID diagnosisUuid;

    /**
     * Explicit unlink of the diagnosis reference.
     * When true the item's diagnosis is cleared; diagnosisUuid is then ignored.
     */
    private Boolean unlinkDiagnosis;

    private TreatmentType treatmentType;

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

    /** Optimistic-locking token; when supplied and stale, the update is rejected with 409. */
    private Long version;
}
