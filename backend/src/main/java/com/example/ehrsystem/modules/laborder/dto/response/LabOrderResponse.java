package com.example.ehrsystem.modules.laborder.dto.response;

import com.example.ehrsystem.modules.laborder.entity.LabOrderPriority;
import com.example.ehrsystem.modules.laborder.entity.LabOrderStatus;
import lombok.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

/**
 * Full lab order detail: summary fields plus its ordered tests.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class LabOrderResponse {

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
    private List<LabOrderItemResponse> items;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private Long version;
}
