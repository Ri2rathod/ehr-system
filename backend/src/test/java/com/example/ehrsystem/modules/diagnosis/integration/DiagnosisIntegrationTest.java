package com.example.ehrsystem.modules.diagnosis.integration;

import com.example.ehrsystem.modules.diagnosis.entity.Diagnosis;
import com.example.ehrsystem.modules.diagnosis.repository.DiagnosisRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class DiagnosisIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private DiagnosisRepository diagnosisRepository;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private TransactionTemplate transactionTemplate;

    private String adminToken;
    private String patientToken;
    private UUID patientUuid;
    private UUID doctorUuid;
    private LocalDate monday;

    private UUID encounterAUuid;
    private UUID encounterBUuid;
    private UUID encounterCUuid;
    private UUID encounterDUuid;

    private UUID diagnosisD1Uuid; // secondary in encounter A
    private UUID diagnosisD2Uuid; // original primary in encounter A (later demoted)
    private UUID diagnosisD3Uuid; // replacement primary in encounter A
    private UUID diagnosisD4Uuid; // diagnosis in encounter B
    private UUID diagnosisDcUuid; // diagnosis in encounter C (before completion)

    private String authHeader() {
        return "Bearer " + adminToken;
    }

    private String getAdminToken() throws Exception {
        var loginPayload = Map.of("email", "admin@ehr.local", "password", "Admin@123");
        var result = mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginPayload)))
                .andExpect(status().isOk())
                .andReturn();
        return JsonPath.parse(result.getResponse().getContentAsString()).read("$.accessToken");
    }

    private String registerAndLogin(String suffix) throws Exception {
        var registerPayload = Map.of(
                "firstName", "DiagnosisUser",
                "lastName", "Test" + suffix,
                "email", "diagnosis.user." + suffix + "@example.com",
                "password", "Secret123!",
                "username", "diagnosisuser" + suffix
        );
        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(registerPayload)))
                .andExpect(status().isCreated());

        var loginPayload = Map.of("email", "diagnosis.user." + suffix + "@example.com", "password", "Secret123!");
        var result = mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginPayload)))
                .andExpect(status().isOk())
                .andReturn();
        return JsonPath.parse(result.getResponse().getContentAsString()).read("$.accessToken");
    }

    private LocalDate nextDayOfWeek(DayOfWeek dayOfWeek) {
        LocalDate date = LocalDate.now().plusDays(1);
        while (date.getDayOfWeek() != dayOfWeek) {
            date = date.plusDays(1);
        }
        return date;
    }

    private String createAppointment(LocalDateTime start, String visitType) throws Exception {
        var payload = Map.of(
                "patientUuid", patientUuid.toString(),
                "doctorUuid", doctorUuid.toString(),
                "startTime", start.toString(),
                "endTime", start.plusMinutes(30).toString(),
                "visitType", visitType,
                "reasonForVisit", "Diagnosis integration test"
        );
        var result = mockMvc.perform(post("/api/v1/appointments")
                        .header("Authorization", authHeader())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(payload)))
                .andExpect(status().isCreated())
                .andReturn();
        return JsonPath.parse(result.getResponse().getContentAsString()).read("$.uuid");
    }

    private void transitionAppointment(String appointmentUuid, String status) throws Exception {
        mockMvc.perform(put("/api/v1/appointments/" + appointmentUuid + "/status")
                        .header("Authorization", authHeader())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("status", status))))
                .andExpect(status().isOk());
    }

    private String createEncounter(String appointmentUuid, String chiefComplaint) throws Exception {
        var payload = new java.util.HashMap<String, Object>();
        payload.put("appointmentUuid", appointmentUuid);
        if (chiefComplaint != null) payload.put("chiefComplaint", chiefComplaint);

        var result = mockMvc.perform(post("/api/v1/encounters")
                        .header("Authorization", authHeader())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(payload)))
                .andExpect(status().isCreated())
                .andReturn();
        return JsonPath.parse(result.getResponse().getContentAsString()).read("$.uuid");
    }

    private String createCheckedInEncounter(LocalDateTime start, String chiefComplaint) throws Exception {
        String appointmentUuid = createAppointment(start, "CONSULTATION");
        transitionAppointment(appointmentUuid, "CONFIRMED");
        transitionAppointment(appointmentUuid, "CHECKED_IN");
        return createEncounter(appointmentUuid, chiefComplaint);
    }

    @BeforeAll
    void setupAll() throws Exception {
        adminToken = getAdminToken();
        long suffix = System.currentTimeMillis();
        patientToken = registerAndLogin(Long.toString(suffix));

        var patientPayload = Map.of(
                "firstName", "DiagnosisPatient",
                "lastName", "Test",
                "gender", "MALE",
                "email", "diagnosis.patient." + suffix + "@test.com"
        );
        var patientResult = mockMvc.perform(post("/api/v1/patients")
                        .header("Authorization", authHeader())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(patientPayload)))
                .andExpect(status().isCreated())
                .andReturn();
        patientUuid = UUID.fromString(JsonPath.parse(patientResult.getResponse().getContentAsString()).read("$.uuid"));

        var doctorPayload = Map.of(
                "firstName", "DiagnosisDoctor",
                "lastName", "Test",
                "email", "diagnosis.doctor." + suffix + "@test.com",
                "specialization", "INTERNAL_MEDICINE"
        );
        var doctorResult = mockMvc.perform(post("/api/v1/doctors")
                        .header("Authorization", authHeader())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(doctorPayload)))
                .andExpect(status().isCreated())
                .andReturn();
        doctorUuid = UUID.fromString(JsonPath.parse(doctorResult.getResponse().getContentAsString()).read("$.uuid"));

        for (DayOfWeek day : DayOfWeek.values()) {
            if (day == DayOfWeek.SATURDAY || day == DayOfWeek.SUNDAY) continue;
            for (var range : new String[][]{
                    {"WORK", "09:00:00", "13:00:00"},
                    {"BREAK", "13:00:00", "14:00:00"},
                    {"WORK", "14:00:00", "18:00:00"}}) {
                var payload = Map.of(
                        "dayOfWeek", day.name(),
                        "scheduleType", range[0],
                        "startTime", range[1],
                        "endTime", range[2]
                );
                mockMvc.perform(post("/api/v1/doctors/" + doctorUuid + "/availability")
                                .header("Authorization", authHeader())
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(payload)))
                        .andExpect(status().isCreated());
            }
        }

        monday = nextDayOfWeek(DayOfWeek.MONDAY);
        encounterAUuid = UUID.fromString(createCheckedInEncounter(monday.atTime(9, 0), "Chest pain on exertion"));
        encounterBUuid = UUID.fromString(createCheckedInEncounter(monday.atTime(10, 0), "Follow-up visit"));
        encounterCUuid = UUID.fromString(createCheckedInEncounter(monday.atTime(11, 0), "Diabetes review"));
        encounterDUuid = UUID.fromString(createCheckedInEncounter(monday.atTime(12, 0), "Routine visit"));
    }

    // =============================================================
    // Creation
    // =============================================================

    @Test
    @Order(1)
    void createSecondaryDiagnosis_success() throws Exception {
        var payload = Map.of(
                "code", "E66.9",
                "codeSystem", "ICD-10-CM",
                "name", "Obesity, unspecified",
                "diagnosisType", "SECONDARY",
                "clinicalStatus", "ACTIVE"
        );

        mockMvc.perform(post("/api/v1/encounters/" + encounterAUuid + "/diagnoses")
                        .header("Authorization", authHeader())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(payload)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.uuid").exists())
                .andExpect(jsonPath("$.encounterUuid").value(encounterAUuid.toString()))
                .andExpect(jsonPath("$.code").value("E66.9"))
                .andExpect(jsonPath("$.codeSystem").value("ICD-10-CM"))
                .andExpect(jsonPath("$.name").value("Obesity, unspecified"))
                .andExpect(jsonPath("$.diagnosisType").value("SECONDARY"))
                .andExpect(jsonPath("$.clinicalStatus").value("ACTIVE"))
                .andExpect(jsonPath("$.version").value(0));

        var result = mockMvc.perform(get("/api/v1/encounters/" + encounterAUuid + "/diagnoses")
                        .header("Authorization", authHeader()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].code").value("E66.9"))
                .andReturn();
        diagnosisD1Uuid = UUID.fromString(JsonPath.parse(result.getResponse().getContentAsString()).read("$[0].uuid"));
    }

    @Test
    @Order(2)
    void createPrimaryDiagnosis_success() throws Exception {
        var payload = Map.of(
                "code", "E11.9",
                "codeSystem", "ICD-10-CM",
                "name", "Type 2 diabetes mellitus without complications",
                "diagnosisType", "PRIMARY",
                "clinicalStatus", "ACTIVE",
                "onsetDate", "2026-03-12",
                "notes", "Known type 2 diabetes"
        );

        var result = mockMvc.perform(post("/api/v1/encounters/" + encounterAUuid + "/diagnoses")
                        .header("Authorization", authHeader())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(payload)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.diagnosisType").value("PRIMARY"))
                .andExpect(jsonPath("$.code").value("E11.9"))
                .andExpect(jsonPath("$.onsetDate").value("2026-03-12"))
                .andReturn();
        diagnosisD2Uuid = UUID.fromString(JsonPath.parse(result.getResponse().getContentAsString()).read("$.uuid"));
    }

    @Test
    @Order(3)
    void createSecondPrimary_rejectedWithConflict() throws Exception {
        var payload = Map.of(
                "code", "I10",
                "codeSystem", "ICD-10-CM",
                "name", "Essential (primary) hypertension",
                "diagnosisType", "PRIMARY"
        );

        mockMvc.perform(post("/api/v1/encounters/" + encounterAUuid + "/diagnoses")
                        .header("Authorization", authHeader())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(payload)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message")
                        .value("A primary diagnosis already exists for this encounter."));
    }

    @Test
    @Order(4)
    void replacePrimary_diagnoses() throws Exception {
        var payload = Map.of(
                "code", "I10",
                "codeSystem", "ICD-10-CM",
                "name", "Essential (primary) hypertension",
                "diagnosisType", "PRIMARY",
                "replacePrimary", true
        );

        var result = mockMvc.perform(post("/api/v1/encounters/" + encounterAUuid + "/diagnoses")
                        .header("Authorization", authHeader())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(payload)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.diagnosisType").value("PRIMARY"))
                .andReturn();
        diagnosisD3Uuid = UUID.fromString(JsonPath.parse(result.getResponse().getContentAsString()).read("$.uuid"));

        // The previous primary was demoted to SECONDARY
        mockMvc.perform(get("/api/v1/encounters/" + encounterAUuid + "/diagnoses/" + diagnosisD2Uuid)
                        .header("Authorization", authHeader()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.diagnosisType").value("SECONDARY"));
    }

    @Test
    @Order(5)
    void createDiagnosis_inSecondEncounter_success() throws Exception {
        var payload = Map.of(
                "name", "Essential (primary) hypertension",
                "code", "I10",
                "codeSystem", "ICD-10-CM",
                "diagnosisType", "SECONDARY"
        );

        var result = mockMvc.perform(post("/api/v1/encounters/" + encounterBUuid + "/diagnoses")
                        .header("Authorization", authHeader())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(payload)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.encounterUuid").value(encounterBUuid.toString()))
                .andReturn();
        diagnosisD4Uuid = UUID.fromString(JsonPath.parse(result.getResponse().getContentAsString()).read("$.uuid"));
    }

    // =============================================================
    // Retrieval and encounter-scoped access
    // =============================================================

    @Test
    @Order(6)
    void getDiagnosis_success() throws Exception {
        mockMvc.perform(get("/api/v1/encounters/" + encounterAUuid + "/diagnoses/" + diagnosisD1Uuid)
                        .header("Authorization", authHeader()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.uuid").value(diagnosisD1Uuid.toString()))
                .andExpect(jsonPath("$.encounterUuid").value(encounterAUuid.toString()))
                .andExpect(jsonPath("$.name").value("Obesity, unspecified"));
    }

    @Test
    @Order(7)
    void getDiagnosis_fromAnotherEncounter_notFound() throws Exception {
        mockMvc.perform(get("/api/v1/encounters/" + encounterAUuid + "/diagnoses/" + diagnosisD4Uuid)
                        .header("Authorization", authHeader()))
                .andExpect(status().isNotFound());
    }

    @Test
    @Order(8)
    void updateDiagnosis_fromAnotherEncounter_notFound() throws Exception {
        var payload = Map.of("name", "Renamed from wrong encounter", "version", 0);

        mockMvc.perform(put("/api/v1/encounters/" + encounterAUuid + "/diagnoses/" + diagnosisD4Uuid)
                        .header("Authorization", authHeader())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(payload)))
                .andExpect(status().isNotFound());
    }

    @Test
    @Order(9)
    void listDiagnoses_primaryFirst() throws Exception {
        var result = mockMvc.perform(get("/api/v1/encounters/" + encounterAUuid + "/diagnoses")
                        .header("Authorization", authHeader()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(3))
                .andExpect(jsonPath("$[0].diagnosisType").value("PRIMARY"))
                .andReturn();

        String body = result.getResponse().getContentAsString();
        assertThat(JsonPath.parse(body).read("$[0].uuid").toString()).isEqualTo(diagnosisD3Uuid.toString());
        // encounter B diagnoses are not exposed through encounter A
        assertThat(body).doesNotContain(diagnosisD4Uuid.toString());
    }

    // =============================================================
    // Update
    // =============================================================

    @Test
    @Order(10)
    void updateDiagnosis_success() throws Exception {
        var payload = Map.of(
                "name", "Obesity, unspecified (updated)",
                "clinicalStatus", "RESOLVED",
                "resolvedDate", "2026-09-01",
                "version", 0
        );

        mockMvc.perform(put("/api/v1/encounters/" + encounterAUuid + "/diagnoses/" + diagnosisD1Uuid)
                        .header("Authorization", authHeader())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(payload)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Obesity, unspecified (updated)"))
                .andExpect(jsonPath("$.clinicalStatus").value("RESOLVED"))
                .andExpect(jsonPath("$.resolvedDate").value("2026-09-01"))
                .andExpect(jsonPath("$.version").value(1));
    }

    @Test
    @Order(11)
    void updateDiagnosis_invalidStatus_badRequest() throws Exception {
        var payload = Map.of("clinicalStatus", "BOGUS_STATUS");

        mockMvc.perform(put("/api/v1/encounters/" + encounterAUuid + "/diagnoses/" + diagnosisD1Uuid)
                        .header("Authorization", authHeader())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(payload)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @Order(12)
    void updateDiagnosis_invalidDateOrdering_badRequest() throws Exception {
        var payload = Map.of(
                "onsetDate", "2026-03-01",
                "resolvedDate", "2026-02-01"
        );

        mockMvc.perform(put("/api/v1/encounters/" + encounterAUuid + "/diagnoses/" + diagnosisD1Uuid)
                        .header("Authorization", authHeader())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(payload)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Resolved date must be on or after onset date"));
    }

    @Test
    @Order(13)
    void updateDiagnosis_staleVersion_conflict() throws Exception {
        var payload = Map.of("name", "Stale write", "version", 0);

        mockMvc.perform(put("/api/v1/encounters/" + encounterAUuid + "/diagnoses/" + diagnosisD1Uuid)
                        .header("Authorization", authHeader())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(payload)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message")
                        .value("This clinical record was updated by another user. Reload to continue."));
    }

    @Test
    @Order(14)
    void updateDiagnosis_jpaOptimisticLock_conflict() {
        String uuid = diagnosisD2Uuid.toString();

        assertThatThrownBy(() -> transactionTemplate.executeWithoutResult(status -> {
            Diagnosis diagnosis = diagnosisRepository.findByUuidAndDeletedAtIsNull(diagnosisD2Uuid)
                    .orElseThrow();
            jdbcTemplate.update("UPDATE diagnoses SET version = version + 5 WHERE uuid = ?::uuid", uuid);
            diagnosis.setNotes("conflicting concurrent write");
            diagnosisRepository.saveAndFlush(diagnosis);
        })).isInstanceOf(OptimisticLockingFailureException.class);
    }

    @Test
    @Order(15)
    void updateDiagnosis_missingPermission_forbidden() throws Exception {
        var payload = Map.of("name", "No permission write");

        mockMvc.perform(put("/api/v1/encounters/" + encounterAUuid + "/diagnoses/" + diagnosisD1Uuid)
                        .header("Authorization", "Bearer " + patientToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(payload)))
                .andExpect(status().isForbidden());
    }

    @Test
    @Order(16)
    void createDiagnosis_missingName_badRequest() throws Exception {
        var payload = Map.of("diagnosisType", "SECONDARY");

        mockMvc.perform(post("/api/v1/encounters/" + encounterAUuid + "/diagnoses")
                        .header("Authorization", authHeader())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(payload)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Validation failed"));
    }

    // =============================================================
    // Completed encounter is read-only
    // =============================================================

    @Test
    @Order(17)
    void createDiagnosis_inEncounterBeforeCompletion_success() throws Exception {
        var payload = Map.of(
                "name", "Hyperglycemia",
                "code", "R73.9",
                "codeSystem", "ICD-10-CM",
                "diagnosisType", "SECONDARY"
        );

        var result = mockMvc.perform(post("/api/v1/encounters/" + encounterCUuid + "/diagnoses")
                        .header("Authorization", authHeader())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(payload)))
                .andExpect(status().isCreated())
                .andReturn();
        diagnosisDcUuid = UUID.fromString(JsonPath.parse(result.getResponse().getContentAsString()).read("$.uuid"));
    }

    @Test
    @Order(18)
    void completeEncounterC_success() throws Exception {
        mockMvc.perform(post("/api/v1/encounters/" + encounterCUuid + "/complete")
                        .header("Authorization", authHeader())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("COMPLETED"));
    }

    @Test
    @Order(19)
    void createDiagnosis_completedEncounter_rejected() throws Exception {
        var payload = Map.of("name", "Should not be allowed", "diagnosisType", "SECONDARY");

        mockMvc.perform(post("/api/v1/encounters/" + encounterCUuid + "/diagnoses")
                        .header("Authorization", authHeader())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(payload)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Cannot modify diagnoses for a completed encounter"));
    }

    @Test
    @Order(20)
    void updateDiagnosis_completedEncounter_rejected() throws Exception {
        var payload = Map.of("name", "Should not be allowed");

        mockMvc.perform(put("/api/v1/encounters/" + encounterCUuid + "/diagnoses/" + diagnosisDcUuid)
                        .header("Authorization", authHeader())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(payload)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Cannot modify diagnoses for a completed encounter"));
    }

    @Test
    @Order(21)
    void deactivateDiagnosis_completedEncounter_rejected() throws Exception {
        mockMvc.perform(delete("/api/v1/encounters/" + encounterCUuid + "/diagnoses/" + diagnosisDcUuid)
                        .header("Authorization", authHeader()))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Cannot modify diagnoses for a completed encounter"));
    }

    // =============================================================
    // Deactivation (soft delete)
    // =============================================================

    @Test
    @Order(22)
    void deactivateDiagnosis_missingPermission_forbidden() throws Exception {
        mockMvc.perform(delete("/api/v1/encounters/" + encounterAUuid + "/diagnoses/" + diagnosisD1Uuid)
                        .header("Authorization", "Bearer " + patientToken))
                .andExpect(status().isForbidden());
    }

    @Test
    @Order(23)
    void deactivateDiagnosis_softDelete_success() throws Exception {
        mockMvc.perform(delete("/api/v1/encounters/" + encounterAUuid + "/diagnoses/" + diagnosisD1Uuid)
                        .header("Authorization", authHeader()))
                .andExpect(status().isNoContent());
    }

    @Test
    @Order(24)
    void deactivatedDiagnosis_excludedFromQueries() throws Exception {
        mockMvc.perform(get("/api/v1/encounters/" + encounterAUuid + "/diagnoses/" + diagnosisD1Uuid)
                        .header("Authorization", authHeader()))
                .andExpect(status().isNotFound());

        mockMvc.perform(get("/api/v1/encounters/" + encounterAUuid + "/diagnoses")
                        .header("Authorization", authHeader()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2));
    }

    @Test
    @Order(25)
    void deactivatePrimary_freesPrimarySlot() throws Exception {
        mockMvc.perform(delete("/api/v1/encounters/" + encounterAUuid + "/diagnoses/" + diagnosisD3Uuid)
                        .header("Authorization", authHeader()))
                .andExpect(status().isNoContent());

        var payload = Map.of(
                "name", "Type 2 diabetes mellitus, uncontrolled",
                "code", "E11.65",
                "codeSystem", "ICD-10-CM",
                "diagnosisType", "PRIMARY"
        );

        mockMvc.perform(post("/api/v1/encounters/" + encounterAUuid + "/diagnoses")
                        .header("Authorization", authHeader())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(payload)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.diagnosisType").value("PRIMARY"));
    }

    // =============================================================
    // Invalid / missing encounter, auth
    // =============================================================

    @Test
    @Order(26)
    void createDiagnosis_invalidEncounter_notFound() throws Exception {
        var payload = Map.of("name", "Orphan diagnosis");
        UUID missing = UUID.randomUUID();

        mockMvc.perform(post("/api/v1/encounters/" + missing + "/diagnoses")
                        .header("Authorization", authHeader())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(payload)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("Encounter not found with UUID: " + missing));
    }

    @Test
    @Order(27)
    void createDiagnosis_softDeletedEncounter_notFound() throws Exception {
        jdbcTemplate.update("UPDATE encounters SET deleted_at = now() WHERE uuid = ?::uuid",
                encounterDUuid.toString());

        var payload = Map.of("name", "Diagnosis for deleted encounter");

        mockMvc.perform(post("/api/v1/encounters/" + encounterDUuid + "/diagnoses")
                        .header("Authorization", authHeader())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(payload)))
                .andExpect(status().isNotFound());
    }

    @Test
    @Order(28)
    void diagnosisEndpoints_requireAuthentication() throws Exception {
        mockMvc.perform(get("/api/v1/encounters/" + encounterAUuid + "/diagnoses"))
                .andExpect(status().isUnauthorized());

        mockMvc.perform(post("/api/v1/encounters/" + encounterAUuid + "/diagnoses")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("name", "No auth"))))
                .andExpect(status().isUnauthorized());
    }
}
