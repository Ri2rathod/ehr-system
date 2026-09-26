package com.example.ehrsystem.modules.diagnosis.dto.response;

import com.example.ehrsystem.modules.diagnosis.entity.DiagnosisStatus;
import com.example.ehrsystem.modules.diagnosis.entity.DiagnosisType;
import lombok.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DiagnosisResponse {

    private Long id;
    private UUID uuid;
    private UUID encounterUuid;

    private String code;
    private String codeSystem;
    private String name;

    private DiagnosisType diagnosisType;
    private DiagnosisStatus clinicalStatus;

    private LocalDate onsetDate;
    private LocalDate resolvedDate;

    private String notes;

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private Long version;
}
