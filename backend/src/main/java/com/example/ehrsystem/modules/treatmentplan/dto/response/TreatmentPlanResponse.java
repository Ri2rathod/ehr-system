package com.example.ehrsystem.modules.treatmentplan.dto.response;

import com.example.ehrsystem.modules.treatmentplan.entity.TreatmentPlanStatus;
import lombok.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TreatmentPlanResponse {

    private Long id;
    private UUID uuid;
    private UUID encounterUuid;
    private String title;
    private TreatmentPlanStatus status;
    private String goals;
    private String instructions;
    private String followUpInstructions;
    private String notes;
    private LocalDate startDate;
    private LocalDate endDate;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private Long version;
}
