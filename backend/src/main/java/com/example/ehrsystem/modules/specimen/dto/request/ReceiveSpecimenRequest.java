package com.example.ehrsystem.modules.specimen.dto.request;

import lombok.*;

import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ReceiveSpecimenRequest {

    /** Receipt time; defaults to now when omitted. */
    private LocalDateTime receivedAt;
}
