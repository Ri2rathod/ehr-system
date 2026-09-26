package com.example.ehrsystem.modules.encounter.dto.request;

import com.example.ehrsystem.modules.appointment.entity.VisitType;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.*;

import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CreateEncounterRequest {

    @NotNull(message = "Appointment UUID is required")
    private UUID appointmentUuid;

    private VisitType encounterType;

    @Size(max = 500, message = "Chief complaint must be at most 500 characters")
    private String chiefComplaint;

    @Size(max = 5000, message = "History of present illness must be at most 5000 characters")
    private String historyOfPresentIllness;
}
