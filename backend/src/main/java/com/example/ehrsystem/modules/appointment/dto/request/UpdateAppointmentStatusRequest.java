package com.example.ehrsystem.modules.appointment.dto.request;

import com.example.ehrsystem.modules.appointment.entity.AppointmentStatus;
import jakarta.validation.constraints.NotNull;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UpdateAppointmentStatusRequest {

    @NotNull(message = "Status is required")
    private AppointmentStatus status;

    private String cancellationReason;
}
