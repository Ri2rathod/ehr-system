package com.example.ehrsystem.modules.labtest.dto.response;

import com.example.ehrsystem.modules.labtest.entity.ResultType;
import com.example.ehrsystem.modules.labtest.entity.SpecimenType;
import lombok.*;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * Lightweight projection for search/autocomplete lists.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class LabTestSummaryResponse {

    private UUID uuid;
    private String code;
    private String name;
    private String shortName;
    private String category;
    private SpecimenType specimenType;
    private ResultType resultType;
    private String unit;
    private BigDecimal defaultReferenceLow;
    private BigDecimal defaultReferenceHigh;
    private String defaultReferenceText;
    private Boolean isActive;
}
