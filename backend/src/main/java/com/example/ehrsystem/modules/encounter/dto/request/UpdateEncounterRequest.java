package com.example.ehrsystem.modules.encounter.dto.request;

import jakarta.validation.constraints.Size;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UpdateEncounterRequest {

    @Size(max = 500, message = "Chief complaint must be at most 500 characters")
    private String chiefComplaint;

    @Size(max = 5000, message = "History of present illness must be at most 5000 characters")
    private String historyOfPresentIllness;

    @Size(max = 10000, message = "Clinical notes must be at most 10000 characters")
    private String clinicalNotes;

    @Size(max = 10000, message = "Assessment must be at most 10000 characters")
    private String assessment;

    @Size(max = 10000, message = "Treatment plan must be at most 10000 characters")
    private String treatmentPlan;

    @Size(max = 5000, message = "Follow-up notes must be at most 5000 characters")
    private String followUpNotes;

    private Long version;
}
