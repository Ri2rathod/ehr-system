package com.example.ehrsystem.modules.scheduling.dto.request;

import com.example.ehrsystem.modules.scheduling.entity.DayOfWeekEnum;
import com.example.ehrsystem.modules.scheduling.entity.ScheduleType;
import lombok.*;

import java.time.LocalDate;
import java.time.LocalTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UpdateAvailabilityRequest {

    private DayOfWeekEnum dayOfWeek;

    private ScheduleType scheduleType;

    private LocalTime startTime;

    private LocalTime endTime;

    private Boolean isActive;

    private LocalDate effectiveFrom;

    private LocalDate effectiveUntil;
}
