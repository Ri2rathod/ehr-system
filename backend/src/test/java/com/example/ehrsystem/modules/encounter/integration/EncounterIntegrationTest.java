package com.example.ehrsystem.modules.encounter.integration;

import com.example.ehrsystem.modules.appointment.entity.Appointment;
import com.example.ehrsystem.modules.appointment.repository.AppointmentRepository;
import com.example.ehrsystem.modules.encounter.entity.Encounter;
import com.example.ehrsystem.modules.encounter.entity.EncounterStatus;
import com.example.ehrsystem.modules.encounter.repository.EncounterRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
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
class EncounterIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private EncounterRepository encounterRepository;

    @Autowired
    private AppointmentRepository appointmentRepository;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private TransactionTemplate transactionTemplate;

    private String adminToken;
    private UUID patientUuid;
    private UUID doctorUuid;
    private LocalDate monday;
    private LocalDate tuesday;

    private UUID encounterAUuid;
    private String encounterANumber;
    private int encounterAVersion;
    private UUID encounterCUuid;
    private UUID encounterDUuid;
    private UUID encounterFUuid;
    private UUID appointmentAUuid;

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
                "reasonForVisit", "Encounter integration test"
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

    private String createCheckedInAppointment(LocalDateTime start, String visitType) throws Exception {
        String appointmentUuid = createAppointment(start, visitType);
        transitionAppointment(appointmentUuid, "CONFIRMED");
        transitionAppointment(appointmentUuid, "CHECKED_IN");
        return appointmentUuid;
    }

    private String createEncounter(String appointmentUuid, String visitType, String chiefComplaint) throws Exception {
        var payload = new java.util.HashMap<String, Object>();
        payload.put("appointmentUuid", appointmentUuid);
        if (visitType != null) payload.put("encounterType", visitType);
        if (chiefComplaint != null) payload.put("chiefComplaint", chiefComplaint);

        var result = mockMvc.perform(post("/api/v1/encounters")
                        .header("Authorization", authHeader())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(payload)))
                .andExpect(status().isCreated())
                .andReturn();
        return result.getResponse().getContentAsString();
    }

    @BeforeAll
    void setupAll() throws Exception {
        adminToken = getAdminToken();
        long suffix = System.currentTimeMillis();

        var patientPayload = Map.of(
                "firstName", "EncounterPatient",
                "lastName", "Test",
                "gender", "MALE",
                "email", "encounter.patient." + suffix + "@test.com"
        );
        var patientResult = mockMvc.perform(post("/api/v1/patients")
                        .header("Authorization", authHeader())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(patientPayload)))
                .andExpect(status().isCreated())
                .andReturn();
        patientUuid = UUID.fromString(JsonPath.parse(patientResult.getResponse().getContentAsString()).read("$.uuid"));

        var doctorPayload = Map.of(
                "firstName", "EncounterDoctor",
                "lastName", "Test",
                "email", "encounter.doctor." + suffix + "@test.com",
                "specialization", "CARDIOLOGY"
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
            String dayName = day.name();
            for (var range : new String[][]{
                    {"WORK", "09:00:00", "13:00:00"},
                    {"BREAK", "13:00:00", "14:00:00"},
                    {"WORK", "14:00:00", "18:00:00"}}) {
                var payload = Map.of(
                        "dayOfWeek", dayName,
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
        tuesday = nextDayOfWeek(DayOfWeek.TUESDAY);
    }

    @Test
    @Order(1)
    void createEncounter_fromCheckedInAppointment_success() throws Exception {
        appointmentAUuid = UUID.fromString(createCheckedInAppointment(monday.atTime(9, 0), "CONSULTATION"));

        String body = createEncounter(appointmentAUuid.toString(), "CONSULTATION", "Chest pain on exertion");

        encounterAUuid = UUID.fromString(JsonPath.parse(body).read("$.uuid"));
        encounterANumber = JsonPath.parse(body).read("$.encounterNumber");
        encounterAVersion = JsonPath.parse(body).read("$.version");

        mockMvc.perform(get("/api/v1/encounters/" + encounterAUuid)
                        .header("Authorization", authHeader()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("IN_PROGRESS"))
                .andExpect(jsonPath("$.encounterNumber").value(encounterANumber))
                .andExpect(jsonPath("$.encounterNumber").value(
                        org.hamcrest.Matchers.matchesPattern("ENC-\\d{4}-\\d{6}")))
                .andExpect(jsonPath("$.patientUuid").value(patientUuid.toString()))
                .andExpect(jsonPath("$.doctorUuid").value(doctorUuid.toString()))
                .andExpect(jsonPath("$.appointmentUuid").value(appointmentAUuid.toString()))
                .andExpect(jsonPath("$.appointmentNumber").exists())
                .andExpect(jsonPath("$.patientMrn").exists())
                .andExpect(jsonPath("$.patientGender").exists())
                .andExpect(jsonPath("$.startedAt").exists())
                .andExpect(jsonPath("$.version").value(encounterAVersion));

        mockMvc.perform(get("/api/v1/appointments/" + appointmentAUuid)
                        .header("Authorization", authHeader()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("IN_PROGRESS"));
    }

    @Test
    @Order(2)
    void createEncounter_unknownAppointment_404() throws Exception {
        var payload = Map.of("appointmentUuid", UUID.randomUUID().toString());

        mockMvc.perform(post("/api/v1/encounters")
                        .header("Authorization", authHeader())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(payload)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value(
                        org.hamcrest.Matchers.containsString("Appointment not found with UUID")));
    }

    @Test
    @Order(3)
    void createEncounter_appointmentNotCheckedIn_400() throws Exception {
        String scheduledAppointmentUuid = createAppointment(monday.atTime(10, 0), "FOLLOW_UP");

        var payload = Map.of("appointmentUuid", scheduledAppointmentUuid);

        mockMvc.perform(post("/api/v1/encounters")
                        .header("Authorization", authHeader())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(payload)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Appointment is not eligible for encounter creation"));
    }

    @Test
    @Order(4)
    void createEncounter_duplicateAppointment_400() throws Exception {
        var payload = Map.of("appointmentUuid", appointmentAUuid.toString());

        mockMvc.perform(post("/api/v1/encounters")
                        .header("Authorization", authHeader())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(payload)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Encounter already exists for this appointment"));
    }

    @Test
    @Order(5)
    void getEncounter_byEncounterNumber_success() throws Exception {
        mockMvc.perform(get("/api/v1/encounters/number/" + encounterANumber)
                        .header("Authorization", authHeader()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.uuid").value(encounterAUuid.toString()))
                .andExpect(jsonPath("$.encounterNumber").value(encounterANumber));
    }

    @Test
    @Order(6)
    void getEncounter_unknownUuid_404() throws Exception {
        mockMvc.perform(get("/api/v1/encounters/" + UUID.randomUUID())
                        .header("Authorization", authHeader()))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value(
                        org.hamcrest.Matchers.containsString("Encounter not found with UUID")));
    }

    @Test
    @Order(7)
    void updateDocumentation_success() throws Exception {
        var payload = Map.of(
                "chiefComplaint", "Exertional chest pain",
                "historyOfPresentIllness", "45 year old male with 2 weeks of chest pain on exertion.",
                "clinicalNotes", "Patient appears stable. No acute distress.",
                "version", encounterAVersion
        );

        var result = mockMvc.perform(put("/api/v1/encounters/" + encounterAUuid)
                        .header("Authorization", authHeader())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(payload)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.chiefComplaint").value("Exertional chest pain"))
                .andExpect(jsonPath("$.clinicalNotes").value("Patient appears stable. No acute distress."))
                .andExpect(jsonPath("$.version").value(encounterAVersion + 1))
                .andReturn();

        encounterAVersion = JsonPath.parse(result.getResponse().getContentAsString()).read("$.version");
    }

    @Test
    @Order(8)
    void updateDocumentation_staleVersion_409() throws Exception {
        var payload = Map.of(
                "chiefComplaint", "Conflicting write",
                "version", 99999
        );

        mockMvc.perform(put("/api/v1/encounters/" + encounterAUuid)
                        .header("Authorization", authHeader())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(payload)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").value(
                        "Encounter has been modified by another user. Please refresh and try again."));
    }

    @Test
    @Order(9)
    void completeEncounter_success() throws Exception {
        String appointmentCUuid = createCheckedInAppointment(monday.atTime(11, 0), "CONSULTATION");
        String body = createEncounter(appointmentCUuid, "CONSULTATION", "Follow-up headache");
        encounterCUuid = UUID.fromString(JsonPath.parse(body).read("$.uuid"));

        mockMvc.perform(post("/api/v1/encounters/" + encounterCUuid + "/complete")
                        .header("Authorization", authHeader())
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("COMPLETED"))
                .andExpect(jsonPath("$.endedAt").exists());

        mockMvc.perform(get("/api/v1/appointments/" + appointmentCUuid)
                        .header("Authorization", authHeader()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("COMPLETED"));
    }

    @Test
    @Order(10)
    void completeEncounter_twice_400() throws Exception {
        mockMvc.perform(post("/api/v1/encounters/" + encounterCUuid + "/complete")
                        .header("Authorization", authHeader())
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Encounter is already completed"));
    }

    @Test
    @Order(11)
    void updateDocumentation_completedEncounter_400() throws Exception {
        var payload = Map.of("clinicalNotes", "Late edit");

        mockMvc.perform(put("/api/v1/encounters/" + encounterCUuid)
                        .header("Authorization", authHeader())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(payload)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Cannot modify completed encounter"));
    }

    @Test
    @Order(12)
    void recordVitals_completedEncounter_400() throws Exception {
        var payload = Map.of("heartRate", 70);

        mockMvc.perform(post("/api/v1/encounters/" + encounterCUuid + "/vitals")
                        .header("Authorization", authHeader())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(payload)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Cannot record vitals for a completed encounter"));
    }

    @Test
    @Order(13)
    void cancelCompletedEncounter_400() throws Exception {
        var payload = Map.of("reason", "Attempted cancel");

        mockMvc.perform(post("/api/v1/encounters/" + encounterCUuid + "/cancel")
                        .header("Authorization", authHeader())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(payload)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Cannot change status from terminal state: COMPLETED"));
    }

    @Test
    @Order(14)
    void cancelEncounter_missingReason_400() throws Exception {
        String appointmentDUuid = createCheckedInAppointment(monday.atTime(14, 0), "TELEMEDICINE");
        String body = createEncounter(appointmentDUuid, "TELEMEDICINE", "Remote review");
        encounterDUuid = UUID.fromString(JsonPath.parse(body).read("$.uuid"));

        mockMvc.perform(post("/api/v1/encounters/" + encounterDUuid + "/cancel")
                        .header("Authorization", authHeader())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of())))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Validation failed"))
                .andExpect(jsonPath("$.validationErrors.reason").value("Cancellation reason is required"));
    }

    @Test
    @Order(15)
    void cancelEncounter_success() throws Exception {
        mockMvc.perform(post("/api/v1/encounters/" + encounterDUuid + "/cancel")
                        .header("Authorization", authHeader())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("reason", "Wrong encounter opened"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("CANCELLED"))
                .andExpect(jsonPath("$.cancellationReason").value("Wrong encounter opened"));
    }

    @Test
    @Order(16)
    void completeEncounter_missingChiefComplaint_400() throws Exception {
        String appointmentEUuid = createCheckedInAppointment(monday.atTime(15, 0), "PROCEDURE");
        String body = createEncounter(appointmentEUuid, "PROCEDURE", null);
        UUID encounterEUuid = UUID.fromString(JsonPath.parse(body).read("$.uuid"));

        mockMvc.perform(post("/api/v1/encounters/" + encounterEUuid + "/complete")
                        .header("Authorization", authHeader())
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Chief complaint is required to complete the encounter"));

        mockMvc.perform(post("/api/v1/encounters/" + encounterEUuid + "/complete")
                        .header("Authorization", authHeader())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "chiefComplaint", "Wound check",
                                "assessment", "Healing well"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("COMPLETED"))
                .andExpect(jsonPath("$.assessment").value("Healing well"));
    }

    @Test
    @Order(17)
    void recordVitals_success() throws Exception {
        var payload = Map.of(
                "temperature", 36.8,
                "heartRate", 72,
                "respiratoryRate", 16,
                "systolicBp", 120,
                "diastolicBp", 80,
                "oxygenSaturation", 98.0,
                "weight", 68.0,
                "height", 170.0
        );

        mockMvc.perform(post("/api/v1/encounters/" + encounterAUuid + "/vitals")
                        .header("Authorization", authHeader())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(payload)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.uuid").exists())
                .andExpect(jsonPath("$.temperature").value(36.8))
                .andExpect(jsonPath("$.heartRate").value(72))
                .andExpect(jsonPath("$.systolicBp").value(120))
                .andExpect(jsonPath("$.diastolicBp").value(80))
                .andExpect(jsonPath("$.weight").value(68.0))
                .andExpect(jsonPath("$.bmi").value(23.5))
                .andExpect(jsonPath("$.recordedAt").exists());
    }

    @Test
    @Order(18)
    void recordVitals_invalidTemperature_400() throws Exception {
        var payload = Map.of("temperature", 99.0);

        mockMvc.perform(post("/api/v1/encounters/" + encounterAUuid + "/vitals")
                        .header("Authorization", authHeader())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(payload)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Validation failed"))
                .andExpect(jsonPath("$.validationErrors.temperature").exists());
    }

    @Test
    @Order(19)
    void recordVitals_noValues_400() throws Exception {
        mockMvc.perform(post("/api/v1/encounters/" + encounterAUuid + "/vitals")
                        .header("Authorization", authHeader())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of())))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("At least one vital value is required"));
    }

    @Test
    @Order(20)
    void getVitals_returnsRecordedEntries() throws Exception {
        mockMvc.perform(get("/api/v1/encounters/" + encounterAUuid + "/vitals")
                        .header("Authorization", authHeader()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$.length()").value(org.hamcrest.Matchers.greaterThanOrEqualTo(1)))
                .andExpect(jsonPath("$[0].heartRate").value(72));
    }

    @Test
    @Order(21)
    void searchEncounters_filtersAndPagination() throws Exception {
        var patientFiltered = mockMvc.perform(get("/api/v1/encounters")
                        .header("Authorization", authHeader())
                        .param("patientUuid", patientUuid.toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray())
                .andExpect(jsonPath("$.content.length()").value(org.hamcrest.Matchers.greaterThan(1)))
                .andReturn();

        List<Map<String, Object>> patientItems =
                JsonPath.parse(patientFiltered.getResponse().getContentAsString()).read("$.content");
        assertThat(patientItems).isNotEmpty();
        assertThat(patientItems).allSatisfy(item ->
                assertThat(item.get("patientUuid")).isEqualTo(patientUuid.toString()));

        var doctorFiltered = mockMvc.perform(get("/api/v1/encounters")
                        .header("Authorization", authHeader())
                        .param("patientUuid", patientUuid.toString())
                        .param("doctorUuid", doctorUuid.toString()))
                .andExpect(status().isOk())
                .andReturn();
        List<Map<String, Object>> doctorItems =
                JsonPath.parse(doctorFiltered.getResponse().getContentAsString()).read("$.content");
        assertThat(doctorItems).allSatisfy(item ->
                assertThat(item.get("doctorUuid")).isEqualTo(doctorUuid.toString()));

        var completedFiltered = mockMvc.perform(get("/api/v1/encounters")
                        .header("Authorization", authHeader())
                        .param("patientUuid", patientUuid.toString())
                        .param("status", "COMPLETED"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(org.hamcrest.Matchers.greaterThanOrEqualTo(2)))
                .andReturn();
        List<Map<String, Object>> completedItems =
                JsonPath.parse(completedFiltered.getResponse().getContentAsString()).read("$.content");
        assertThat(completedItems).allSatisfy(item ->
                assertThat(item.get("status")).isEqualTo("COMPLETED"));

        mockMvc.perform(get("/api/v1/encounters")
                        .header("Authorization", authHeader())
                        .param("patientUuid", patientUuid.toString())
                        .param("dateFrom", LocalDate.now().toString())
                        .param("dateTo", LocalDate.now().toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(org.hamcrest.Matchers.greaterThan(1)));

        mockMvc.perform(get("/api/v1/encounters")
                        .header("Authorization", authHeader())
                        .param("patientUuid", patientUuid.toString())
                        .param("dateFrom", LocalDate.now().plusYears(1).toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(0));

        mockMvc.perform(get("/api/v1/encounters")
                        .header("Authorization", authHeader())
                        .param("patientUuid", patientUuid.toString())
                        .param("encounterType", "CONSULTATION"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray());

        mockMvc.perform(get("/api/v1/encounters")
                        .header("Authorization", authHeader())
                        .param("patientUuid", patientUuid.toString())
                        .param("page", "0")
                        .param("size", "2"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(2))
                .andExpect(jsonPath("$.page").value(0))
                .andExpect(jsonPath("$.size").value(2))
                .andExpect(jsonPath("$.totalElements").value(org.hamcrest.Matchers.greaterThanOrEqualTo(4)));
    }

    @Test
    @Order(22)
    void searchEncounters_unknownPatient_404() throws Exception {
        mockMvc.perform(get("/api/v1/encounters")
                        .header("Authorization", authHeader())
                        .param("patientUuid", UUID.randomUUID().toString()))
                .andExpect(status().isNotFound());
    }

    @Test
    @Order(23)
    void completeEncounter_updatesAppointmentStatus() throws Exception {
        mockMvc.perform(post("/api/v1/encounters/" + encounterAUuid + "/complete")
                        .header("Authorization", authHeader())
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("COMPLETED"));

        mockMvc.perform(get("/api/v1/appointments/" + appointmentAUuid)
                        .header("Authorization", authHeader()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("COMPLETED"));
    }

    @Test
    @Order(24)
    void completeDraftEncounter_invalidTransition_400() throws Exception {
        String appointmentFUuid = createCheckedInAppointment(monday.atTime(16, 0), "WALK_IN");
        encounterFUuid = seedDraftEncounter(appointmentFUuid);

        mockMvc.perform(post("/api/v1/encounters/" + encounterFUuid + "/complete")
                        .header("Authorization", authHeader())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("chiefComplaint", "Draft complete"))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value(
                        "Invalid encounter status transition from DRAFT to COMPLETED"));
    }

    @Test
    @Order(25)
    void startDraftEncounter_success() throws Exception {
        mockMvc.perform(post("/api/v1/encounters/" + encounterFUuid + "/start")
                        .header("Authorization", authHeader())
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("IN_PROGRESS"))
                .andExpect(jsonPath("$.startedAt").exists());
    }

    @Test
    @Order(26)
    void inactivePatient_blocksEncounterCreation() throws Exception {
        String appointmentHUuid = createCheckedInAppointment(tuesday.atTime(9, 0), "CONSULTATION");
        jdbcTemplate.update("UPDATE patients SET status = 'INACTIVE' WHERE uuid = ?::uuid", patientUuid.toString());

        try {
            var payload = Map.of("appointmentUuid", appointmentHUuid);
            mockMvc.perform(post("/api/v1/encounters")
                            .header("Authorization", authHeader())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(payload)))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.message").value("Patient is not active"));
        } finally {
            jdbcTemplate.update("UPDATE patients SET status = 'ACTIVE' WHERE uuid = ?::uuid", patientUuid.toString());
        }
    }

    @Test
    @Order(27)
    void inactiveDoctor_blocksEncounterCreation() throws Exception {
        String appointmentIUuid = createCheckedInAppointment(tuesday.atTime(10, 0), "CONSULTATION");
        jdbcTemplate.update("UPDATE doctors SET status = 'INACTIVE' WHERE uuid = ?::uuid", doctorUuid.toString());

        try {
            var payload = Map.of("appointmentUuid", appointmentIUuid);
            mockMvc.perform(post("/api/v1/encounters")
                            .header("Authorization", authHeader())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(payload)))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.message").value("Doctor is not active"));
        } finally {
            jdbcTemplate.update("UPDATE doctors SET status = 'ACTIVE' WHERE uuid = ?::uuid", doctorUuid.toString());
        }
    }

    private UUID seedDraftEncounter(String appointmentUuid) {
        Appointment appointment = appointmentRepository.findByUuidAndDeletedAtIsNull(UUID.fromString(appointmentUuid))
                .orElseThrow();

        Encounter encounter = Encounter.builder()
                .patient(appointment.getPatient())
                .doctor(appointment.getDoctor())
                .appointment(appointment)
                .encounterType(appointment.getVisitType())
                .status(EncounterStatus.DRAFT)
                .build();
        encounter.setEncounterNumber("ENC-DRAFT-" + System.nanoTime());

        return encounterRepository.save(encounter).getUuid();
    }

    @Test
    @Order(28)
    void softDeletedEncounter_notReturned() throws Exception {
        mockMvc.perform(get("/api/v1/encounters/" + encounterFUuid)
                        .header("Authorization", authHeader()))
                .andExpect(status().isOk());

        jdbcTemplate.update("UPDATE encounters SET deleted_at = now() WHERE uuid = ?::uuid", encounterFUuid.toString());

        mockMvc.perform(get("/api/v1/encounters/" + encounterFUuid)
                        .header("Authorization", authHeader()))
                .andExpect(status().isNotFound());

        mockMvc.perform(get("/api/v1/encounters/number/" + encounterANumber)
                        .header("Authorization", authHeader()))
                .andExpect(status().isOk());
    }

    @Test
    @Order(29)
    void duplicateActiveEncounter_uniqueIndexViolated() {
        Appointment appointment = appointmentRepository.findByUuidAndDeletedAtIsNull(appointmentAUuid)
                .orElseThrow();

        Encounter duplicate = Encounter.builder()
                .patient(appointment.getPatient())
                .doctor(appointment.getDoctor())
                .appointment(appointment)
                .encounterType(appointment.getVisitType())
                .status(EncounterStatus.IN_PROGRESS)
                .build();
        duplicate.setEncounterNumber("ENC-DUP-" + System.nanoTime());

        assertThatThrownBy(() -> encounterRepository.save(duplicate))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    @Order(30)
    void optimisticLocking_conflictingWriteFails() {
        String uuid = encounterAUuid.toString();

        assertThatThrownBy(() -> transactionTemplate.executeWithoutResult(status -> {
            Encounter encounter = encounterRepository.findByUuidAndDeletedAtIsNull(encounterAUuid)
                    .orElseThrow();
            jdbcTemplate.update("UPDATE encounters SET version = version + 1 WHERE uuid = ?::uuid", uuid);
            encounter.setClinicalNotes("conflicting concurrent write");
            encounterRepository.saveAndFlush(encounter);
        })).isInstanceOf(OptimisticLockingFailureException.class);
    }

    @Test
    @Order(31)
    void unauthorized_withoutToken_401() throws Exception {
        mockMvc.perform(get("/api/v1/encounters"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @Order(32)
    void forbidden_userWithoutEncounterPermissions_403() throws Exception {
        long suffix = System.currentTimeMillis();
        String email = "encounter.noperm." + suffix + "@test.com";

        var registerPayload = Map.of(
                "firstName", "NoPerm",
                "lastName", "User",
                "email", email,
                "password", "Secret123!",
                "username", "encnoperm" + suffix
        );
        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(registerPayload)))
                .andExpect(status().isCreated());

        var loginPayload = Map.of("email", email, "password", "Secret123!");
        var loginResult = mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginPayload)))
                .andExpect(status().isOk())
                .andReturn();
        String token = JsonPath.parse(loginResult.getResponse().getContentAsString()).read("$.accessToken");

        mockMvc.perform(get("/api/v1/encounters")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk());

        mockMvc.perform(post("/api/v1/encounters")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                Map.of("appointmentUuid", UUID.randomUUID().toString()))))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.message").value("Access denied"));
    }
}
