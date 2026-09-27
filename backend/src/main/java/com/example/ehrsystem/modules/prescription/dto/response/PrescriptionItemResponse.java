package com.example.ehrsystem.modules.prescription.dto.response;

import com.example.ehrsystem.modules.medication.entity.DoseUnit;
import com.example.ehrsystem.modules.medication.entity.DosageForm;
import com.example.ehrsystem.modules.medication.entity.Route;
import com.example.ehrsystem.modules.prescription.entity.FrequencyUnit;
import com.example.ehrsystem.modules.prescription.entity.MedicationFrequency;
import com.example.ehrsystem.modules.prescription.entity.PrescriptionItemStatus;
import com.example.ehrsystem.modules.prescription.entity.QuantityUnit;
import com.example.ehrsystem.modules.treatmentplan.entity.DurationUnit;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PrescriptionItemResponse {

    private Long id;
    private UUID uuid;
    private UUID prescriptionUuid;

    private UUID medicationUuid;
    private String medicationCode;
    private String medicationGenericName;
    private String medicationBrandName;
    private BigDecimal medicationStrength;
    private DoseUnit medicationStrengthUnit;
    private DosageForm medicationDosageForm;
    private Route medicationRoute;

    private BigDecimal dose;
    private DoseUnit doseUnit;
    private Route route;
    private MedicationFrequency frequency;
    private Integer frequencyValue;
    private FrequencyUnit frequencyUnit;
    private Integer duration;
    private DurationUnit durationUnit;
    private Integer quantity;
    private QuantityUnit quantityUnit;
    private Integer refills;
    private String instructions;
    private LocalDate startDate;
    private LocalDate endDate;
    private PrescriptionItemStatus status;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private Long version;
}
