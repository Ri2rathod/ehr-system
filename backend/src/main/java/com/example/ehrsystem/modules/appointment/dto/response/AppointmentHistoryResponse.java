package com.example.ehrsystem.modules.appointment.dto.response;

import lombok.*;

import java.time.LocalDateTime;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AppointmentHistoryResponse {

    private UUID uuid;
    private String appointmentNumber;
    private String action;
    private LocalDateTime previousStartTime;
    private LocalDateTime previousEndTime;
    private String previousStatus;
    private LocalDateTime newStartTime;
    private LocalDateTime newEndTime;
    private String newStatus;
    private String reason;
    private Long performedBy;
    private LocalDateTime createdAt;
}
