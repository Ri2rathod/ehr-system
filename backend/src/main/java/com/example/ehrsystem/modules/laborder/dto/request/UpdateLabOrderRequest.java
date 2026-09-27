package com.example.ehrsystem.modules.laborder.dto.request;

import com.example.ehrsystem.modules.laborder.entity.LabOrderPriority;
import jakarta.validation.constraints.Size;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UpdateLabOrderRequest {

    private LabOrderPriority priority;

    @Size(max = 2000, message = "Instructions must be at most 2000 characters")
    private String instructions;

    private Long version;
}
