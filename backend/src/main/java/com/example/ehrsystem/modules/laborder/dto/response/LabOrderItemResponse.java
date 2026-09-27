package com.example.ehrsystem.modules.laborder.dto.response;

import com.example.ehrsystem.modules.laborder.entity.LabOrderItemStatus;
import lombok.*;

import java.time.LocalDateTime;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class LabOrderItemResponse {

    private Long id;
    private UUID uuid;
    private UUID labOrderUuid;
    private UUID labTestUuid;
    private String labTestCode;
    private String labTestName;
    private String instructions;
    private LabOrderItemStatus status;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private Long version;
}
