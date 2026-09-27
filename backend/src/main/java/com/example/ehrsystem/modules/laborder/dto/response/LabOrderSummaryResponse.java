package com.example.ehrsystem.modules.laborder.dto.response;

import com.example.ehrsystem.modules.laborder.entity.LabOrderPriority;
import com.example.ehrsystem.modules.laborder.entity.LabOrderStatus;
import lombok.*;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Lab order list projection: identity, workflow state and per-order counts
 * (tests, specimen progress, result progress) without embedded children.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class LabOrderSummaryResponse {

    private Long id;
    private UUID uuid;
    private UUID encounterUuid;
    private String orderNumber;
    private LabOrderPriority priority;
    private LabOrderStatus status;
    private String instructions;
    private LocalDateTime orderedAt;
    private long itemCount;
    private long specimenCount;
    private String specimenStatus;
    private long resultCount;
    private long finalizedResultCount;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private Long version;
}
