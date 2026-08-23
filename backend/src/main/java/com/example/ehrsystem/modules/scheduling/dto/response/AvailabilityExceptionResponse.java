package com.example.ehrsystem.modules.scheduling.dto.response;

import com.example.ehrsystem.modules.scheduling.entity.ExceptionType;
import lombok.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AvailabilityExceptionResponse {

    private Long id;
    private UUID uuid;
    private UUID doctorUuid;

    private LocalDate exceptionDate;
    private ExceptionType exceptionType;

    private LocalTime startTime;
    private LocalTime endTime;

    private Boolean isFullDay;

    private String reason;

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
