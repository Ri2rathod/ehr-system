package com.example.ehrsystem.modules.treatmentplan.integration;

import com.example.ehrsystem.modules.treatmentplan.entity.TreatmentPlan;
import com.example.ehrsystem.modules.treatmentplan.repository.TreatmentPlanRepository;
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
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.hamcrest.Matchers.nullValue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class TreatmentPlanIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private TreatmentPlanRepository treatmentPlanRepository;

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

    private UUID plan1Uuid; // main lifecycle plan in encounter A (ends COMPLETED)
    private UUID plan2Uuid; // item tests plan in encounter A (stays DRAFT)
    private UUID plan3Uuid; // cancel tests plan in encounter A
    private UUID plan4Uuid; // soft delete tests plan in encounter A
    private UUID planCUuid; // plan in encounter C (before completion)

    private UUID diagnosisA1Uuid; // valid link target in encounter A
    private UUID diagnosisA2Uuid; // in encounter A, later soft deleted
    private UUID diagnosisB1Uuid; // in encounter B (cross-encounter rejection)

    private UUID item1Uuid; // linked to diagnosisA1, ends COMPLETED
    private UUID item2Uuid; // no diagnosis, ends CANCELLED
    private UUID item4Uuid; // soft delete test item on plan4

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
                "firstName", "TreatmentUser",
                "lastName", "Test" + suffix,
                "email", "treatment.user." + suffix + "@example.com",
                "password", "Secret123!",
                "username", "treatmentuser" + suffix
        );
        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(registerPayload)))
                .andExpect(status().isCreated());

        var loginPayload = Map.of("email", "treatment.user." + suffix + "@example.com", "password", "Secret123!");
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
                "reasonForVisit", "Treatment plan integration test"
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
        var payload = new HashMap<String, Object>();
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

    private String createDiagnosis(UUID encounterUuid, String name, String code) throws Exception {
        var payload = new HashMap<String, Object>();
        payload.put("name", name);
        payload.put("diagnosisType", "SECONDARY");
        if (code != null) {
            payload.put("code", code);
            payload.put("codeSystem", "ICD-10-CM");
        }
        var result = mockMvc.perform(post("/api/v1/encounters/" + encounterUuid + "/diagnoses")
                        .header("Authorization", authHeader())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(payload)))
                .andExpect(status().isCreated())
                .andReturn();
        return JsonPath.parse(result.getResponse().getContentAsString()).read("$.uuid");
    }

    @BeforeAll
    void setupAll() throws Exception {
        adminToken = getAdminToken();
        long suffix = System.currentTimeMillis();
        patientToken = registerAndLogin(Long.toString(suffix));

        var patientPayload = Map.of(
                "firstName", "TreatmentPatient",
                "lastName", "Test",
                "gender", "MALE",
                "email", "treatment.patient." + suffix + "@test.com"
        );
        var patientResult = mockMvc.perform(post("/api/v1/patients")
                        .header("Authorization", authHeader())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(patientPayload)))
                .andExpect(status().isCreated())
                .andReturn();
        patientUuid = UUID.fromString(JsonPath.parse(patientResult.getResponse().getContentAsString()).read("$.uuid"));

        var doctorPayload = Map.of(
                "firstName", "TreatmentDoctor",
                "lastName", "Test",
                "email", "treatment.doctor." + suffix + "@test.com",
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
        encounterAUuid = UUID.fromString(createCheckedInEncounter(monday.atTime(9, 0), "Diabetes management"));
        encounterBUuid = UUID.fromString(createCheckedInEncounter(monday.atTime(10, 0), "Follow-up visit"));
        encounterCUuid = UUID.fromString(createCheckedInEncounter(monday.atTime(11, 0), "Lifestyle review"));
        encounterDUuid = UUID.fromString(createCheckedInEncounter(monday.atTime(12, 0), "Routine visit"));
    }

    // =============================================================
    // Plan creation
    // =============================================================

    @Test
    @Order(1)
    void createPlan_success() throws Exception {
        var payload = Map.of(
                "title", "Diabetes management plan",
                "goals", "HbA1c below 7%",
                "instructions", "Follow the diet and exercise schedule strictly",
                "followUpInstructions", "Review in 3 months",
                "notes", "Patient motivated",
                "startDate", "2026-10-01",
                "endDate", "2027-03-31"
        );

        var result = mockMvc.perform(post("/api/v1/encounters/" + encounterAUuid + "/treatment-plans")
                        .header("Authorization", authHeader())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(payload)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.uuid").exists())
                .andExpect(jsonPath("$.encounterUuid").value(encounterAUuid.toString()))
                .andExpect(jsonPath("$.title").value("Diabetes management plan"))
                .andExpect(jsonPath("$.goals").value("HbA1c below 7%"))
                .andExpect(jsonPath("$.status").value("DRAFT"))
                .andExpect(jsonPath("$.startDate").value("2026-10-01"))
                .andExpect(jsonPath("$.endDate").value("2027-03-31"))
                .andExpect(jsonPath("$.version").value(0))
                .andReturn();
        plan1Uuid = UUID.fromString(JsonPath.parse(result.getResponse().getContentAsString()).read("$.uuid"));
    }

    @Test
    @Order(2)
    void createPlan_missingTitle_badRequest() throws Exception {
        var payload = Map.of("goals", "No title here");

        mockMvc.perform(post("/api/v1/encounters/" + encounterAUuid + "/treatment-plans")
                        .header("Authorization", authHeader())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(payload)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Validation failed"));
    }

    @Test
    @Order(3)
    void createPlan_invalidDateRange_badRequest() throws Exception {
        var payload = new HashMap<String, Object>();
        payload.put("title", "Bad dates");
        payload.put("startDate", "2026-11-01");
        payload.put("endDate", "2026-10-01");

        mockMvc.perform(post("/api/v1/encounters/" + encounterAUuid + "/treatment-plans")
                        .header("Authorization", authHeader())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(payload)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("End date must be on or after start date"));
    }

    // =============================================================
    // Plan retrieval
    // =============================================================

    @Test
    @Order(4)
    void getPlan_success() throws Exception {
        mockMvc.perform(get("/api/v1/encounters/" + encounterAUuid + "/treatment-plans/" + plan1Uuid)
                        .header("Authorization", authHeader()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.uuid").value(plan1Uuid.toString()))
                .andExpect(jsonPath("$.encounterUuid").value(encounterAUuid.toString()))
                .andExpect(jsonPath("$.title").value("Diabetes management plan"));
    }

    @Test
    @Order(5)
    void getPlan_fromAnotherEncounter_notFound() throws Exception {
        mockMvc.perform(get("/api/v1/encounters/" + encounterBUuid + "/treatment-plans/" + plan1Uuid)
                        .header("Authorization", authHeader()))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message")
                        .value("Treatment plan not found with UUID: " + plan1Uuid));
    }

    @Test
    @Order(6)
    void listPlans_encounterScoped() throws Exception {
        mockMvc.perform(get("/api/v1/encounters/" + encounterAUuid + "/treatment-plans")
                        .header("Authorization", authHeader()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].uuid").value(plan1Uuid.toString()));
    }

    // =============================================================
    // Plan update + concurrency
    // =============================================================

    @Test
    @Order(7)
    void updatePlan_success() throws Exception {
        var payload = Map.of(
                "title", "Diabetes management plan (revised)",
                "goals", "HbA1c below 6.5%",
                "version", 0
        );

        mockMvc.perform(put("/api/v1/encounters/" + encounterAUuid + "/treatment-plans/" + plan1Uuid)
                        .header("Authorization", authHeader())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(payload)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.title").value("Diabetes management plan (revised)"))
                .andExpect(jsonPath("$.goals").value("HbA1c below 6.5%"))
                .andExpect(jsonPath("$.version").value(1));
    }

    @Test
    @Order(8)
    void updatePlan_staleVersion_conflict() throws Exception {
        var payload = Map.of("title", "Stale write", "version", 0);

        mockMvc.perform(put("/api/v1/encounters/" + encounterAUuid + "/treatment-plans/" + plan1Uuid)
                        .header("Authorization", authHeader())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(payload)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message")
                        .value("This clinical record was updated by another user. Reload to continue."));
    }

    @Test
    @Order(9)
    void updatePlan_invalidDateRange_badRequest() throws Exception {
        var payload = new HashMap<String, Object>();
        payload.put("startDate", "2027-01-01");
        payload.put("endDate", "2026-06-01");
        payload.put("version", 1);

        mockMvc.perform(put("/api/v1/encounters/" + encounterAUuid + "/treatment-plans/" + plan1Uuid)
                        .header("Authorization", authHeader())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(payload)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("End date must be on or after start date"));
    }

    @Test
    @Order(10)
    void updatePlan_jpaOptimisticLock_conflict() {
        String uuid = plan1Uuid.toString();

        assertThatThrownBy(() -> transactionTemplate.executeWithoutResult(status -> {
            TreatmentPlan plan = treatmentPlanRepository.findByUuidAndDeletedAtIsNull(plan1Uuid)
                    .orElseThrow();
            jdbcTemplate.update("UPDATE treatment_plans SET version = version + 5 WHERE uuid = ?::uuid", uuid);
            plan.setGoals("conflicting concurrent write");
            treatmentPlanRepository.saveAndFlush(plan);
        })).isInstanceOf(OptimisticLockingFailureException.class);
    }

    // =============================================================
    // Plan lifecycle (explicit transitions only)
    // =============================================================

    @Test
    @Order(11)
    void activatePlan_success() throws Exception {
        mockMvc.perform(post("/api/v1/encounters/" + encounterAUuid
                        + "/treatment-plans/" + plan1Uuid + "/activate")
                        .header("Authorization", authHeader())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("ACTIVE"))
                .andExpect(jsonPath("$.version").value(2));
    }

    @Test
    @Order(12)
    void activatePlan_idempotent() throws Exception {
        mockMvc.perform(post("/api/v1/encounters/" + encounterAUuid
                        + "/treatment-plans/" + plan1Uuid + "/activate")
                        .header("Authorization", authHeader())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("ACTIVE"))
                .andExpect(jsonPath("$.version").value(2));
    }

    @Test
    @Order(13)
    void completePlan_success() throws Exception {
        mockMvc.perform(post("/api/v1/encounters/" + encounterAUuid
                        + "/treatment-plans/" + plan1Uuid + "/complete")
                        .header("Authorization", authHeader())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("COMPLETED"))
                .andExpect(jsonPath("$.version").value(3));
    }

    @Test
    @Order(14)
    void activatePlan_completedPlan_terminalConflict() throws Exception {
        mockMvc.perform(post("/api/v1/encounters/" + encounterAUuid
                        + "/treatment-plans/" + plan1Uuid + "/activate")
                        .header("Authorization", authHeader())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message")
                        .value("Cannot change treatment plan status from terminal state: COMPLETED"));
    }

    @Test
    @Order(15)
    void cancelPlan_completedPlan_terminalConflict() throws Exception {
        mockMvc.perform(post("/api/v1/encounters/" + encounterAUuid
                        + "/treatment-plans/" + plan1Uuid + "/cancel")
                        .header("Authorization", authHeader())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message")
                        .value("Cannot change treatment plan status from terminal state: COMPLETED"));
    }

    @Test
    @Order(16)
    void createPlan_forItems_success() throws Exception {
        var payload = Map.of(
                "title", "Lifestyle modification plan",
                "startDate", "2026-10-01",
                "endDate", "2027-03-31"
        );

        var result = mockMvc.perform(post("/api/v1/encounters/" + encounterAUuid + "/treatment-plans")
                        .header("Authorization", authHeader())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(payload)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("DRAFT"))
                .andReturn();
        plan2Uuid = UUID.fromString(JsonPath.parse(result.getResponse().getContentAsString()).read("$.uuid"));
    }

    @Test
    @Order(17)
    void createPlan_forCancel_success() throws Exception {
        var payload = Map.of("title", "Plan to cancel");

        var result = mockMvc.perform(post("/api/v1/encounters/" + encounterAUuid + "/treatment-plans")
                        .header("Authorization", authHeader())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(payload)))
                .andExpect(status().isCreated())
                .andReturn();
        plan3Uuid = UUID.fromString(JsonPath.parse(result.getResponse().getContentAsString()).read("$.uuid"));
    }

    @Test
    @Order(18)
    void cancelPlan_draft_success() throws Exception {
        mockMvc.perform(post("/api/v1/encounters/" + encounterAUuid
                        + "/treatment-plans/" + plan3Uuid + "/cancel")
                        .header("Authorization", authHeader())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("CANCELLED"));
    }

    @Test
    @Order(19)
    void cancelPlan_alreadyCancelled_badRequest() throws Exception {
        mockMvc.perform(post("/api/v1/encounters/" + encounterAUuid
                        + "/treatment-plans/" + plan3Uuid + "/cancel")
                        .header("Authorization", authHeader())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Treatment plan is already cancelled"));
    }

    // =============================================================
    // Completed encounter is read-only
    // =============================================================

    @Test
    @Order(20)
    void createPlan_inEncounterBeforeCompletion_success() throws Exception {
        var payload = Map.of("title", "Plan created before completion");

        var result = mockMvc.perform(post("/api/v1/encounters/" + encounterCUuid + "/treatment-plans")
                        .header("Authorization", authHeader())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(payload)))
                .andExpect(status().isCreated())
                .andReturn();
        planCUuid = UUID.fromString(JsonPath.parse(result.getResponse().getContentAsString()).read("$.uuid"));
    }

    @Test
    @Order(21)
    void completeEncounterC_success() throws Exception {
        mockMvc.perform(post("/api/v1/encounters/" + encounterCUuid + "/complete")
                        .header("Authorization", authHeader())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("COMPLETED"));
    }

    @Test
    @Order(22)
    void createPlan_completedEncounter_rejected() throws Exception {
        var payload = Map.of("title", "Should not be allowed");

        mockMvc.perform(post("/api/v1/encounters/" + encounterCUuid + "/treatment-plans")
                        .header("Authorization", authHeader())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(payload)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message")
                        .value("Cannot modify treatment plans for a completed encounter"));
    }

    @Test
    @Order(23)
    void updatePlan_completedEncounter_rejected() throws Exception {
        var payload = Map.of("title", "Should not be allowed");

        mockMvc.perform(put("/api/v1/encounters/" + encounterCUuid + "/treatment-plans/" + planCUuid)
                        .header("Authorization", authHeader())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(payload)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message")
                        .value("Cannot modify treatment plans for a completed encounter"));
    }

    @Test
    @Order(24)
    void activatePlan_completedEncounter_rejected() throws Exception {
        mockMvc.perform(post("/api/v1/encounters/" + encounterCUuid
                        + "/treatment-plans/" + planCUuid + "/activate")
                        .header("Authorization", authHeader())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message")
                        .value("Cannot modify treatment plans for a completed encounter"));
    }

    @Test
    @Order(25)
    void deletePlan_completedEncounter_rejected() throws Exception {
        mockMvc.perform(delete("/api/v1/encounters/" + encounterCUuid
                        + "/treatment-plans/" + planCUuid)
                        .header("Authorization", authHeader()))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message")
                        .value("Cannot modify treatment plans for a completed encounter"));
    }

    @Test
    @Order(26)
    void createItem_completedEncounter_rejected() throws Exception {
        var payload = Map.of("name", "Should not be allowed", "treatmentType", "DIET");

        mockMvc.perform(post("/api/v1/encounters/" + encounterCUuid
                        + "/treatment-plans/" + planCUuid + "/items")
                        .header("Authorization", authHeader())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(payload)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message")
                        .value("Cannot modify treatment items for a completed encounter"));
    }

    // =============================================================
    // Diagnosis fixtures for item linkage
    // =============================================================

    @Test
    @Order(27)
    void createDiagnosisA1_success() throws Exception {
        diagnosisA1Uuid = UUID.fromString(
                createDiagnosis(encounterAUuid, "Type 2 diabetes mellitus", "E11.9"));
        assertThat(diagnosisA1Uuid).isNotNull();
    }

    @Test
    @Order(28)
    void createDiagnosisA2_success() throws Exception {
        diagnosisA2Uuid = UUID.fromString(
                createDiagnosis(encounterAUuid, "Obesity, unspecified", "E66.9"));
        assertThat(diagnosisA2Uuid).isNotNull();
    }

    @Test
    @Order(29)
    void createDiagnosisB1_success() throws Exception {
        diagnosisB1Uuid = UUID.fromString(
                createDiagnosis(encounterBUuid, "Essential hypertension", "I10"));
        assertThat(diagnosisB1Uuid).isNotNull();
    }

    @Test
    @Order(30)
    void deleteDiagnosisA2_softDelete() throws Exception {
        mockMvc.perform(delete("/api/v1/encounters/" + encounterAUuid
                        + "/diagnoses/" + diagnosisA2Uuid)
                        .header("Authorization", authHeader()))
                .andExpect(status().isNoContent());
    }

    // =============================================================
    // Item creation and validation
    // =============================================================

    @Test
    @Order(31)
    void createItem_withDiagnosis_success() throws Exception {
        var payload = new HashMap<String, Object>();
        payload.put("diagnosisUuid", diagnosisA1Uuid.toString());
        payload.put("treatmentType", "DIET");
        payload.put("name", "Low glycemic diet");
        payload.put("description", "Avoid refined sugar");
        payload.put("frequency", "3 meals per day");
        payload.put("duration", 12);
        payload.put("durationUnit", "WEEKS");
        payload.put("priority", "HIGH");
        payload.put("startDate", "2026-10-05");
        payload.put("endDate", "2026-12-31");

        var result = mockMvc.perform(post("/api/v1/encounters/" + encounterAUuid
                        + "/treatment-plans/" + plan2Uuid + "/items")
                        .header("Authorization", authHeader())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(payload)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.treatmentPlanUuid").value(plan2Uuid.toString()))
                .andExpect(jsonPath("$.diagnosisUuid").value(diagnosisA1Uuid.toString()))
                .andExpect(jsonPath("$.diagnosisCode").value("E11.9"))
                .andExpect(jsonPath("$.diagnosisName").value("Type 2 diabetes mellitus"))
                .andExpect(jsonPath("$.treatmentType").value("DIET"))
                .andExpect(jsonPath("$.name").value("Low glycemic diet"))
                .andExpect(jsonPath("$.duration").value(12))
                .andExpect(jsonPath("$.durationUnit").value("WEEKS"))
                .andExpect(jsonPath("$.priority").value("HIGH"))
                .andExpect(jsonPath("$.status").value("PLANNED"))
                .andExpect(jsonPath("$.version").value(0))
                .andReturn();
        item1Uuid = UUID.fromString(JsonPath.parse(result.getResponse().getContentAsString()).read("$.uuid"));
    }

    @Test
    @Order(32)
    void createItem_withoutDiagnosis_success() throws Exception {
        var payload = new HashMap<String, Object>();
        payload.put("treatmentType", "EXERCISE");
        payload.put("name", "30-minute brisk walk");
        payload.put("startDate", "2026-10-01");

        var result = mockMvc.perform(post("/api/v1/encounters/" + encounterAUuid
                        + "/treatment-plans/" + plan2Uuid + "/items")
                        .header("Authorization", authHeader())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(payload)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.diagnosisUuid").value(nullValue()))
                .andExpect(jsonPath("$.priority").value("MEDIUM"))
                .andExpect(jsonPath("$.status").value("PLANNED"))
                .andReturn();
        item2Uuid = UUID.fromString(JsonPath.parse(result.getResponse().getContentAsString()).read("$.uuid"));
    }

    @Test
    @Order(33)
    void createItem_missingName_badRequest() throws Exception {
        var payload = Map.of("treatmentType", "DIET");

        mockMvc.perform(post("/api/v1/encounters/" + encounterAUuid
                        + "/treatment-plans/" + plan2Uuid + "/items")
                        .header("Authorization", authHeader())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(payload)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Validation failed"));
    }

    @Test
    @Order(34)
    void createItem_missingTreatmentType_badRequest() throws Exception {
        var payload = Map.of("name", "Missing treatment type");

        mockMvc.perform(post("/api/v1/encounters/" + encounterAUuid
                        + "/treatment-plans/" + plan2Uuid + "/items")
                        .header("Authorization", authHeader())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(payload)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Validation failed"));
    }

    @Test
    @Order(35)
    void createItem_invalidTreatmentType_badRequest() throws Exception {
        var payload = Map.of("name", "Bogus type", "treatmentType", "SOMETHING_ELSE");

        mockMvc.perform(post("/api/v1/encounters/" + encounterAUuid
                        + "/treatment-plans/" + plan2Uuid + "/items")
                        .header("Authorization", authHeader())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(payload)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @Order(36)
    void createItem_invalidDateRange_badRequest() throws Exception {
        var payload = new HashMap<String, Object>();
        payload.put("treatmentType", "MONITORING");
        payload.put("name", "Bad dates");
        payload.put("startDate", "2026-12-01");
        payload.put("endDate", "2026-11-01");

        mockMvc.perform(post("/api/v1/encounters/" + encounterAUuid
                        + "/treatment-plans/" + plan2Uuid + "/items")
                        .header("Authorization", authHeader())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(payload)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("End date must be on or after start date"));
    }

    @Test
    @Order(37)
    void createItem_beforePlanStart_badRequest() throws Exception {
        var payload = new HashMap<String, Object>();
        payload.put("treatmentType", "MONITORING");
        payload.put("name", "Before plan window");
        payload.put("startDate", "2026-09-01");

        mockMvc.perform(post("/api/v1/encounters/" + encounterAUuid
                        + "/treatment-plans/" + plan2Uuid + "/items")
                        .header("Authorization", authHeader())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(payload)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message")
                        .value("Treatment item start date cannot be before the plan start date"));
    }

    @Test
    @Order(38)
    void createItem_durationWithoutUnit_badRequest() throws Exception {
        var payload = new HashMap<String, Object>();
        payload.put("treatmentType", "MONITORING");
        payload.put("name", "Duration without unit");
        payload.put("duration", 5);

        mockMvc.perform(post("/api/v1/encounters/" + encounterAUuid
                        + "/treatment-plans/" + plan2Uuid + "/items")
                        .header("Authorization", authHeader())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(payload)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message")
                        .value("Duration unit is required when duration is provided"));
    }

    @Test
    @Order(39)
    void createItem_unitWithoutDuration_badRequest() throws Exception {
        var payload = new HashMap<String, Object>();
        payload.put("treatmentType", "MONITORING");
        payload.put("name", "Unit without duration");
        payload.put("durationUnit", "WEEKS");

        mockMvc.perform(post("/api/v1/encounters/" + encounterAUuid
                        + "/treatment-plans/" + plan2Uuid + "/items")
                        .header("Authorization", authHeader())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(payload)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message")
                        .value("Duration is required when duration unit is provided"));
    }

    @Test
    @Order(40)
    void createItem_nonexistentDiagnosis_notFound() throws Exception {
        UUID missing = UUID.randomUUID();
        var payload = new HashMap<String, Object>();
        payload.put("diagnosisUuid", missing.toString());
        payload.put("treatmentType", "MONITORING");
        payload.put("name", "Unknown diagnosis link");

        mockMvc.perform(post("/api/v1/encounters/" + encounterAUuid
                        + "/treatment-plans/" + plan2Uuid + "/items")
                        .header("Authorization", authHeader())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(payload)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("Diagnosis not found with UUID: " + missing));
    }

    @Test
    @Order(41)
    void createItem_deletedDiagnosis_notFound() throws Exception {
        var payload = new HashMap<String, Object>();
        payload.put("diagnosisUuid", diagnosisA2Uuid.toString());
        payload.put("treatmentType", "MONITORING");
        payload.put("name", "Deleted diagnosis link");

        mockMvc.perform(post("/api/v1/encounters/" + encounterAUuid
                        + "/treatment-plans/" + plan2Uuid + "/items")
                        .header("Authorization", authHeader())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(payload)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message")
                        .value("Diagnosis not found with UUID: " + diagnosisA2Uuid));
    }

    @Test
    @Order(42)
    void createItem_crossEncounterDiagnosis_rejected() throws Exception {
        var payload = new HashMap<String, Object>();
        payload.put("diagnosisUuid", diagnosisB1Uuid.toString());
        payload.put("treatmentType", "MONITORING");
        payload.put("name", "Cross-encounter link");

        mockMvc.perform(post("/api/v1/encounters/" + encounterAUuid
                        + "/treatment-plans/" + plan2Uuid + "/items")
                        .header("Authorization", authHeader())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(payload)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message")
                        .value("Diagnosis does not belong to this encounter"));
    }

    @Test
    @Order(43)
    void createItem_planNotFound_notFound() throws Exception {
        UUID missingPlan = UUID.randomUUID();
        var payload = Map.of("name", "Orphan item", "treatmentType", "DIET");

        mockMvc.perform(post("/api/v1/encounters/" + encounterAUuid
                        + "/treatment-plans/" + missingPlan + "/items")
                        .header("Authorization", authHeader())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(payload)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message")
                        .value("Treatment plan not found with UUID: " + missingPlan));
    }

    @Test
    @Order(44)
    void createItem_completedPlan_rejected() throws Exception {
        var payload = Map.of("name", "Item on completed plan", "treatmentType", "DIET");

        mockMvc.perform(post("/api/v1/encounters/" + encounterAUuid
                        + "/treatment-plans/" + plan1Uuid + "/items")
                        .header("Authorization", authHeader())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(payload)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message")
                        .value("Cannot modify items of a completed treatment plan"));
    }

    // =============================================================
    // Item retrieval
    // =============================================================

    @Test
    @Order(45)
    void listItems_success() throws Exception {
        var result = mockMvc.perform(get("/api/v1/encounters/" + encounterAUuid
                        + "/treatment-plans/" + plan2Uuid + "/items")
                        .header("Authorization", authHeader()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].uuid").value(item1Uuid.toString()))
                .andExpect(jsonPath("$[0].diagnosisCode").value("E11.9"))
                .andExpect(jsonPath("$[1].uuid").value(item2Uuid.toString()))
                .andReturn();

        String body = result.getResponse().getContentAsString();
        assertThat(body).doesNotContain(diagnosisB1Uuid.toString());
    }

    @Test
    @Order(46)
    void listItems_planNotFound_notFound() throws Exception {
        UUID missingPlan = UUID.randomUUID();

        mockMvc.perform(get("/api/v1/encounters/" + encounterAUuid
                        + "/treatment-plans/" + missingPlan + "/items")
                        .header("Authorization", authHeader()))
                .andExpect(status().isNotFound());
    }

    // =============================================================
    // Item update + concurrency
    // =============================================================

    @Test
    @Order(47)
    void updateItem_success() throws Exception {
        var payload = new HashMap<String, Object>();
        payload.put("priority", "LOW");
        payload.put("frequency", "3 main meals, no snacks");
        payload.put("version", 0);

        mockMvc.perform(put("/api/v1/encounters/" + encounterAUuid
                        + "/treatment-plans/" + plan2Uuid + "/items/" + item1Uuid)
                        .header("Authorization", authHeader())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(payload)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.priority").value("LOW"))
                .andExpect(jsonPath("$.frequency").value("3 main meals, no snacks"))
                .andExpect(jsonPath("$.version").value(1));
    }

    @Test
    @Order(48)
    void updateItem_staleVersion_conflict() throws Exception {
        var payload = new HashMap<String, Object>();
        payload.put("name", "Stale write");
        payload.put("version", 0);

        mockMvc.perform(put("/api/v1/encounters/" + encounterAUuid
                        + "/treatment-plans/" + plan2Uuid + "/items/" + item1Uuid)
                        .header("Authorization", authHeader())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(payload)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message")
                        .value("This clinical record was updated by another user. Reload to continue."));
    }

    @Test
    @Order(49)
    void updateItem_invalidDateRange_badRequest() throws Exception {
        var payload = new HashMap<String, Object>();
        payload.put("startDate", "2026-12-01");
        payload.put("endDate", "2026-11-01");
        payload.put("version", 1);

        mockMvc.perform(put("/api/v1/encounters/" + encounterAUuid
                        + "/treatment-plans/" + plan2Uuid + "/items/" + item1Uuid)
                        .header("Authorization", authHeader())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(payload)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("End date must be on or after start date"));
    }

    @Test
    @Order(50)
    void updateItem_beforePlanStart_badRequest() throws Exception {
        var payload = new HashMap<String, Object>();
        payload.put("startDate", "2026-09-05");
        payload.put("version", 1);

        mockMvc.perform(put("/api/v1/encounters/" + encounterAUuid
                        + "/treatment-plans/" + plan2Uuid + "/items/" + item1Uuid)
                        .header("Authorization", authHeader())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(payload)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message")
                        .value("Treatment item start date cannot be before the plan start date"));
    }

    @Test
    @Order(51)
    void updateItem_fromAnotherPlan_notFound() throws Exception {
        var payload = new HashMap<String, Object>();
        payload.put("name", "Wrong plan");
        payload.put("version", 0);

        mockMvc.perform(put("/api/v1/encounters/" + encounterAUuid
                        + "/treatment-plans/" + plan3Uuid + "/items/" + item2Uuid)
                        .header("Authorization", authHeader())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(payload)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message")
                        .value("Treatment item not found with UUID: " + item2Uuid));
    }

    @Test
    @Order(52)
    void updateItem_unlinkDiagnosis_success() throws Exception {
        var payload = new HashMap<String, Object>();
        payload.put("unlinkDiagnosis", true);
        payload.put("version", 1);

        mockMvc.perform(put("/api/v1/encounters/" + encounterAUuid
                        + "/treatment-plans/" + plan2Uuid + "/items/" + item1Uuid)
                        .header("Authorization", authHeader())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(payload)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.diagnosisUuid").value(nullValue()))
                .andExpect(jsonPath("$.version").value(2));
    }

    // =============================================================
    // Item lifecycle (explicit transitions only)
    // =============================================================

    @Test
    @Order(53)
    void startItem_success() throws Exception {
        mockMvc.perform(post("/api/v1/encounters/" + encounterAUuid
                        + "/treatment-plans/" + plan2Uuid + "/items/" + item1Uuid + "/start")
                        .header("Authorization", authHeader())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("IN_PROGRESS"))
                .andExpect(jsonPath("$.version").value(3));
    }

    @Test
    @Order(54)
    void startItem_idempotent() throws Exception {
        mockMvc.perform(post("/api/v1/encounters/" + encounterAUuid
                        + "/treatment-plans/" + plan2Uuid + "/items/" + item1Uuid + "/start")
                        .header("Authorization", authHeader())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("IN_PROGRESS"))
                .andExpect(jsonPath("$.version").value(3));
    }

    @Test
    @Order(55)
    void completeItem_success() throws Exception {
        mockMvc.perform(post("/api/v1/encounters/" + encounterAUuid
                        + "/treatment-plans/" + plan2Uuid + "/items/" + item1Uuid + "/complete")
                        .header("Authorization", authHeader())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("COMPLETED"))
                .andExpect(jsonPath("$.version").value(4));
    }

    @Test
    @Order(56)
    void cancelItem_completedItem_terminalConflict() throws Exception {
        mockMvc.perform(post("/api/v1/encounters/" + encounterAUuid
                        + "/treatment-plans/" + plan2Uuid + "/items/" + item1Uuid + "/cancel")
                        .header("Authorization", authHeader())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message")
                        .value("Cannot change treatment item status from terminal state: COMPLETED"));
    }

    @Test
    @Order(57)
    void completeItem_plannedItem_invalidTransition() throws Exception {
        mockMvc.perform(post("/api/v1/encounters/" + encounterAUuid
                        + "/treatment-plans/" + plan2Uuid + "/items/" + item2Uuid + "/complete")
                        .header("Authorization", authHeader())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message")
                        .value("Invalid treatment item status transition from PLANNED to COMPLETED"));
    }

    @Test
    @Order(58)
    void cancelItem_success() throws Exception {
        mockMvc.perform(post("/api/v1/encounters/" + encounterAUuid
                        + "/treatment-plans/" + plan2Uuid + "/items/" + item2Uuid + "/cancel")
                        .header("Authorization", authHeader())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("CANCELLED"));
    }

    @Test
    @Order(59)
    void startItem_cancelledItem_terminalConflict() throws Exception {
        mockMvc.perform(post("/api/v1/encounters/" + encounterAUuid
                        + "/treatment-plans/" + plan2Uuid + "/items/" + item2Uuid + "/start")
                        .header("Authorization", authHeader())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message")
                        .value("Cannot change treatment item status from terminal state: CANCELLED"));
    }

    @Test
    @Order(60)
    void deleteItem_completedItem_rejected() throws Exception {
        mockMvc.perform(delete("/api/v1/encounters/" + encounterAUuid
                        + "/treatment-plans/" + plan2Uuid + "/items/" + item1Uuid)
                        .header("Authorization", authHeader()))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message")
                        .value("Cannot delete a completed treatment item"));
    }

    // =============================================================
    // Soft delete (plan + items)
    // =============================================================

    @Test
    @Order(61)
    void createPlan_forDeletion_success() throws Exception {
        var payload = Map.of("title", "Plan to be deleted");

        var result = mockMvc.perform(post("/api/v1/encounters/" + encounterAUuid + "/treatment-plans")
                        .header("Authorization", authHeader())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(payload)))
                .andExpect(status().isCreated())
                .andReturn();
        plan4Uuid = UUID.fromString(JsonPath.parse(result.getResponse().getContentAsString()).read("$.uuid"));
    }

    @Test
    @Order(62)
    void createItem_onDeletionPlan_success() throws Exception {
        var payload = Map.of("name", "Item to be deleted", "treatmentType", "EDUCATION");

        var result = mockMvc.perform(post("/api/v1/encounters/" + encounterAUuid
                        + "/treatment-plans/" + plan4Uuid + "/items")
                        .header("Authorization", authHeader())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(payload)))
                .andExpect(status().isCreated())
                .andReturn();
        item4Uuid = UUID.fromString(JsonPath.parse(result.getResponse().getContentAsString()).read("$.uuid"));
    }

    @Test
    @Order(63)
    void deleteItem_success() throws Exception {
        mockMvc.perform(delete("/api/v1/encounters/" + encounterAUuid
                        + "/treatment-plans/" + plan4Uuid + "/items/" + item4Uuid)
                        .header("Authorization", authHeader()))
                .andExpect(status().isNoContent());
    }

    @Test
    @Order(64)
    void deletedItem_excludedFromQueries() throws Exception {
        mockMvc.perform(get("/api/v1/encounters/" + encounterAUuid
                        + "/treatment-plans/" + plan4Uuid + "/items")
                        .header("Authorization", authHeader()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(0));
    }

    @Test
    @Order(65)
    void deletePlan_success() throws Exception {
        mockMvc.perform(delete("/api/v1/encounters/" + encounterAUuid
                        + "/treatment-plans/" + plan4Uuid)
                        .header("Authorization", authHeader()))
                .andExpect(status().isNoContent());
    }

    @Test
    @Order(66)
    void deletedPlan_excludedFromQueries() throws Exception {
        mockMvc.perform(get("/api/v1/encounters/" + encounterAUuid
                        + "/treatment-plans/" + plan4Uuid)
                        .header("Authorization", authHeader()))
                .andExpect(status().isNotFound());

        var result = mockMvc.perform(get("/api/v1/encounters/" + encounterAUuid + "/treatment-plans")
                        .header("Authorization", authHeader()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(3))
                .andReturn();

        assertThat(result.getResponse().getContentAsString()).doesNotContain(plan4Uuid.toString());
    }

    @Test
    @Order(67)
    void deletePlan_completedPlan_rejected() throws Exception {
        mockMvc.perform(delete("/api/v1/encounters/" + encounterAUuid
                        + "/treatment-plans/" + plan1Uuid)
                        .header("Authorization", authHeader()))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message")
                        .value("Cannot delete a completed treatment plan"));
    }

    // =============================================================
    // Permissions and authentication
    // =============================================================

    @Test
    @Order(68)
    void treatmentEndpoints_missingPermission_forbidden() throws Exception {
        var planPayload = Map.of("title", "No permission plan");
        mockMvc.perform(post("/api/v1/encounters/" + encounterAUuid + "/treatment-plans")
                        .header("Authorization", "Bearer " + patientToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(planPayload)))
                .andExpect(status().isForbidden());

        var itemPayload = Map.of("name", "No permission item", "treatmentType", "DIET");
        mockMvc.perform(post("/api/v1/encounters/" + encounterAUuid
                        + "/treatment-plans/" + plan2Uuid + "/items")
                        .header("Authorization", "Bearer " + patientToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(itemPayload)))
                .andExpect(status().isForbidden());

        mockMvc.perform(post("/api/v1/encounters/" + encounterAUuid
                        + "/treatment-plans/" + plan2Uuid + "/activate")
                        .header("Authorization", "Bearer " + patientToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isForbidden());

        mockMvc.perform(post("/api/v1/encounters/" + encounterAUuid
                        + "/treatment-plans/" + plan2Uuid + "/items/" + item1Uuid + "/start")
                        .header("Authorization", "Bearer " + patientToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isForbidden());

        mockMvc.perform(delete("/api/v1/encounters/" + encounterAUuid
                        + "/treatment-plans/" + plan1Uuid)
                        .header("Authorization", "Bearer " + patientToken))
                .andExpect(status().isForbidden());
    }

    @Test
    @Order(69)
    void treatmentEndpoints_requireAuthentication() throws Exception {
        mockMvc.perform(get("/api/v1/encounters/" + encounterAUuid + "/treatment-plans"))
                .andExpect(status().isUnauthorized());

        mockMvc.perform(get("/api/v1/encounters/" + encounterAUuid
                        + "/treatment-plans/" + plan2Uuid + "/items"))
                .andExpect(status().isUnauthorized());

        mockMvc.perform(post("/api/v1/encounters/" + encounterAUuid + "/treatment-plans")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("title", "No auth"))))
                .andExpect(status().isUnauthorized());
    }

    // =============================================================
    // Invalid / missing encounter
    // =============================================================

    @Test
    @Order(70)
    void createPlan_invalidEncounter_notFound() throws Exception {
        UUID missing = UUID.randomUUID();
        var payload = Map.of("title", "Orphan plan");

        mockMvc.perform(post("/api/v1/encounters/" + missing + "/treatment-plans")
                        .header("Authorization", authHeader())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(payload)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("Encounter not found with UUID: " + missing));
    }

    @Test
    @Order(71)
    void createPlan_softDeletedEncounter_notFound() throws Exception {
        jdbcTemplate.update("UPDATE encounters SET deleted_at = now() WHERE uuid = ?::uuid",
                encounterDUuid.toString());

        var payload = Map.of("title", "Plan for deleted encounter");

        mockMvc.perform(post("/api/v1/encounters/" + encounterDUuid + "/treatment-plans")
                        .header("Authorization", authHeader())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(payload)))
                .andExpect(status().isNotFound());
    }
}
