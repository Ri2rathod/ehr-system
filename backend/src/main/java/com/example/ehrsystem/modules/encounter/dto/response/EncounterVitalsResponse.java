package com.example.ehrsystem.modules.encounter.dto.response;

import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class EncounterVitalsResponse {

    private Long id;
    private UUID uuid;
    private UUID encounterUuid;

    private BigDecimal temperature;
    private Integer heartRate;
    private Integer respiratoryRate;
    private Integer systolicBp;
    private Integer diastolicBp;
    private BigDecimal oxygenSaturation;
    private BigDecimal weight;
    private BigDecimal height;
    private BigDecimal bmi;

    private LocalDateTime recordedAt;
    private Long recordedBy;

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private Long version;
}
