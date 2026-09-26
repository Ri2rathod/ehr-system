package com.example.ehrsystem.modules.encounter.dto.response;

import com.example.ehrsystem.modules.appointment.entity.VisitType;
import com.example.ehrsystem.modules.encounter.entity.EncounterStatus;
import lombok.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class EncounterResponse {

    private Long id;
    private UUID uuid;
    private String encounterNumber;

    private UUID patientUuid;
    private String patientName;
    private String patientMrn;
    private String patientGender;
    private LocalDate patientDateOfBirth;

    private UUID doctorUuid;
    private String doctorName;
    private String doctorCode;

    private UUID appointmentUuid;
    private String appointmentNumber;

    private VisitType encounterType;
    private EncounterStatus status;

    private LocalDateTime startedAt;
    private LocalDateTime endedAt;

    private String chiefComplaint;
    private String historyOfPresentIllness;
    private String clinicalNotes;
    private String assessment;
    private String treatmentPlan;
    private String followUpNotes;
    private String cancellationReason;

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private Long version;
}
