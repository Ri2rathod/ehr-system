package com.example.ehrsystem.modules.appointment.dto.request;

import com.example.ehrsystem.modules.appointment.entity.VisitType;
import jakarta.validation.constraints.FutureOrPresent;
import jakarta.validation.constraints.NotNull;
import lombok.*;

import java.time.LocalDateTime;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CreateAppointmentRequest {

    @NotNull(message = "Patient UUID is required")
    private UUID patientUuid;

    @NotNull(message = "Doctor UUID is required")
    private UUID doctorUuid;

    @NotNull(message = "Start time is required")
    @FutureOrPresent(message = "Start time must be in the present or future")
    private LocalDateTime startTime;

    @NotNull(message = "End time is required")
    @FutureOrPresent(message = "End time must be in the present or future")
    private LocalDateTime endTime;

    @NotNull(message = "Visit type is required")
    private VisitType visitType;

    private String reasonForVisit;
    private String notes;
}
