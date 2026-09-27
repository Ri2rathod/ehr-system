package com.example.ehrsystem.modules.laborder.dto.request;

import com.example.ehrsystem.modules.laborder.entity.LabOrderPriority;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Size;
import lombok.*;

import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CreateLabOrderRequest {

    private LabOrderPriority priority;

    @Size(max = 2000, message = "Instructions must be at most 2000 characters")
    private String instructions;

    @Valid
    private List<CreateLabOrderItemRequest> items;
}
