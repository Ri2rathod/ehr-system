package com.example.ehrsystem.modules.scheduling.dto.request;

import com.example.ehrsystem.modules.scheduling.entity.DayOfWeekEnum;
import com.example.ehrsystem.modules.scheduling.entity.ScheduleType;
import jakarta.validation.constraints.NotNull;
import lombok.*;

import java.time.LocalDate;
import java.time.LocalTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CreateAvailabilityRequest {

    @NotNull(message = "Day of week is required")
    private DayOfWeekEnum dayOfWeek;

    @NotNull(message = "Schedule type is required")
    private ScheduleType scheduleType;

    @NotNull(message = "Start time is required")
    private LocalTime startTime;

    @NotNull(message = "End time is required")
    private LocalTime endTime;

    private Boolean isActive;

    private LocalDate effectiveFrom;

    private LocalDate effectiveUntil;
}
