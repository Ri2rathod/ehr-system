package com.example.ehrsystem.modules.labresult.dto.response;

import com.example.ehrsystem.modules.labresult.entity.AbnormalFlag;
import lombok.*;

import java.math.BigDecimal;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class LabResultValueResponse {

    private Long id;
    private UUID uuid;
    private UUID labTestUuid;
    private String labTestCode;
    private String labTestName;
    private BigDecimal valueNumeric;
    private String valueText;
    private String valueCode;
    private String unit;
    private BigDecimal referenceLow;
    private BigDecimal referenceHigh;
    private String referenceText;
    private AbnormalFlag abnormalFlag;
    private String notes;
    private Long version;
}
