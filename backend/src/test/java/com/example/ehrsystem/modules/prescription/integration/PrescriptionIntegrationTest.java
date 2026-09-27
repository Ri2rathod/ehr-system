package com.example.ehrsystem.modules.prescription.integration;

import com.example.ehrsystem.modules.prescription.entity.Prescription;
import com.example.ehrsystem.modules.prescription.repository.PrescriptionRepository;
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
class PrescriptionIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private PrescriptionRepository prescriptionRepository;

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

    private UUID medA; // active: Metformin Hydrochloride
    private UUID medB; // active: Atorvastatin
    private UUID medC; // deactivated: Simvastatin
    private UUID medD; // soft deleted: Omeprazole
    private String runCode; // unique per run so re-runs never collide on codes
    private String medACode;
    private String medCCode;
    private String medDCode;

    private UUID rx1; // main lifecycle in encounter A (ends COMPLETED)
    private UUID rx2; // cancel flow (stays CANCELLED)
    private UUID rx3; // void flow (ends VOID)
    private UUID rx4; // soft delete flow
    private UUID rxC; // created in encounter C before completion

    private UUID item1; // updated, ends COMPLETED
    private UUID item2; // ends CANCELLED
    private UUID item3; // ends DISCONTINUED
    private UUID item4; // on rx3
    private UUID rx4Item1;
    private UUID rx4Item2;

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
                "firstName", "PrescriptionUser",
                "lastName", "Test" + suffix,
                "email", "prescription.user." + suffix + "@example.com",
                "password", "Secret123!",
                "username", "prescriptionuser" + suffix
        );
        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(registerPayload)))
                .andExpect(status().isCreated());

        var loginPayload = Map.of(
                "email", "prescription.user." + suffix + "@example.com",
                "password", "Secret123!");
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
                "reasonForVisit", "Prescription integration test"
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

    private String createMedication(String code, String genericName) throws Exception {
        var payload = Map.of(
                "code", code,
                "codeSystem", "LOCAL",
                "genericName", genericName,
                "strength", 500,
                "strengthUnit", "MG",
                "dosageForm", "TABLET",
                "route", "ORAL"
        );
        var result = mockMvc.perform(post("/api/v1/medications")
                        .header("Authorization", authHeader())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(payload)))
                .andExpect(status().isCreated())
                .andReturn();
        return JsonPath.parse(result.getResponse().getContentAsString()).read("$.uuid");
    }

    private String createPrescription(UUID encounterUuid) throws Exception {
        var result = mockMvc.perform(post("/api/v1/encounters/" + encounterUuid + "/prescriptions")
                        .header("Authorization", authHeader())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("notes", "Generated by test"))))
                .andExpect(status().isCreated())
                .andReturn();
        return JsonPath.parse(result.getResponse().getContentAsString()).read("$.uuid");
    }

    private String createItemPayload(UUID medicationUuid) throws Exception {
        var payload = new HashMap<String, Object>();
        payload.put("medicationUuid", medicationUuid.toString());
        payload.put("dose", 500);
        payload.put("doseUnit", "MG");
        payload.put("route", "ORAL");
        payload.put("frequency", "ONCE_DAILY");
        payload.put("duration", 30);
        payload.put("durationUnit", "DAYS");
        payload.put("quantity", 30);
        payload.put("quantityUnit", "TABLETS");
        payload.put("refills", 1);
        payload.put("instructions", "Take with food");
        payload.put("startDate", "2026-10-01");
        payload.put("endDate", "2026-10-31");
        return objectMapper.writeValueAsString(payload);
    }

    @BeforeAll
    void setupAll() throws Exception {
        adminToken = getAdminToken();
        long suffix = System.currentTimeMillis();
        runCode = Long.toString(suffix);
        patientToken = registerAndLogin(Long.toString(suffix));

        var patientPayload = Map.of(
                "firstName", "PrescriptionPatient",
                "lastName", "Test",
                "gender", "MALE",
                "email", "prescription.patient." + suffix + "@test.com"
        );
        var patientResult = mockMvc.perform(post("/api/v1/patients")
                        .header("Authorization", authHeader())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(patientPayload)))
                .andExpect(status().isCreated())
                .andReturn();
        patientUuid = UUID.fromString(JsonPath.parse(patientResult.getResponse().getContentAsString()).read("$.uuid"));

        var doctorPayload = Map.of(
                "firstName", "PrescriptionDoctor",
                "lastName", "Test",
                "email", "prescription.doctor." + suffix + "@test.com",
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
        encounterAUuid = UUID.fromString(createCheckedInEncounter(monday.atTime(9, 0), "Hypertension review"));
        encounterBUuid = UUID.fromString(createCheckedInEncounter(monday.atTime(10, 0), "Follow-up visit"));
        encounterCUuid = UUID.fromString(createCheckedInEncounter(monday.atTime(11, 0), "Medication review"));
        encounterDUuid = UUID.fromString(createCheckedInEncounter(monday.atTime(12, 0), "Routine visit"));
    }

    // =============================================================
    // Medication catalog
    // =============================================================

    @Test
    @Order(1)
    void createMedication_success() throws Exception {
        medACode = "MET-" + runCode;
        medA = UUID.fromString(createMedication(medACode, "Metformin Hydrochloride"));

        mockMvc.perform(get("/api/v1/medications/" + medA)
                        .header("Authorization", authHeader()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.genericName").value("Metformin Hydrochloride"))
                .andExpect(jsonPath("$.code").value(medACode))
                .andExpect(jsonPath("$.isActive").value(true))
                .andExpect(jsonPath("$.version").value(0));
    }

    @Test
    @Order(2)
    void createMedication_duplicateCode_conflict() throws Exception {
        var payload = Map.of(
                "code", medACode,
                "codeSystem", "LOCAL",
                "genericName", "Metformin Duplicate"
        );

        mockMvc.perform(post("/api/v1/medications")
                        .header("Authorization", authHeader())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(payload)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message")
                        .value("A medication with this code already exists in the catalog."));
    }

    @Test
    @Order(3)
    void createMedication_missingGenericName_badRequest() throws Exception {
        var payload = Map.of("code", "NO-NAME");

        mockMvc.perform(post("/api/v1/medications")
                        .header("Authorization", authHeader())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(payload)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Validation failed"));
    }

    @Test
    @Order(4)
    void createMedication_invalidEnum_badRequest() throws Exception {
        var payload = Map.of("genericName", "Bad form", "dosageForm", "BOGUS_FORM");

        mockMvc.perform(post("/api/v1/medications")
                        .header("Authorization", authHeader())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(payload)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @Order(5)
    void createMedication_secondAndInactiveAndDeleted_success() throws Exception {
        medB = UUID.fromString(createMedication("ATV-" + runCode, "Atorvastatin"));
        medCCode = "SIM-" + runCode;
        medC = UUID.fromString(createMedication(medCCode, "Simvastatin"));
        medDCode = "OMP-" + runCode;
        medD = UUID.fromString(createMedication(medDCode, "Omeprazole"));
        assertThat(medA).isNotNull().isNotEqualTo(medB);
    }

    @Test
    @Order(6)
    void searchMedications_query_success() throws Exception {
        mockMvc.perform(get("/api/v1/medications?query=" + medACode)
                        .header("Authorization", authHeader()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].genericName").value("Metformin Hydrochloride"))
                .andExpect(jsonPath("$.totalElements").value(1));
    }

    @Test
    @Order(7)
    void updateMedication_success() throws Exception {
        var payload = Map.of("strength", 40, "version", 0);

        mockMvc.perform(put("/api/v1/medications/" + medB)
                        .header("Authorization", authHeader())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(payload)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.strength").value(40))
                .andExpect(jsonPath("$.version").value(1));
    }

    @Test
    @Order(8)
    void updateMedication_staleVersion_conflict() throws Exception {
        var payload = Map.of("strength", 80, "version", 0);

        mockMvc.perform(put("/api/v1/medications/" + medB)
                        .header("Authorization", authHeader())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(payload)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message")
                        .value("This clinical record was updated by another user. Reload to continue."));
    }

    @Test
    @Order(9)
    void deactivateMedication_success() throws Exception {
        var payload = Map.of("isActive", false, "version", 0);

        mockMvc.perform(put("/api/v1/medications/" + medC)
                        .header("Authorization", authHeader())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(payload)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.isActive").value(false))
                .andExpect(jsonPath("$.version").value(1));
    }

    @Test
    @Order(10)
    void searchMedications_inactiveExcludedFromActiveSearch() throws Exception {
        mockMvc.perform(get("/api/v1/medications?query=" + medCCode)
                        .header("Authorization", authHeader()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(0));

        mockMvc.perform(get("/api/v1/medications?query=" + medCCode + "&includeInactive=true")
                        .header("Authorization", authHeader()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.content[0].isActive").value(false));
    }

    @Test
    @Order(11)
    void deleteMedication_success_excludedFromQueries() throws Exception {
        mockMvc.perform(delete("/api/v1/medications/" + medD)
                        .header("Authorization", authHeader()))
                .andExpect(status().isNoContent());

        mockMvc.perform(get("/api/v1/medications/" + medD)
                        .header("Authorization", authHeader()))
                .andExpect(status().isNotFound());

        mockMvc.perform(get("/api/v1/medications?query=" + medDCode + "&includeInactive=true")
                        .header("Authorization", authHeader()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(0));
    }

    // =============================================================
    // Prescription creation and retrieval
    // =============================================================

    @Test
    @Order(12)
    void createPrescription_derivesPatientAndDoctor() throws Exception {
        var result = mockMvc.perform(post("/api/v1/encounters/" + encounterAUuid + "/prescriptions")
                        .header("Authorization", authHeader())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("notes", "Start therapy"))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("DRAFT"))
                .andExpect(jsonPath("$.encounterUuid").value(encounterAUuid.toString()))
                .andExpect(jsonPath("$.patientUuid").value(patientUuid.toString()))
                .andExpect(jsonPath("$.doctorUuid").value(doctorUuid.toString()))
                .andExpect(jsonPath("$.notes").value("Start therapy"))
                .andExpect(jsonPath("$.prescribedAt").exists())
                .andExpect(jsonPath("$.version").value(0))
                .andReturn();
        String body = result.getResponse().getContentAsString();
        rx1 = UUID.fromString(JsonPath.parse(body).read("$.uuid"));
        String number = JsonPath.parse(body).read("$.prescriptionNumber");
        assertThat(number).matches("PRES-\\d{4}-\\d{6}");
    }

    @Test
    @Order(13)
    void activatePrescription_withoutItems_badRequest() throws Exception {
        mockMvc.perform(post("/api/v1/encounters/" + encounterAUuid
                        + "/prescriptions/" + rx1 + "/activate")
                        .header("Authorization", authHeader())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message")
                        .value("Cannot activate a prescription without medication items"));
    }

    @Test
    @Order(14)
    void listPrescriptions_encounterScoped() throws Exception {
        mockMvc.perform(get("/api/v1/encounters/" + encounterAUuid + "/prescriptions")
                        .header("Authorization", authHeader()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].uuid").value(rx1.toString()));

        mockMvc.perform(get("/api/v1/encounters/" + encounterBUuid + "/prescriptions")
                        .header("Authorization", authHeader()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(0));
    }

    @Test
    @Order(15)
    void getPrescription_fromAnotherEncounter_notFound() throws Exception {
        mockMvc.perform(get("/api/v1/encounters/" + encounterBUuid
                        + "/prescriptions/" + rx1)
                        .header("Authorization", authHeader()))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message")
                        .value("Prescription not found with UUID: " + rx1));
    }

    @Test
    @Order(16)
    void updatePrescription_notes_success() throws Exception {
        var payload = Map.of("notes", "Updated notes", "version", 0);

        mockMvc.perform(put("/api/v1/encounters/" + encounterAUuid + "/prescriptions/" + rx1)
                        .header("Authorization", authHeader())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(payload)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.notes").value("Updated notes"))
                .andExpect(jsonPath("$.status").value("DRAFT"))
                .andExpect(jsonPath("$.version").value(1));
    }

    @Test
    @Order(17)
    void updatePrescription_staleVersion_conflict() throws Exception {
        var payload = Map.of("notes", "Stale write", "version", 0);

        mockMvc.perform(put("/api/v1/encounters/" + encounterAUuid + "/prescriptions/" + rx1)
                        .header("Authorization", authHeader())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(payload)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message")
                        .value("This clinical record was updated by another user. Reload to continue."));
    }

    @Test
    @Order(18)
    void voidPrescription_draft_invalidTransition() throws Exception {
        mockMvc.perform(post("/api/v1/encounters/" + encounterAUuid
                        + "/prescriptions/" + rx1 + "/void")
                        .header("Authorization", authHeader())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message")
                        .value("Invalid prescription status transition from DRAFT to VOID"));
    }

    // =============================================================
    // Prescription item creation and validation
    // =============================================================

    @Test
    @Order(19)
    void createItem_success() throws Exception {
        var result = mockMvc.perform(post("/api/v1/encounters/" + encounterAUuid
                        + "/prescriptions/" + rx1 + "/items")
                        .header("Authorization", authHeader())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(createItemPayload(medA)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.prescriptionUuid").value(rx1.toString()))
                .andExpect(jsonPath("$.medicationUuid").value(medA.toString()))
                .andExpect(jsonPath("$.medicationCode").value(medACode))
                .andExpect(jsonPath("$.medicationGenericName").value("Metformin Hydrochloride"))
                .andExpect(jsonPath("$.medicationDosageForm").value("TABLET"))
                .andExpect(jsonPath("$.dose").value(500))
                .andExpect(jsonPath("$.doseUnit").value("MG"))
                .andExpect(jsonPath("$.route").value("ORAL"))
                .andExpect(jsonPath("$.frequency").value("ONCE_DAILY"))
                .andExpect(jsonPath("$.duration").value(30))
                .andExpect(jsonPath("$.durationUnit").value("DAYS"))
                .andExpect(jsonPath("$.quantity").value(30))
                .andExpect(jsonPath("$.quantityUnit").value("TABLETS"))
                .andExpect(jsonPath("$.refills").value(1))
                .andExpect(jsonPath("$.status").value("ACTIVE"))
                .andExpect(jsonPath("$.version").value(0))
                .andReturn();
        item1 = UUID.fromString(JsonPath.parse(result.getResponse().getContentAsString()).read("$.uuid"));
    }

    @Test
    @Order(20)
    void createItem_missingDose_badRequest() throws Exception {
        var payload = new HashMap<String, Object>();
        payload.put("medicationUuid", medA.toString());
        payload.put("doseUnit", "MG");
        payload.put("route", "ORAL");
        payload.put("frequency", "ONCE_DAILY");

        mockMvc.perform(post("/api/v1/encounters/" + encounterAUuid
                        + "/prescriptions/" + rx1 + "/items")
                        .header("Authorization", authHeader())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(payload)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Validation failed"));
    }

    @Test
    @Order(21)
    void createItem_negativeDose_badRequest() throws Exception {
        var payload = new HashMap<String, Object>();
        payload.put("medicationUuid", medA.toString());
        payload.put("dose", -5);
        payload.put("doseUnit", "MG");
        payload.put("route", "ORAL");
        payload.put("frequency", "ONCE_DAILY");

        mockMvc.perform(post("/api/v1/encounters/" + encounterAUuid
                        + "/prescriptions/" + rx1 + "/items")
                        .header("Authorization", authHeader())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(payload)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Validation failed"));
    }

    @Test
    @Order(22)
    void createItem_negativeQuantity_badRequest() throws Exception {
        var payload = new HashMap<String, Object>();
        payload.put("medicationUuid", medA.toString());
        payload.put("dose", 500);
        payload.put("doseUnit", "MG");
        payload.put("route", "ORAL");
        payload.put("frequency", "ONCE_DAILY");
        payload.put("quantity", -10);
        payload.put("quantityUnit", "TABLETS");

        mockMvc.perform(post("/api/v1/encounters/" + encounterAUuid
                        + "/prescriptions/" + rx1 + "/items")
                        .header("Authorization", authHeader())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(payload)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Validation failed"));
    }

    @Test
    @Order(23)
    void createItem_negativeRefills_badRequest() throws Exception {
        var payload = new HashMap<String, Object>();
        payload.put("medicationUuid", medA.toString());
        payload.put("dose", 500);
        payload.put("doseUnit", "MG");
        payload.put("route", "ORAL");
        payload.put("frequency", "ONCE_DAILY");
        payload.put("refills", -1);

        mockMvc.perform(post("/api/v1/encounters/" + encounterAUuid
                        + "/prescriptions/" + rx1 + "/items")
                        .header("Authorization", authHeader())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(payload)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Validation failed"));
    }

    @Test
    @Order(24)
    void createItem_invalidDateRange_badRequest() throws Exception {
        var payload = new HashMap<String, Object>();
        payload.put("medicationUuid", medA.toString());
        payload.put("dose", 500);
        payload.put("doseUnit", "MG");
        payload.put("route", "ORAL");
        payload.put("frequency", "ONCE_DAILY");
        payload.put("startDate", "2026-11-01");
        payload.put("endDate", "2026-10-01");

        mockMvc.perform(post("/api/v1/encounters/" + encounterAUuid
                        + "/prescriptions/" + rx1 + "/items")
                        .header("Authorization", authHeader())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(payload)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("End date must be on or after start date"));
    }

    @Test
    @Order(25)
    void createItem_durationWithoutUnit_badRequest() throws Exception {
        var payload = new HashMap<String, Object>();
        payload.put("medicationUuid", medA.toString());
        payload.put("dose", 500);
        payload.put("doseUnit", "MG");
        payload.put("route", "ORAL");
        payload.put("frequency", "ONCE_DAILY");
        payload.put("duration", 7);

        mockMvc.perform(post("/api/v1/encounters/" + encounterAUuid
                        + "/prescriptions/" + rx1 + "/items")
                        .header("Authorization", authHeader())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(payload)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message")
                        .value("Duration unit is required when duration is provided"));
    }

    @Test
    @Order(26)
    void createItem_customFrequencyWithoutInstructions_badRequest() throws Exception {
        var payload = new HashMap<String, Object>();
        payload.put("medicationUuid", medA.toString());
        payload.put("dose", 500);
        payload.put("doseUnit", "MG");
        payload.put("route", "ORAL");
        payload.put("frequency", "CUSTOM");

        mockMvc.perform(post("/api/v1/encounters/" + encounterAUuid
                        + "/prescriptions/" + rx1 + "/items")
                        .header("Authorization", authHeader())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(payload)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message")
                        .value("Instructions are required when frequency is CUSTOM"));
    }

    @Test
    @Order(27)
    void createItem_unknownMedication_notFound() throws Exception {
        UUID missing = UUID.randomUUID();
        var payload = new HashMap<String, Object>();
        payload.put("medicationUuid", missing.toString());
        payload.put("dose", 500);
        payload.put("doseUnit", "MG");
        payload.put("route", "ORAL");
        payload.put("frequency", "ONCE_DAILY");

        mockMvc.perform(post("/api/v1/encounters/" + encounterAUuid
                        + "/prescriptions/" + rx1 + "/items")
                        .header("Authorization", authHeader())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(payload)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message")
                        .value("Medication not found with UUID: " + missing));
    }

    @Test
    @Order(28)
    void createItem_inactiveMedication_badRequest() throws Exception {
        var payload = new HashMap<String, Object>();
        payload.put("medicationUuid", medC.toString());
        payload.put("dose", 40);
        payload.put("doseUnit", "MG");
        payload.put("route", "ORAL");
        payload.put("frequency", "AT_BEDTIME");

        mockMvc.perform(post("/api/v1/encounters/" + encounterAUuid
                        + "/prescriptions/" + rx1 + "/items")
                        .header("Authorization", authHeader())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(payload)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message")
                        .value("Medication is not active for prescribing: Simvastatin"));
    }

    @Test
    @Order(29)
    void createItem_secondMedication_success() throws Exception {
        var payload = new HashMap<String, Object>();
        payload.put("medicationUuid", medB.toString());
        payload.put("dose", 20);
        payload.put("doseUnit", "MG");
        payload.put("route", "ORAL");
        payload.put("frequency", "AS_NEEDED");

        var result = mockMvc.perform(post("/api/v1/encounters/" + encounterAUuid
                        + "/prescriptions/" + rx1 + "/items")
                        .header("Authorization", authHeader())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(payload)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.medicationGenericName").value("Atorvastatin"))
                .andExpect(jsonPath("$.refills").value(0))
                .andExpect(jsonPath("$.status").value("ACTIVE"))
                .andReturn();
        item2 = UUID.fromString(JsonPath.parse(result.getResponse().getContentAsString()).read("$.uuid"));
    }

    @Test
    @Order(30)
    void listItems_success() throws Exception {
        mockMvc.perform(get("/api/v1/encounters/" + encounterAUuid
                        + "/prescriptions/" + rx1 + "/items")
                        .header("Authorization", authHeader()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].uuid").value(item1.toString()))
                .andExpect(jsonPath("$[1].uuid").value(item2.toString()));
    }

    // =============================================================
    // Item update + concurrency
    // =============================================================

    @Test
    @Order(31)
    void updateItem_doseChange_success() throws Exception {
        var payload = Map.of("dose", 850, "version", 0);

        mockMvc.perform(put("/api/v1/encounters/" + encounterAUuid
                        + "/prescriptions/" + rx1 + "/items/" + item1)
                        .header("Authorization", authHeader())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(payload)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.dose").value(850))
                .andExpect(jsonPath("$.version").value(1));
    }

    @Test
    @Order(32)
    void updateItem_staleVersion_conflict() throws Exception {
        var payload = Map.of("dose", 1000, "version", 0);

        mockMvc.perform(put("/api/v1/encounters/" + encounterAUuid
                        + "/prescriptions/" + rx1 + "/items/" + item1)
                        .header("Authorization", authHeader())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(payload)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message")
                        .value("This clinical record was updated by another user. Reload to continue."));
    }

    @Test
    @Order(33)
    void updateItem_medicationIdentityImmutable() throws Exception {
        var payload = new HashMap<String, Object>();
        payload.put("medicationUuid", medB.toString());
        payload.put("version", 1);

        mockMvc.perform(put("/api/v1/encounters/" + encounterAUuid
                        + "/prescriptions/" + rx1 + "/items/" + item1)
                        .header("Authorization", authHeader())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(payload)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.medicationUuid").value(medA.toString()))
                .andExpect(jsonPath("$.version").value(1));
    }

    // =============================================================
    // Prescription activation
    // =============================================================

    @Test
    @Order(34)
    void activatePrescription_success() throws Exception {
        mockMvc.perform(post("/api/v1/encounters/" + encounterAUuid
                        + "/prescriptions/" + rx1 + "/activate")
                        .header("Authorization", authHeader())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("ACTIVE"))
                .andExpect(jsonPath("$.version").value(2));
    }

    @Test
    @Order(35)
    void activatePrescription_idempotent() throws Exception {
        mockMvc.perform(post("/api/v1/encounters/" + encounterAUuid
                        + "/prescriptions/" + rx1 + "/activate")
                        .header("Authorization", authHeader())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("ACTIVE"))
                .andExpect(jsonPath("$.version").value(2));
    }

    @Test
    @Order(36)
    void createItem_afterActivation_allowed() throws Exception {
        var result = mockMvc.perform(post("/api/v1/encounters/" + encounterAUuid
                        + "/prescriptions/" + rx1 + "/items")
                        .header("Authorization", authHeader())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(createItemPayload(medA)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("ACTIVE"))
                .andReturn();
        item3 = UUID.fromString(JsonPath.parse(result.getResponse().getContentAsString()).read("$.uuid"));
    }

    // =============================================================
    // Item lifecycle (independent of prescription status)
    // =============================================================

    @Test
    @Order(37)
    void discontinueItem_success_prescriptionUnchanged() throws Exception {
        mockMvc.perform(post("/api/v1/encounters/" + encounterAUuid
                        + "/prescriptions/" + rx1 + "/items/" + item3 + "/discontinue")
                        .header("Authorization", authHeader())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("DISCONTINUED"))
                .andExpect(jsonPath("$.version").value(1));

        // Discontinuing one item never changes the prescription status.
        mockMvc.perform(get("/api/v1/encounters/" + encounterAUuid
                        + "/prescriptions/" + rx1)
                        .header("Authorization", authHeader()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("ACTIVE"));
    }

    @Test
    @Order(38)
    void discontinueItem_alreadyDiscontinued_badRequest() throws Exception {
        mockMvc.perform(post("/api/v1/encounters/" + encounterAUuid
                        + "/prescriptions/" + rx1 + "/items/" + item3 + "/discontinue")
                        .header("Authorization", authHeader())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message")
                        .value("Prescription item is already discontinued"));
    }

    @Test
    @Order(39)
    void completeItem_success() throws Exception {
        mockMvc.perform(post("/api/v1/encounters/" + encounterAUuid
                        + "/prescriptions/" + rx1 + "/items/" + item1 + "/complete")
                        .header("Authorization", authHeader())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("COMPLETED"))
                .andExpect(jsonPath("$.version").value(2));
    }

    @Test
    @Order(40)
    void cancelItem_completedItem_terminalConflict() throws Exception {
        mockMvc.perform(post("/api/v1/encounters/" + encounterAUuid
                        + "/prescriptions/" + rx1 + "/items/" + item1 + "/cancel")
                        .header("Authorization", authHeader())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message")
                        .value("Cannot change prescription item status from terminal state: COMPLETED"));
    }

    @Test
    @Order(41)
    void deleteItem_completedItem_rejected() throws Exception {
        mockMvc.perform(delete("/api/v1/encounters/" + encounterAUuid
                        + "/prescriptions/" + rx1 + "/items/" + item1)
                        .header("Authorization", authHeader()))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message")
                        .value("Cannot delete a completed prescription item"));
    }

    @Test
    @Order(42)
    void cancelItem_success() throws Exception {
        mockMvc.perform(post("/api/v1/encounters/" + encounterAUuid
                        + "/prescriptions/" + rx1 + "/items/" + item2 + "/cancel")
                        .header("Authorization", authHeader())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("CANCELLED"));
    }

    // =============================================================
    // Prescription lifecycle: complete
    // =============================================================

    @Test
    @Order(43)
    void completePrescription_success() throws Exception {
        mockMvc.perform(post("/api/v1/encounters/" + encounterAUuid
                        + "/prescriptions/" + rx1 + "/complete")
                        .header("Authorization", authHeader())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("COMPLETED"))
                .andExpect(jsonPath("$.version").value(3));
    }

    @Test
    @Order(44)
    void activatePrescription_completed_terminalConflict() throws Exception {
        mockMvc.perform(post("/api/v1/encounters/" + encounterAUuid
                        + "/prescriptions/" + rx1 + "/activate")
                        .header("Authorization", authHeader())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message")
                        .value("Cannot change prescription status from terminal state: COMPLETED"));
    }

    @Test
    @Order(45)
    void cancelPrescription_completed_terminalConflict() throws Exception {
        mockMvc.perform(post("/api/v1/encounters/" + encounterAUuid
                        + "/prescriptions/" + rx1 + "/cancel")
                        .header("Authorization", authHeader())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message")
                        .value("Cannot change prescription status from terminal state: COMPLETED"));
    }

    @Test
    @Order(46)
    void createItem_completedPrescription_rejected() throws Exception {
        mockMvc.perform(post("/api/v1/encounters/" + encounterAUuid
                        + "/prescriptions/" + rx1 + "/items")
                        .header("Authorization", authHeader())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(createItemPayload(medA)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message")
                        .value("Cannot modify items of a completed prescription"));
    }

    // =============================================================
    // Prescription lifecycle: cancel
    // =============================================================

    @Test
    @Order(47)
    void createPrescription_forCancel_success() throws Exception {
        rx2 = UUID.fromString(createPrescription(encounterAUuid));
        assertThat(rx2).isNotEqualTo(rx1);
    }

    @Test
    @Order(48)
    void cancelPrescription_draft_success() throws Exception {
        mockMvc.perform(post("/api/v1/encounters/" + encounterAUuid
                        + "/prescriptions/" + rx2 + "/cancel")
                        .header("Authorization", authHeader())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("CANCELLED"))
                .andExpect(jsonPath("$.version").value(1));
    }

    @Test
    @Order(49)
    void cancelPrescription_alreadyCancelled_badRequest() throws Exception {
        mockMvc.perform(post("/api/v1/encounters/" + encounterAUuid
                        + "/prescriptions/" + rx2 + "/cancel")
                        .header("Authorization", authHeader())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Prescription is already cancelled"));
    }

    @Test
    @Order(50)
    void createItem_cancelledPrescription_rejected() throws Exception {
        mockMvc.perform(post("/api/v1/encounters/" + encounterAUuid
                        + "/prescriptions/" + rx2 + "/items")
                        .header("Authorization", authHeader())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(createItemPayload(medA)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message")
                        .value("Cannot modify items of a cancelled prescription"));
    }

    @Test
    @Order(51)
    void activatePrescription_cancelled_terminalConflict() throws Exception {
        mockMvc.perform(post("/api/v1/encounters/" + encounterAUuid
                        + "/prescriptions/" + rx2 + "/activate")
                        .header("Authorization", authHeader())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message")
                        .value("Cannot change prescription status from terminal state: CANCELLED"));
    }

    // =============================================================
    // Prescription lifecycle: void
    // =============================================================

    @Test
    @Order(52)
    void createPrescription_forVoid_success() throws Exception {
        rx3 = UUID.fromString(createPrescription(encounterAUuid));
    }

    @Test
    @Order(53)
    void createItem_forVoid_success() throws Exception {
        var result = mockMvc.perform(post("/api/v1/encounters/" + encounterAUuid
                        + "/prescriptions/" + rx3 + "/items")
                        .header("Authorization", authHeader())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(createItemPayload(medA)))
                .andExpect(status().isCreated())
                .andReturn();
        item4 = UUID.fromString(JsonPath.parse(result.getResponse().getContentAsString()).read("$.uuid"));
    }

    @Test
    @Order(54)
    void activatePrescription_forVoid_success() throws Exception {
        mockMvc.perform(post("/api/v1/encounters/" + encounterAUuid
                        + "/prescriptions/" + rx3 + "/activate")
                        .header("Authorization", authHeader())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("ACTIVE"))
                .andExpect(jsonPath("$.version").value(1));
    }

    @Test
    @Order(55)
    void voidPrescription_active_success() throws Exception {
        mockMvc.perform(post("/api/v1/encounters/" + encounterAUuid
                        + "/prescriptions/" + rx3 + "/void")
                        .header("Authorization", authHeader())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("VOID"))
                .andExpect(jsonPath("$.version").value(2));
    }

    @Test
    @Order(56)
    void voidPrescription_alreadyVoided_badRequest() throws Exception {
        mockMvc.perform(post("/api/v1/encounters/" + encounterAUuid
                        + "/prescriptions/" + rx3 + "/void")
                        .header("Authorization", authHeader())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Prescription is already voided"));
    }

    @Test
    @Order(57)
    void discontinueItem_voidedPrescription_rejected() throws Exception {
        mockMvc.perform(post("/api/v1/encounters/" + encounterAUuid
                        + "/prescriptions/" + rx3 + "/items/" + item4 + "/discontinue")
                        .header("Authorization", authHeader())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message")
                        .value("Cannot modify items of a voided prescription"));
    }

    // =============================================================
    // Completed encounter is read-only
    // =============================================================

    @Test
    @Order(58)
    void createPrescription_onEncounterC_beforeCompletion_success() throws Exception {
        rxC = UUID.fromString(createPrescription(encounterCUuid));
    }

    @Test
    @Order(59)
    void completeEncounterC_success() throws Exception {
        mockMvc.perform(post("/api/v1/encounters/" + encounterCUuid + "/complete")
                        .header("Authorization", authHeader())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("COMPLETED"));
    }

    @Test
    @Order(60)
    void createPrescription_completedEncounter_rejected() throws Exception {
        mockMvc.perform(post("/api/v1/encounters/" + encounterCUuid + "/prescriptions")
                        .header("Authorization", authHeader())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("notes", "nope"))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message")
                        .value("Cannot modify prescriptions for a completed encounter"));
    }

    @Test
    @Order(61)
    void updatePrescription_completedEncounter_rejected() throws Exception {
        var payload = Map.of("notes", "nope", "version", 0);

        mockMvc.perform(put("/api/v1/encounters/" + encounterCUuid + "/prescriptions/" + rxC)
                        .header("Authorization", authHeader())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(payload)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message")
                        .value("Cannot modify prescriptions for a completed encounter"));
    }

    @Test
    @Order(62)
    void activatePrescription_completedEncounter_rejected() throws Exception {
        mockMvc.perform(post("/api/v1/encounters/" + encounterCUuid
                        + "/prescriptions/" + rxC + "/activate")
                        .header("Authorization", authHeader())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message")
                        .value("Cannot modify prescriptions for a completed encounter"));
    }

    @Test
    @Order(63)
    void deletePrescription_completedEncounter_rejected() throws Exception {
        mockMvc.perform(delete("/api/v1/encounters/" + encounterCUuid
                        + "/prescriptions/" + rxC)
                        .header("Authorization", authHeader()))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message")
                        .value("Cannot modify prescriptions for a completed encounter"));
    }

    @Test
    @Order(64)
    void createItem_completedEncounter_rejected() throws Exception {
        mockMvc.perform(post("/api/v1/encounters/" + encounterCUuid
                        + "/prescriptions/" + rxC + "/items")
                        .header("Authorization", authHeader())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(createItemPayload(medA)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message")
                        .value("Cannot modify prescription items for a completed encounter"));
    }

    // =============================================================
    // Soft delete (item + prescription cascade)
    // =============================================================

    @Test
    @Order(65)
    void createPrescription_forDeletion_success() throws Exception {
        rx4 = UUID.fromString(createPrescription(encounterAUuid));
    }

    @Test
    @Order(66)
    void createItem_firstForDeletion_success() throws Exception {
        var result = mockMvc.perform(post("/api/v1/encounters/" + encounterAUuid
                        + "/prescriptions/" + rx4 + "/items")
                        .header("Authorization", authHeader())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(createItemPayload(medA)))
                .andExpect(status().isCreated())
                .andReturn();
        rx4Item1 = UUID.fromString(JsonPath.parse(result.getResponse().getContentAsString()).read("$.uuid"));
    }

    @Test
    @Order(67)
    void createItem_secondForDeletion_success() throws Exception {
        var payload = new HashMap<String, Object>();
        payload.put("medicationUuid", medB.toString());
        payload.put("dose", 20);
        payload.put("doseUnit", "MG");
        payload.put("route", "ORAL");
        payload.put("frequency", "WEEKLY");

        var result = mockMvc.perform(post("/api/v1/encounters/" + encounterAUuid
                        + "/prescriptions/" + rx4 + "/items")
                        .header("Authorization", authHeader())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(payload)))
                .andExpect(status().isCreated())
                .andReturn();
        rx4Item2 = UUID.fromString(JsonPath.parse(result.getResponse().getContentAsString()).read("$.uuid"));
    }

    @Test
    @Order(68)
    void deleteItem_success_excludedFromQueries() throws Exception {
        mockMvc.perform(delete("/api/v1/encounters/" + encounterAUuid
                        + "/prescriptions/" + rx4 + "/items/" + rx4Item1)
                        .header("Authorization", authHeader()))
                .andExpect(status().isNoContent());

        var result = mockMvc.perform(get("/api/v1/encounters/" + encounterAUuid
                        + "/prescriptions/" + rx4 + "/items")
                        .header("Authorization", authHeader()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].uuid").value(rx4Item2.toString()))
                .andReturn();
        assertThat(result.getResponse().getContentAsString()).doesNotContain(rx4Item1.toString());
    }

    @Test
    @Order(69)
    void deletePrescription_success_cascadesItems() throws Exception {
        mockMvc.perform(delete("/api/v1/encounters/" + encounterAUuid
                        + "/prescriptions/" + rx4)
                        .header("Authorization", authHeader()))
                .andExpect(status().isNoContent());

        mockMvc.perform(get("/api/v1/encounters/" + encounterAUuid
                        + "/prescriptions/" + rx4)
                        .header("Authorization", authHeader()))
                .andExpect(status().isNotFound());

        var result = mockMvc.perform(get("/api/v1/encounters/" + encounterAUuid + "/prescriptions")
                        .header("Authorization", authHeader()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(3))
                .andReturn();
        assertThat(result.getResponse().getContentAsString()).doesNotContain(rx4.toString());

        // Items of the deleted prescription are not reachable either.
        mockMvc.perform(get("/api/v1/encounters/" + encounterAUuid
                        + "/prescriptions/" + rx4 + "/items")
                        .header("Authorization", authHeader()))
                .andExpect(status().isNotFound());
    }

    @Test
    @Order(70)
    void deletePrescription_completed_rejected() throws Exception {
        mockMvc.perform(delete("/api/v1/encounters/" + encounterAUuid
                        + "/prescriptions/" + rx1)
                        .header("Authorization", authHeader()))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message")
                        .value("Cannot delete a completed prescription"));
    }

    // =============================================================
    // Cross-parent isolation
    // =============================================================

    @Test
    @Order(71)
    void updateItem_fromAnotherPrescription_notFound() throws Exception {
        var payload = Map.of("dose", 100, "version", 3);

        mockMvc.perform(put("/api/v1/encounters/" + encounterAUuid
                        + "/prescriptions/" + rx2 + "/items/" + item1)
                        .header("Authorization", authHeader())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(payload)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message")
                        .value("Prescription item not found with UUID: " + item1));
    }

    @Test
    @Order(72)
    void getPrescription_invalidEncounter_notFound() throws Exception {
        UUID missing = UUID.randomUUID();

        mockMvc.perform(get("/api/v1/encounters/" + missing + "/prescriptions")
                        .header("Authorization", authHeader()))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("Encounter not found with UUID: " + missing));
    }

    @Test
    @Order(73)
    void createPrescription_softDeletedEncounter_notFound() throws Exception {
        jdbcTemplate.update("UPDATE encounters SET deleted_at = now() WHERE uuid = ?::uuid",
                encounterDUuid.toString());

        mockMvc.perform(post("/api/v1/encounters/" + encounterDUuid + "/prescriptions")
                        .header("Authorization", authHeader())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("notes", "orphan"))))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message")
                        .value("Encounter not found with UUID: " + encounterDUuid));
    }

    @Test
    @Order(74)
    void prescriptionNumbers_uniqueAndFormatted() throws Exception {
        var result = mockMvc.perform(get("/api/v1/encounters/" + encounterAUuid + "/prescriptions")
                        .header("Authorization", authHeader()))
                .andExpect(status().isOk())
                .andReturn();

        String body = result.getResponse().getContentAsString();
        var numbers = JsonPath.parse(body).read("$[*].prescriptionNumber", java.util.List.class);
        assertThat(numbers).hasSize(3);
        assertThat(numbers).allSatisfy(n -> assertThat((String) n).matches("PRES-\\d{4}-\\d{6}"))
                .doesNotHaveDuplicates();
    }

    @Test
    @Order(75)
    void prescription_jpaOptimisticLock_conflict() {
        String uuid = rx2.toString();

        assertThatThrownBy(() -> transactionTemplate.executeWithoutResult(status -> {
            Prescription prescription = prescriptionRepository.findByUuidAndDeletedAtIsNull(rx2)
                    .orElseThrow();
            jdbcTemplate.update("UPDATE prescriptions SET version = version + 5 WHERE uuid = ?::uuid", uuid);
            prescription.setNotes("conflicting concurrent write");
            prescriptionRepository.saveAndFlush(prescription);
        })).isInstanceOf(OptimisticLockingFailureException.class);
    }

    // =============================================================
    // Permissions and authentication
    // =============================================================

    @Test
    @Order(76)
    void prescriptionEndpoints_missingPermission_forbidden() throws Exception {
        var rxPayload = Map.of("notes", "no permission");
        mockMvc.perform(post("/api/v1/encounters/" + encounterAUuid + "/prescriptions")
                        .header("Authorization", "Bearer " + patientToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(rxPayload)))
                .andExpect(status().isForbidden());

        mockMvc.perform(post("/api/v1/encounters/" + encounterAUuid
                        + "/prescriptions/" + rx1 + "/items")
                        .header("Authorization", "Bearer " + patientToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(createItemPayload(medA)))
                .andExpect(status().isForbidden());

        mockMvc.perform(post("/api/v1/encounters/" + encounterAUuid
                        + "/prescriptions/" + rx1 + "/activate")
                        .header("Authorization", "Bearer " + patientToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isForbidden());

        mockMvc.perform(post("/api/v1/encounters/" + encounterAUuid
                        + "/prescriptions/" + rx1 + "/items/" + item2 + "/discontinue")
                        .header("Authorization", "Bearer " + patientToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isForbidden());

        mockMvc.perform(post("/api/v1/medications")
                        .header("Authorization", "Bearer " + patientToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("genericName", "No perm"))))
                .andExpect(status().isForbidden());

        mockMvc.perform(get("/api/v1/medications?query=metformin")
                        .header("Authorization", "Bearer " + patientToken))
                .andExpect(status().isForbidden());

        mockMvc.perform(delete("/api/v1/encounters/" + encounterAUuid
                        + "/prescriptions/" + rx3)
                        .header("Authorization", "Bearer " + patientToken))
                .andExpect(status().isForbidden());
    }

    @Test
    @Order(77)
    void prescriptionEndpoints_patientCanView() throws Exception {
        // RECEPTIONIST and PATIENT receive PRESCRIPTION_VIEW only.
        mockMvc.perform(get("/api/v1/encounters/" + encounterAUuid + "/prescriptions")
                        .header("Authorization", "Bearer " + patientToken))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/v1/encounters/" + encounterAUuid
                        + "/prescriptions/" + rx1 + "/items")
                        .header("Authorization", "Bearer " + patientToken))
                .andExpect(status().isOk());
    }

    @Test
    @Order(78)
    void prescriptionEndpoints_requireAuthentication() throws Exception {
        mockMvc.perform(get("/api/v1/encounters/" + encounterAUuid + "/prescriptions"))
                .andExpect(status().isUnauthorized());

        mockMvc.perform(post("/api/v1/encounters/" + encounterAUuid + "/prescriptions")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("notes", "no auth"))))
                .andExpect(status().isUnauthorized());

        mockMvc.perform(get("/api/v1/medications?query=metformin"))
                .andExpect(status().isUnauthorized());

        mockMvc.perform(post("/api/v1/encounters/" + encounterAUuid
                        + "/prescriptions/" + rx1 + "/items")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(createItemPayload(medA)))
                .andExpect(status().isUnauthorized());
    }
}
