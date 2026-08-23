package com.example.ehrsystem.modules.scheduling.dto.response;

import com.example.ehrsystem.modules.scheduling.entity.DayOfWeekEnum;
import com.example.ehrsystem.modules.scheduling.entity.ScheduleType;
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
public class AvailabilityResponse {

    private Long id;
    private UUID uuid;
    private UUID doctorUuid;

    private DayOfWeekEnum dayOfWeek;
    private ScheduleType scheduleType;

    private LocalTime startTime;
    private LocalTime endTime;

    private Boolean isActive;

    private LocalDate effectiveFrom;
    private LocalDate effectiveUntil;

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
