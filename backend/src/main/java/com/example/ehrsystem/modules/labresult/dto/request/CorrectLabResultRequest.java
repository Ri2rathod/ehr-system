package com.example.ehrsystem.modules.labresult.dto.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;
import lombok.*;

import java.util.List;

/**
 * Correction of a FINAL result: mandatory reason plus a full replacement
 * of the value set.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CorrectLabResultRequest {

    @NotBlank(message = "Correction reason is required")
    @Size(max = 1000, message = "Correction reason must be at most 1000 characters")
    private String correctionReason;

    @Size(max = 5000, message = "Comments must be at most 5000 characters")
    private String comments;

    @NotEmpty(message = "At least one result value is required")
    @Valid
    private List<CreateLabResultValueRequest> values;

    private Long version;
}
