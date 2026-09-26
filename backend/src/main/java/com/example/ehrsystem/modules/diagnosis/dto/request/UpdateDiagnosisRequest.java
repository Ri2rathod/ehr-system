package com.example.ehrsystem.modules.diagnosis.dto.request;

import com.example.ehrsystem.modules.diagnosis.entity.DiagnosisStatus;
import jakarta.validation.constraints.Size;
import lombok.*;

import java.time.LocalDate;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UpdateDiagnosisRequest {

    @Size(max = 500, message = "Diagnosis name must be at most 500 characters")
    private String name;

    private DiagnosisStatus clinicalStatus;

    private LocalDate onsetDate;

    private LocalDate resolvedDate;

    @Size(max = 5000, message = "Notes must be at most 5000 characters")
    private String notes;

    private Long version;
}
