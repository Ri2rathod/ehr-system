package com.example.ehrsystem.modules.appointment.dto.response;

import com.example.ehrsystem.modules.appointment.entity.AppointmentStatus;
import com.example.ehrsystem.modules.appointment.entity.VisitType;
import lombok.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AppointmentResponse {
    private Long id;
    private UUID uuid;
    private String appointmentNumber;
    private UUID patientUuid;
    private String patientName;
    private String patientMrn;
    private UUID doctorUuid;
    private String doctorName;
    private String doctorCode;
    private LocalDate appointmentDate;
    private LocalDateTime startTime;
    private LocalDateTime endTime;
    private VisitType visitType;
    private AppointmentStatus status;
    private String reasonForVisit;
    private String notes;
    private String cancellationReason;
    private LocalDateTime checkedInAt;
    private LocalDateTime inProgressAt;
    private LocalDateTime completedAt;
    private LocalDateTime cancelledAt;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
