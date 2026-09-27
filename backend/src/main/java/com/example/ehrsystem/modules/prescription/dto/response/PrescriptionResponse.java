package com.example.ehrsystem.modules.prescription.dto.response;

import com.example.ehrsystem.modules.prescription.entity.PrescriptionStatus;
import lombok.*;

import java.time.LocalDateTime;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PrescriptionResponse {

    private Long id;
    private UUID uuid;
    private UUID encounterUuid;
    private UUID patientUuid;
    private UUID doctorUuid;
    private String prescriptionNumber;
    private PrescriptionStatus status;
    private LocalDateTime prescribedAt;
    private String notes;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private Long version;
}
