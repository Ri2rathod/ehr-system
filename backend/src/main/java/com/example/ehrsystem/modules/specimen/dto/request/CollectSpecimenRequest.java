package com.example.ehrsystem.modules.specimen.dto.request;

import jakarta.validation.constraints.Size;
import lombok.*;

import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CollectSpecimenRequest {

    /** Collection time; defaults to now when omitted. */
    private LocalDateTime collectedAt;

    @Size(max = 100, message = "Specimen identifier must be at most 100 characters")
    private String specimenIdentifier;
}
