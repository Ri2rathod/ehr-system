package com.example.ehrsystem.modules.labtest.dto.response;

import com.example.ehrsystem.modules.labtest.entity.ResultType;
import com.example.ehrsystem.modules.labtest.entity.SpecimenType;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class LabTestResponse {

    private Long id;
    private UUID uuid;
    private String code;
    private String codeSystem;
    private String name;
    private String shortName;
    private String description;
    private String category;
    private SpecimenType specimenType;
    private ResultType resultType;
    private String unit;
    private BigDecimal defaultReferenceLow;
    private BigDecimal defaultReferenceHigh;
    private String defaultReferenceText;
    private Boolean isActive;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private Long version;
}
