package com.example.ehrsystem.modules.treatmentplan.dto.response;

import com.example.ehrsystem.modules.treatmentplan.entity.DurationUnit;
import com.example.ehrsystem.modules.treatmentplan.entity.TreatmentItemStatus;
import com.example.ehrsystem.modules.treatmentplan.entity.TreatmentPriority;
import com.example.ehrsystem.modules.treatmentplan.entity.TreatmentType;
import lombok.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TreatmentPlanItemResponse {

    private Long id;
    private UUID uuid;
    private UUID treatmentPlanUuid;

    private UUID diagnosisUuid;
    private String diagnosisCode;
    private String diagnosisName;

    private TreatmentType treatmentType;
    private String name;
    private String description;
    private String instructions;
    private String frequency;
    private Integer duration;
    private DurationUnit durationUnit;
    private TreatmentPriority priority;
    private TreatmentItemStatus status;
    private LocalDate startDate;
    private LocalDate endDate;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private Long version;
}
