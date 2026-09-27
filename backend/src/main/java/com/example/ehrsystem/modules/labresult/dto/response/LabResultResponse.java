package com.example.ehrsystem.modules.labresult.dto.response;

import com.example.ehrsystem.modules.labresult.entity.LabResultStatus;
import lombok.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class LabResultResponse {

    private Long id;
    private UUID uuid;
    private UUID labOrderUuid;
    private String orderNumber;
    private UUID labOrderItemUuid;
    private String labTestCode;
    private String labTestName;
    private UUID specimenUuid;
    private String specimenType;
    private String specimenStatus;
    private LocalDateTime collectedAt;
    private LocalDateTime receivedAt;
    private LabResultStatus status;
    private LocalDateTime resultedAt;
    private LocalDateTime verifiedAt;
    private Long verifiedBy;
    private String comments;
    private String correctionReason;
    private List<LabResultValueResponse> values;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private Long version;
}
