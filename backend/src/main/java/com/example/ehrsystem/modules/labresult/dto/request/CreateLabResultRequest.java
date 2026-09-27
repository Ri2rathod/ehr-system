package com.example.ehrsystem.modules.labresult.dto.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.*;

import java.util.List;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CreateLabResultRequest {

    @NotNull(message = "Lab order item is required")
    private UUID labOrderItemUuid;

    @NotNull(message = "Specimen is required")
    private UUID specimenUuid;

    @Size(max = 5000, message = "Comments must be at most 5000 characters")
    private String comments;

    @NotEmpty(message = "At least one result value is required")
    @Valid
    private List<CreateLabResultValueRequest> values;
}
