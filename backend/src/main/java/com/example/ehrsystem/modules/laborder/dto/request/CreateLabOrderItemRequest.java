package com.example.ehrsystem.modules.laborder.dto.request;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.*;

import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CreateLabOrderItemRequest {

    @NotNull(message = "Lab test is required")
    private UUID labTestUuid;

    @Size(max = 2000, message = "Instructions must be at most 2000 characters")
    private String instructions;
}
