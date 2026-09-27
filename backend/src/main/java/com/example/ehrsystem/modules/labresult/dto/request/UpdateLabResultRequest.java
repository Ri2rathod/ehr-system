package com.example.ehrsystem.modules.labresult.dto.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;
import lombok.*;

import java.util.List;

/**
 * Edits to a PRELIMINARY result: comments and a full replacement of the
 * value set (previous value rows are soft deleted for history).
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UpdateLabResultRequest {

    @Size(max = 5000, message = "Comments must be at most 5000 characters")
    private String comments;

    @NotEmpty(message = "At least one result value is required")
    @Valid
    private List<CreateLabResultValueRequest> values;

    private Long version;
}
