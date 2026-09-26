package com.example.ehrsystem.modules.encounter.dto.request;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UpdateVitalsRequest {

    @DecimalMin(value = "25.0", message = "Temperature must be at least 25°C")
    @DecimalMax(value = "45.0", message = "Temperature must be at most 45°C")
    private BigDecimal temperature;

    @Min(value = 0, message = "Heart rate must be at least 0")
    @Max(value = 300, message = "Heart rate must be at most 300")
    private Integer heartRate;

    @Min(value = 0, message = "Respiratory rate must be at least 0")
    @Max(value = 100, message = "Respiratory rate must be at most 100")
    private Integer respiratoryRate;

    @Min(value = 0, message = "Systolic blood pressure must be at least 0")
    @Max(value = 300, message = "Systolic blood pressure must be at most 300")
    private Integer systolicBp;

    @Min(value = 0, message = "Diastolic blood pressure must be at least 0")
    @Max(value = 200, message = "Diastolic blood pressure must be at most 200")
    private Integer diastolicBp;

    @DecimalMin(value = "0.0", message = "Oxygen saturation must be at least 0")
    @DecimalMax(value = "100.0", message = "Oxygen saturation must be at most 100")
    private BigDecimal oxygenSaturation;

    @DecimalMin(value = "0.0", message = "Weight must be at least 0")
    @DecimalMax(value = "500.0", message = "Weight must be at most 500")
    private BigDecimal weight;

    @DecimalMin(value = "0.0", message = "Height must be at least 0")
    @DecimalMax(value = "250.0", message = "Height must be at most 250")
    private BigDecimal height;

    private LocalDateTime recordedAt;

    private Long version;
}
