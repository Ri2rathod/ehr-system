package com.example.ehrsystem.modules.medication.dto.request;

import com.example.ehrsystem.modules.medication.entity.DoseUnit;
import com.example.ehrsystem.modules.medication.entity.DosageForm;
import com.example.ehrsystem.modules.medication.entity.Route;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.*;

import java.math.BigDecimal;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CreateMedicationRequest {

    @Size(max = 50, message = "Code must be at most 50 characters")
    private String code;

    @Size(max = 50, message = "Code system must be at most 50 characters")
    private String codeSystem;

    @NotBlank(message = "Generic name is required")
    @Size(max = 300, message = "Generic name must be at most 300 characters")
    private String genericName;

    @Size(max = 300, message = "Brand name must be at most 300 characters")
    private String brandName;

    @DecimalMin(value = "0", message = "Strength must not be negative")
    private BigDecimal strength;

    private DoseUnit strengthUnit;

    private DosageForm dosageForm;

    private Route route;
}
