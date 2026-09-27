package com.example.ehrsystem.modules.prescription.dto.request;

import com.example.ehrsystem.modules.medication.entity.DoseUnit;
import com.example.ehrsystem.modules.medication.entity.Route;
import com.example.ehrsystem.modules.prescription.entity.FrequencyUnit;
import com.example.ehrsystem.modules.prescription.entity.MedicationFrequency;
import com.example.ehrsystem.modules.prescription.entity.QuantityUnit;
import com.example.ehrsystem.modules.treatmentplan.entity.DurationUnit;
import jakarta.validation.constraints.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CreatePrescriptionItemRequest {

    @NotNull(message = "Medication is required")
    private UUID medicationUuid;

    @NotNull(message = "Dose is required")
    @PositiveOrZero(message = "Dose must not be negative")
    private BigDecimal dose;

    @NotNull(message = "Dose unit is required")
    private DoseUnit doseUnit;

    @NotNull(message = "Route is required")
    private Route route;

    @NotNull(message = "Frequency is required")
    private MedicationFrequency frequency;

    @Min(value = 1, message = "Frequency value must be at least 1")
    private Integer frequencyValue;

    private FrequencyUnit frequencyUnit;

    @Min(value = 1, message = "Duration must be at least 1")
    private Integer duration;

    private DurationUnit durationUnit;

    @PositiveOrZero(message = "Quantity must not be negative")
    private Integer quantity;

    private QuantityUnit quantityUnit;

    @PositiveOrZero(message = "Refills must not be negative")
    private Integer refills;

    @Size(max = 5000, message = "Instructions must be at most 5000 characters")
    private String instructions;

    private LocalDate startDate;

    private LocalDate endDate;
}
