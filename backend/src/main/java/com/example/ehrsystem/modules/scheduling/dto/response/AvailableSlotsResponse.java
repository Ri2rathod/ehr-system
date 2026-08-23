package com.example.ehrsystem.modules.scheduling.dto.response;

import lombok.*;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AvailableSlotsResponse {

    private UUID doctorUuid;
    private LocalDate date;
    private int slotDurationMinutes;
    private List<AvailableSlotResponse> slots;
}
