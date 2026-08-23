package com.example.ehrsystem.modules.scheduling.dto.request;

import com.example.ehrsystem.modules.scheduling.entity.ExceptionType;
import jakarta.validation.constraints.NotNull;
import lombok.*;

import java.time.LocalDate;
import java.time.LocalTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CreateExceptionRequest {

    @NotNull(message = "Exception date is required")
    private LocalDate exceptionDate;

    @NotNull(message = "Exception type is required")
    private ExceptionType exceptionType;

    private LocalTime startTime;

    private LocalTime endTime;

    private Boolean isFullDay;

    private String reason;
}
