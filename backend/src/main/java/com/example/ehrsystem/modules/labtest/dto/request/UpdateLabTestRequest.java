package com.example.ehrsystem.modules.labtest.dto.request;

import com.example.ehrsystem.modules.labtest.entity.ResultType;
import com.example.ehrsystem.modules.labtest.entity.SpecimenType;
import jakarta.validation.constraints.Size;
import lombok.*;

import java.math.BigDecimal;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UpdateLabTestRequest {

    @Size(max = 300, message = "Name must be at most 300 characters")
    private String name;

    @Size(max = 100, message = "Short name must be at most 100 characters")
    private String shortName;

    private String description;

    @Size(max = 100, message = "Category must be at most 100 characters")
    private String category;

    private SpecimenType specimenType;

    private ResultType resultType;

    @Size(max = 50, message = "Unit must be at most 50 characters")
    private String unit;

    private BigDecimal defaultReferenceLow;

    private BigDecimal defaultReferenceHigh;

    @Size(max = 300, message = "Reference text must be at most 300 characters")
    private String defaultReferenceText;

    private Boolean isActive;

    private Long version;
}
