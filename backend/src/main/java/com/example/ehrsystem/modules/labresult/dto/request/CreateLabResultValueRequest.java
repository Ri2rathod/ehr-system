package com.example.ehrsystem.modules.labresult.dto.request;

import com.example.ehrsystem.modules.labresult.entity.AbnormalFlag;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.*;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * One value row. Validation of the representation (numeric/text/code)
 * against the lab test's result type happens in the service - exactly one
 * representation must be supplied; reference fields fall back to the
 * catalog defaults when omitted.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CreateLabResultValueRequest {

    @NotNull(message = "Lab test is required")
    private UUID labTestUuid;

    private BigDecimal valueNumeric;

    @Size(max = 500, message = "Value text must be at most 500 characters")
    private String valueText;

    @Size(max = 100, message = "Value code must be at most 100 characters")
    private String valueCode;

    @Size(max = 50, message = "Unit must be at most 50 characters")
    private String unit;

    private BigDecimal referenceLow;

    private BigDecimal referenceHigh;

    @Size(max = 300, message = "Reference text must be at most 300 characters")
    private String referenceText;

    private AbnormalFlag abnormalFlag;

    @Size(max = 2000, message = "Notes must be at most 2000 characters")
    private String notes;
}
