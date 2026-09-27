package com.example.ehrsystem.modules.medication.dto.response;

import com.example.ehrsystem.modules.medication.entity.DoseUnit;
import com.example.ehrsystem.modules.medication.entity.DosageForm;
import com.example.ehrsystem.modules.medication.entity.Route;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MedicationResponse {

    private Long id;
    private UUID uuid;
    private String code;
    private String codeSystem;
    private String genericName;
    private String brandName;
    private BigDecimal strength;
    private DoseUnit strengthUnit;
    private DosageForm dosageForm;
    private Route route;
    private Boolean isActive;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private Long version;
}
