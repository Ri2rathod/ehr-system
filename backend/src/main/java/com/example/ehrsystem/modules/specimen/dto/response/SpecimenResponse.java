package com.example.ehrsystem.modules.specimen.dto.response;

import com.example.ehrsystem.modules.labtest.entity.SpecimenType;
import com.example.ehrsystem.modules.specimen.entity.SpecimenStatus;
import lombok.*;

import java.time.LocalDateTime;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SpecimenResponse {

    private Long id;
    private UUID uuid;
    private UUID labOrderUuid;
    private String orderNumber;
    private SpecimenType specimenType;
    private String specimenIdentifier;
    private SpecimenStatus status;
    private LocalDateTime collectedAt;
    private Long collectedBy;
    private LocalDateTime receivedAt;
    private Long receivedBy;
    private String rejectionReason;
    private String notes;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private Long version;
}
