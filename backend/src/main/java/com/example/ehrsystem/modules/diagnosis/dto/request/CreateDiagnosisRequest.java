package com.example.ehrsystem.modules.diagnosis.dto.request;

import com.example.ehrsystem.modules.diagnosis.entity.DiagnosisStatus;
import com.example.ehrsystem.modules.diagnosis.entity.DiagnosisType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.*;

import java.time.LocalDate;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CreateDiagnosisRequest {

    @Size(max = 32, message = "Code must be at most 32 characters")
    private String code;

    @Size(max = 50, message = "Code system must be at most 50 characters")
    private String codeSystem;

    @NotBlank(message = "Diagnosis name is required")
    @Size(max = 500, message = "Diagnosis name must be at most 500 characters")
    private String name;

    private DiagnosisType diagnosisType;

    private DiagnosisStatus clinicalStatus;

    private LocalDate onsetDate;

    @Size(max = 5000, message = "Notes must be at most 5000 characters")
    private String notes;

    /**
     * Controlled replacement of an existing primary diagnosis.
     * When false (default), creating a PRIMARY diagnosis while one already
     * exists fails with a conflict.
     */
    @Builder.Default
    private Boolean replacePrimary = Boolean.FALSE;
}
