package com.example.ehrsystem.modules.scheduling.dto.request;

import com.example.ehrsystem.modules.scheduling.entity.ExceptionType;
import lombok.*;

import java.time.LocalDate;
import java.time.LocalTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UpdateExceptionRequest {

    private LocalDate exceptionDate;

    private ExceptionType exceptionType;

    private LocalTime startTime;

    private LocalTime endTime;

    private Boolean isFullDay;

    private String reason;
}
