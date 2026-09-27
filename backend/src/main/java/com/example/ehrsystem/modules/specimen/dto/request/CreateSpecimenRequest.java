package com.example.ehrsystem.modules.specimen.dto.request;

import com.example.ehrsystem.modules.labtest.entity.SpecimenType;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CreateSpecimenRequest {

    @NotNull(message = "Specimen type is required")
    private SpecimenType specimenType;

    @Size(max = 100, message = "Specimen identifier must be at most 100 characters")
    private String specimenIdentifier;

    @Size(max = 2000, message = "Notes must be at most 2000 characters")
    private String notes;
}
