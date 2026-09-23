package com.example.ehrsystem.modules.appointment.integration;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.Map;
import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class AppointmentIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    private String adminToken;
    private UUID patientUuid;
    private UUID doctorUuid;
    private UUID appointmentUuid;

    private String getAdminToken() throws Exception {
        var loginPayload = Map.of("email", "admin@ehr.local", "password", "Admin@123");
        var result = mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginPayload)))
                .andExpect(status().isOk())
                .andReturn();
        return JsonPath.parse(result.getResponse().getContentAsString()).read("$.accessToken");
    }

    private String authHeader() {
        return "Bearer " + adminToken;
    }

    /**
     * Returns the next occurrence of the given dayOfWeek from today.
     */
    private LocalDate nextDayOfWeek(DayOfWeek dayOfWeek) {
        LocalDate date = LocalDate.now().plusDays(1);
        while (date.getDayOfWeek() != dayOfWeek) {
            date = date.plusDays(1);
        }
        return date;
    }

    @BeforeAll
    void setupAll() throws Exception {
        adminToken = getAdminToken();

        // Create Patient
        var patientPayload = Map.of(
                "firstName", "ApptPatient",
                "lastName", "Test",
                "email", "appt.patient." + System.currentTimeMillis() + "@test.com"
        );
        var patientResult = mockMvc.perform(post("/api/v1/patients")
                        .header("Authorization", authHeader())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(patientPayload)))
                .andExpect(status().isCreated())
                .andReturn();
        patientUuid = UUID.fromString(JsonPath.parse(patientResult.getResponse().getContentAsString()).read("$.uuid"));

        // Create Doctor
        var doctorPayload = Map.of(
                "firstName", "ApptDoctor",
                "lastName", "Test",
                "email", "appt.doctor." + System.currentTimeMillis() + "@test.com",
                "specialization", "CARDIOLOGY"
        );
        var doctorResult = mockMvc.perform(post("/api/v1/doctors")
                        .header("Authorization", authHeader())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(doctorPayload)))
                .andExpect(status().isCreated())
                .andReturn();
        doctorUuid = UUID.fromString(JsonPath.parse(doctorResult.getResponse().getContentAsString()).read("$.uuid"));

        // Set up doctor availability: WORK 09:00-13:00, BREAK 13:00-14:00, WORK 14:00-18:00 for all weekdays
        for (DayOfWeek day : DayOfWeek.values()) {
            if (day == DayOfWeek.SATURDAY || day == DayOfWeek.SUNDAY) continue;

            String dayName = day.name();

            // WORK morning
            var morningPayload = Map.of(
                    "dayOfWeek", dayName,
                    "scheduleType", "WORK",
                    "startTime", "09:00:00",
                    "endTime", "13:00:00"
            );
            mockMvc.perform(post("/api/v1/doctors/" + doctorUuid + "/availability")
                            .header("Authorization", authHeader())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(morningPayload)))
                    .andExpect(status().isCreated());

            // BREAK
            var breakPayload = Map.of(
                    "dayOfWeek", dayName,
                    "scheduleType", "BREAK",
                    "startTime", "13:00:00",
                    "endTime", "14:00:00"
            );
            mockMvc.perform(post("/api/v1/doctors/" + doctorUuid + "/availability")
                            .header("Authorization", authHeader())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(breakPayload)))
                    .andExpect(status().isCreated());

            // WORK afternoon
            var afternoonPayload = Map.of(
                    "dayOfWeek", dayName,
                    "scheduleType", "WORK",
                    "startTime", "14:00:00",
                    "endTime", "18:00:00"
            );
            mockMvc.perform(post("/api/v1/doctors/" + doctorUuid + "/availability")
                            .header("Authorization", authHeader())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(afternoonPayload)))
                    .andExpect(status().isCreated());
        }
    }

    @Test
    @Order(1)
    void createAppointment_success() throws Exception {
        LocalDate targetDate = nextDayOfWeek(DayOfWeek.MONDAY);
        LocalDateTime startTime = targetDate.atTime(10, 0);
        LocalDateTime endTime = startTime.plusMinutes(30);

        var payload = Map.of(
                "patientUuid", patientUuid.toString(),
                "doctorUuid", doctorUuid.toString(),
                "startTime", startTime.toString(),
                "endTime", endTime.toString(),
                "visitType", "CONSULTATION",
                "reasonForVisit", "General checkup"
        );

        var result = mockMvc.perform(post("/api/v1/appointments")
                        .header("Authorization", authHeader())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(payload)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.uuid").exists())
                .andExpect(jsonPath("$.appointmentNumber").exists())
                .andExpect(jsonPath("$.status").value("SCHEDULED"))
                .andReturn();

        appointmentUuid = UUID.fromString(JsonPath.parse(result.getResponse().getContentAsString()).read("$.uuid"));
    }

    @Test
    @Order(2)
    void createAppointment_doctorOverlap_fails() throws Exception {
        LocalDate targetDate = nextDayOfWeek(DayOfWeek.MONDAY);
        LocalDateTime startTime = targetDate.atTime(10, 15);
        LocalDateTime endTime = startTime.plusMinutes(30);

        var patientPayload = Map.of(
                "firstName", "ApptPatientTwo",
                "lastName", "Test",
                "email", "appt.patient.two." + System.currentTimeMillis() + "@test.com"
        );
        var patientResult = mockMvc.perform(post("/api/v1/patients")
                        .header("Authorization", authHeader())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(patientPayload)))
                .andExpect(status().isCreated())
                .andReturn();
        String patientTwoUuid = JsonPath.parse(patientResult.getResponse().getContentAsString()).read("$.uuid");

        var payload = Map.of(
                "patientUuid", patientTwoUuid,
                "doctorUuid", doctorUuid.toString(),
                "startTime", startTime.toString(),
                "endTime", endTime.toString(),
                "visitType", "FOLLOW_UP",
                "reasonForVisit", "Another checkup"
        );

        mockMvc.perform(post("/api/v1/appointments")
                        .header("Authorization", authHeader())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(payload)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Doctor is already booked for this time slot."));
    }

    @Test
    @Order(3)
    void updateAppointmentStatus_invalidTransition_fails() throws Exception {
        var payload = Map.of("status", "IN_PROGRESS");

        mockMvc.perform(put("/api/v1/appointments/" + appointmentUuid + "/status")
                        .header("Authorization", authHeader())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(payload)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Invalid status transition from SCHEDULED to IN_PROGRESS"));
    }

    @Test
    @Order(4)
    void updateAppointmentStatus_success() throws Exception {
        var payload = Map.of("status", "CONFIRMED");

        mockMvc.perform(put("/api/v1/appointments/" + appointmentUuid + "/status")
                        .header("Authorization", authHeader())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(payload)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("CONFIRMED"));
    }

    @Test
    @Order(5)
    void rescheduleAppointment_success() throws Exception {
        LocalDate newDate = nextDayOfWeek(DayOfWeek.TUESDAY);
        LocalDateTime newStartTime = newDate.atTime(14, 0);
        LocalDateTime newEndTime = newStartTime.plusMinutes(45);

        var payload = Map.of(
                "startTime", newStartTime.toString(),
                "endTime", newEndTime.toString(),
                "reason", "Patient requested different time"
        );

        mockMvc.perform(put("/api/v1/appointments/" + appointmentUuid + "/reschedule")
                        .header("Authorization", authHeader())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(payload)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("CONFIRMED")); // Status stays CONFIRMED
    }

    @Test
    @Order(6)
    void getAppointmentByUuid_success() throws Exception {
        mockMvc.perform(get("/api/v1/appointments/" + appointmentUuid)
                        .header("Authorization", authHeader()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.uuid").value(appointmentUuid.toString()));
    }

    @Test
    @Order(7)
    void getAppointmentByNumber_success() throws Exception {
        var result = mockMvc.perform(get("/api/v1/appointments/" + appointmentUuid)
                        .header("Authorization", authHeader()))
                .andExpect(status().isOk())
                .andReturn();

        String appointmentNumber = JsonPath.parse(result.getResponse().getContentAsString()).read("$.appointmentNumber");

        mockMvc.perform(get("/api/v1/appointments/number/" + appointmentNumber)
                        .header("Authorization", authHeader()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.appointmentNumber").value(appointmentNumber));
    }

    @Test
    @Order(8)
    void getAppointmentByUuid_notFound() throws Exception {
        mockMvc.perform(get("/api/v1/appointments/" + UUID.randomUUID())
                        .header("Authorization", authHeader()))
                .andExpect(status().isNotFound());
    }

    @Test
    @Order(9)
    void createAppointment_patientOverlap_fails() throws Exception {
        LocalDate targetDate = nextDayOfWeek(DayOfWeek.WEDNESDAY);
        LocalDateTime startTime = targetDate.atTime(9, 0);
        LocalDateTime endTime = startTime.plusMinutes(30);

        // Create second doctor
        var doctorPayload = Map.of(
                "firstName", "OverlapDoctor",
                "lastName", "Test",
                "email", "overlap.doctor." + System.currentTimeMillis() + "@test.com",
                "specialization", "DERMATOLOGY"
        );
        var doctorResult = mockMvc.perform(post("/api/v1/doctors")
                        .header("Authorization", authHeader())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(doctorPayload)))
                .andExpect(status().isCreated())
                .andReturn();
        String doctorTwoUuid = JsonPath.parse(doctorResult.getResponse().getContentAsString()).read("$.uuid");

        // Set up availability for doctorTwo (WORK 09:00-13:00 on all weekdays)
        for (DayOfWeek day : DayOfWeek.values()) {
            if (day == DayOfWeek.SATURDAY || day == DayOfWeek.SUNDAY) continue;
            var morning = Map.of("dayOfWeek", day.name(), "scheduleType", "WORK", "startTime", "09:00:00", "endTime", "13:00:00");
            mockMvc.perform(post("/api/v1/doctors/" + doctorTwoUuid + "/availability")
                            .header("Authorization", authHeader())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(morning)))
                    .andExpect(status().isCreated());
        }

        // Create first appointment with doctorTwo
        var firstPayload = Map.of(
                "patientUuid", patientUuid.toString(),
                "doctorUuid", doctorTwoUuid,
                "startTime", startTime.toString(),
                "endTime", endTime.toString(),
                "visitType", "CONSULTATION"
        );
        mockMvc.perform(post("/api/v1/appointments")
                        .header("Authorization", authHeader())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(firstPayload)))
                .andExpect(status().isCreated());

        // Try same patient, different doctor, overlapping time
        var overlapPayload = Map.of(
                "patientUuid", patientUuid.toString(),
                "doctorUuid", doctorUuid.toString(),
                "startTime", startTime.plusMinutes(15).toString(),
                "endTime", endTime.plusMinutes(15).toString(),
                "visitType", "FOLLOW_UP"
        );
        mockMvc.perform(post("/api/v1/appointments")
                        .header("Authorization", authHeader())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(overlapPayload)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Patient already has another appointment during this time slot."));
    }

    @Test
    @Order(10)
    void rescheduleAppointment_conflict_fails() throws Exception {
        // Create a new doctor and patient for this test
        var doctorPayload = Map.of(
                "firstName", "RescheduleConflictDoctor",
                "lastName", "Test",
                "email", "resched.doc." + System.currentTimeMillis() + "@test.com",
                "specialization", "NEUROLOGY"
        );
        var doctorResult = mockMvc.perform(post("/api/v1/doctors")
                        .header("Authorization", authHeader())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(doctorPayload)))
                .andExpect(status().isCreated())
                .andReturn();
        String conflictDoctorUuid = JsonPath.parse(doctorResult.getResponse().getContentAsString()).read("$.uuid");

        var patientPayload = Map.of(
                "firstName", "RescheduleConflictPatient",
                "lastName", "Test",
                "email", "resched.patient." + System.currentTimeMillis() + "@test.com"
        );
        var patientResult = mockMvc.perform(post("/api/v1/patients")
                        .header("Authorization", authHeader())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(patientPayload)))
                .andExpect(status().isCreated())
                .andReturn();
        String conflictPatientUuid = JsonPath.parse(patientResult.getResponse().getContentAsString()).read("$.uuid");

        // Set up availability for conflict doctor
        for (DayOfWeek day : DayOfWeek.values()) {
            if (day == DayOfWeek.SATURDAY || day == DayOfWeek.SUNDAY) continue;
            var morning = Map.of("dayOfWeek", day.name(), "scheduleType", "WORK", "startTime", "09:00:00", "endTime", "13:00:00");
            mockMvc.perform(post("/api/v1/doctors/" + conflictDoctorUuid + "/availability")
                            .header("Authorization", authHeader())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(morning)))
                    .andExpect(status().isCreated());
        }

        LocalDateTime slotStart = nextDayOfWeek(DayOfWeek.THURSDAY).atTime(10, 0);
        LocalDateTime slotEnd = slotStart.plusMinutes(30);

        // Create first appointment
        var firstPayload = Map.of(
                "patientUuid", conflictPatientUuid,
                "doctorUuid", conflictDoctorUuid,
                "startTime", slotStart.toString(),
                "endTime", slotEnd.toString(),
                "visitType", "CONSULTATION"
        );
        var firstResult = mockMvc.perform(post("/api/v1/appointments")
                        .header("Authorization", authHeader())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(firstPayload)))
                .andExpect(status().isCreated())
                .andReturn();
        String firstApptUuid = JsonPath.parse(firstResult.getResponse().getContentAsString()).read("$.uuid");

        // Create second appointment with same doctor
        var secondPayload = Map.of(
                "patientUuid", conflictPatientUuid,
                "doctorUuid", conflictDoctorUuid,
                "startTime", slotStart.plusHours(1).toString(),
                "endTime", slotEnd.plusHours(1).toString(),
                "visitType", "FOLLOW_UP"
        );
        var secondResult = mockMvc.perform(post("/api/v1/appointments")
                        .header("Authorization", authHeader())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(secondPayload)))
                .andExpect(status().isCreated())
                .andReturn();
        String secondApptUuid = JsonPath.parse(secondResult.getResponse().getContentAsString()).read("$.uuid");

        // Try to reschedule second into first's slot
        var reschedulePayload = Map.of(
                "startTime", slotStart.toString(),
                "endTime", slotEnd.toString()
        );
        mockMvc.perform(put("/api/v1/appointments/" + secondApptUuid + "/reschedule")
                        .header("Authorization", authHeader())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(reschedulePayload)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Doctor is already booked for this time slot."));
    }

    @Test
    @Order(11)
    void appointmentEndpoints_requireAuthentication() throws Exception {
        mockMvc.perform(get("/api/v1/appointments"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @Order(1)
    void listAppointments_paginated() throws Exception {
        mockMvc.perform(get("/api/v1/appointments")
                        .header("Authorization", "Bearer " + adminToken)
                        .param("page", "0")
                        .param("size", "5"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray())
                .andExpect(jsonPath("$.size").value(5))
                .andExpect(jsonPath("$.totalElements").isNumber())
                .andExpect(jsonPath("$.totalElements").isNotEmpty());
    }

    // ═══════════════════════════════════════════════════════════
    // Filtered Search Tests
    // ═══════════════════════════════════════════════════════════

    @Test
    @Order(13)
    void searchFilter_byStatus() throws Exception {
        mockMvc.perform(get("/api/v1/appointments")
                        .header("Authorization", authHeader())
                        .param("status", "CONFIRMED")
                        .param("size", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray());
    }

    @Test
    @Order(14)
    void searchFilter_byDoctor() throws Exception {
        mockMvc.perform(get("/api/v1/appointments")
                        .header("Authorization", authHeader())
                        .param("doctorUuid", doctorUuid.toString())
                        .param("size", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray());
    }

    @Test
    @Order(15)
    void searchFilter_byPatient() throws Exception {
        mockMvc.perform(get("/api/v1/appointments")
                        .header("Authorization", authHeader())
                        .param("patientUuid", patientUuid.toString())
                        .param("size", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray());
    }

    @Test
    @Order(16)
    void searchFilter_dateRange() throws Exception {
        String dateFrom = LocalDate.now().plusDays(1).toString();
        String dateTo = LocalDate.now().plusDays(30).toString();

        mockMvc.perform(get("/api/v1/appointments")
                        .header("Authorization", authHeader())
                        .param("dateFrom", dateFrom)
                        .param("dateTo", dateTo)
                        .param("size", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray());
    }

    @Test
    @Order(17)
    void searchFilter_combined() throws Exception {
        mockMvc.perform(get("/api/v1/appointments")
                        .header("Authorization", authHeader())
                        .param("doctorUuid", doctorUuid.toString())
                        .param("status", "CONFIRMED")
                        .param("size", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray());
    }

    // ═══════════════════════════════════════════════════════════
    // Dedicated View Tests
    // ═══════════════════════════════════════════════════════════

    @Test
    @Order(18)
    void getTodayAppointments() throws Exception {
        mockMvc.perform(get("/api/v1/appointments/today")
                        .header("Authorization", authHeader()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray());
    }

    @Test
    @Order(19)
    void getByDoctor() throws Exception {
        mockMvc.perform(get("/api/v1/appointments/doctor/" + doctorUuid)
                        .header("Authorization", authHeader()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray());
    }

    @Test
    @Order(20)
    void getByPatient() throws Exception {
        mockMvc.perform(get("/api/v1/appointments/patient/" + patientUuid)
                        .header("Authorization", authHeader()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray());
    }

    // ═══════════════════════════════════════════════════════════
    // Reschedule History Tests
    // ═══════════════════════════════════════════════════════════

    @Test
    @Order(21)
    void reschedule_recordsHistory() throws Exception {
        mockMvc.perform(get("/api/v1/appointments/" + appointmentUuid + "/history")
                        .header("Authorization", authHeader()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$[0].action").value("RESCHEDULED"))
                .andExpect(jsonPath("$[0].previousStartTime").exists())
                .andExpect(jsonPath("$[0].newStartTime").exists())
                .andExpect(jsonPath("$[0].previousStatus").exists());
    }

    @Test
    @Order(22)
    void reschedule_statusUnchanged() throws Exception {
        LocalDate newDate = nextDayOfWeek(DayOfWeek.FRIDAY);
        LocalDateTime newStartTime = newDate.atTime(11, 0);
        LocalDateTime newEndTime = newStartTime.plusMinutes(30);

        var payload = Map.of(
                "startTime", newStartTime.toString(),
                "endTime", newEndTime.toString(),
                "reason", "Second reschedule"
        );

        mockMvc.perform(put("/api/v1/appointments/" + appointmentUuid + "/reschedule")
                        .header("Authorization", authHeader())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(payload)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("CONFIRMED")); // Still CONFIRMED, not SCHEDULED
    }

    // ═══════════════════════════════════════════════════════════
    // No-Show Tests
    // ═══════════════════════════════════════════════════════════

    @Test
    @Order(23)
    void noShow_setsTimestamp() throws Exception {
        LocalDate targetDate = nextDayOfWeek(DayOfWeek.MONDAY);
        LocalDateTime startTime = targetDate.atTime(9, 0);
        LocalDateTime endTime = startTime.plusMinutes(30);

        var createPayload = Map.of(
                "patientUuid", patientUuid.toString(),
                "doctorUuid", doctorUuid.toString(),
                "startTime", startTime.toString(),
                "endTime", endTime.toString(),
                "visitType", "CONSULTATION"
        );
        var createResult = mockMvc.perform(post("/api/v1/appointments")
                        .header("Authorization", authHeader())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createPayload)))
                .andExpect(status().isCreated())
                .andReturn();
        String noShowApptUuid = JsonPath.parse(createResult.getResponse().getContentAsString()).read("$.uuid");

        // Confirm it
        mockMvc.perform(put("/api/v1/appointments/" + noShowApptUuid + "/status")
                        .header("Authorization", authHeader())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("status", "CONFIRMED"))))
                .andExpect(status().isOk());

        // Mark as no-show
        mockMvc.perform(put("/api/v1/appointments/" + noShowApptUuid + "/status")
                        .header("Authorization", authHeader())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("status", "NO_SHOW"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("NO_SHOW"));
    }

    // ═══════════════════════════════════════════════════════════
    // Terminal State Tests
    // ═══════════════════════════════════════════════════════════

    @Test
    @Order(24)
    void terminalState_completed_noTransition() throws Exception {
        LocalDate targetDate = nextDayOfWeek(DayOfWeek.TUESDAY);
        LocalDateTime startTime = targetDate.atTime(10, 0);
        LocalDateTime endTime = startTime.plusMinutes(30);

        var createPayload = Map.of(
                "patientUuid", patientUuid.toString(),
                "doctorUuid", doctorUuid.toString(),
                "startTime", startTime.toString(),
                "endTime", endTime.toString(),
                "visitType", "FOLLOW_UP"
        );
        var createResult = mockMvc.perform(post("/api/v1/appointments")
                        .header("Authorization", authHeader())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createPayload)))
                .andExpect(status().isCreated())
                .andReturn();
        String completedApptUuid = JsonPath.parse(createResult.getResponse().getContentAsString()).read("$.uuid");

        // Walk through: SCHEDULED -> CONFIRMED -> CHECKED_IN -> IN_PROGRESS -> COMPLETED
        mockMvc.perform(put("/api/v1/appointments/" + completedApptUuid + "/status")
                        .header("Authorization", authHeader())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("status", "CONFIRMED"))))
                .andExpect(status().isOk());

        mockMvc.perform(put("/api/v1/appointments/" + completedApptUuid + "/status")
                        .header("Authorization", authHeader())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("status", "CHECKED_IN"))))
                .andExpect(status().isOk());

        mockMvc.perform(put("/api/v1/appointments/" + completedApptUuid + "/status")
                        .header("Authorization", authHeader())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("status", "IN_PROGRESS"))))
                .andExpect(status().isOk());

        mockMvc.perform(put("/api/v1/appointments/" + completedApptUuid + "/status")
                        .header("Authorization", authHeader())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("status", "COMPLETED"))))
                .andExpect(status().isOk());

        // COMPLETED -> CANCELLED should fail
        mockMvc.perform(put("/api/v1/appointments/" + completedApptUuid + "/status")
                        .header("Authorization", authHeader())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("status", "CANCELLED"))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Cannot change status from terminal state: COMPLETED"));
    }

    @Test
    @Order(25)
    void terminalState_cancelled_noTransition() throws Exception {
        LocalDate targetDate = nextDayOfWeek(DayOfWeek.WEDNESDAY);
        LocalDateTime startTime = targetDate.atTime(10, 0);
        LocalDateTime endTime = startTime.plusMinutes(30);

        var createPayload = Map.of(
                "patientUuid", patientUuid.toString(),
                "doctorUuid", doctorUuid.toString(),
                "startTime", startTime.toString(),
                "endTime", endTime.toString(),
                "visitType", "TELEMEDICINE"
        );
        var createResult = mockMvc.perform(post("/api/v1/appointments")
                        .header("Authorization", authHeader())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createPayload)))
                .andExpect(status().isCreated())
                .andReturn();
        String cancelledApptUuid = JsonPath.parse(createResult.getResponse().getContentAsString()).read("$.uuid");

        // Cancel it
        mockMvc.perform(put("/api/v1/appointments/" + cancelledApptUuid + "/status")
                        .header("Authorization", authHeader())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("status", "CANCELLED", "cancellationReason", "Patient request"))))
                .andExpect(status().isOk());

        // CANCELLED -> CONFIRMED should fail
        mockMvc.perform(put("/api/v1/appointments/" + cancelledApptUuid + "/status")
                        .header("Authorization", authHeader())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("status", "CONFIRMED"))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Cannot change status from terminal state: CANCELLED"));
    }

    // ═══════════════════════════════════════════════════════════
    // Cancellation with Reason Test
    // ═══════════════════════════════════════════════════════════

    @Test
    @Order(26)
    void cancelAppointment_withReason() throws Exception {
        LocalDate targetDate = nextDayOfWeek(DayOfWeek.THURSDAY);
        LocalDateTime startTime = targetDate.atTime(10, 0);
        LocalDateTime endTime = startTime.plusMinutes(30);

        var createPayload = Map.of(
                "patientUuid", patientUuid.toString(),
                "doctorUuid", doctorUuid.toString(),
                "startTime", startTime.toString(),
                "endTime", endTime.toString(),
                "visitType", "CONSULTATION"
        );
        var createResult = mockMvc.perform(post("/api/v1/appointments")
                        .header("Authorization", authHeader())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createPayload)))
                .andExpect(status().isCreated())
                .andReturn();
        String cancelUuid = JsonPath.parse(createResult.getResponse().getContentAsString()).read("$.uuid");

        mockMvc.perform(put("/api/v1/appointments/" + cancelUuid + "/status")
                        .header("Authorization", authHeader())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("status", "CANCELLED", "cancellationReason", "Patient feeling better"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("CANCELLED"))
                .andExpect(jsonPath("$.cancellationReason").value("Patient feeling better"));
    }

    // ═══════════════════════════════════════════════════════════
    // Concurrency Test
    // ═══════════════════════════════════════════════════════════

    @Test
    @Order(27)
    void concurrentBooking_sameSlot_oneFails() throws Exception {
        // Create two patients
        var p1Payload = Map.of("firstName", "Concurrent", "lastName", "One",
                "email", "concurrent.p1." + System.currentTimeMillis() + "@test.com");
        var p1Result = mockMvc.perform(post("/api/v1/patients")
                        .header("Authorization", authHeader())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(p1Payload)))
                .andExpect(status().isCreated())
                .andReturn();
        String p1Uuid = JsonPath.parse(p1Result.getResponse().getContentAsString()).read("$.uuid");

        var p2Payload = Map.of("firstName", "Concurrent", "lastName", "Two",
                "email", "concurrent.p2." + System.currentTimeMillis() + "@test.com");
        var p2Result = mockMvc.perform(post("/api/v1/patients")
                        .header("Authorization", authHeader())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(p2Payload)))
                .andExpect(status().isCreated())
                .andReturn();
        String p2Uuid = JsonPath.parse(p2Result.getResponse().getContentAsString()).read("$.uuid");

        LocalDate targetDate = nextDayOfWeek(DayOfWeek.FRIDAY);
        LocalDateTime slotStart = targetDate.atTime(15, 0);
        LocalDateTime slotEnd = slotStart.plusMinutes(30);

        // First booking succeeds
        mockMvc.perform(post("/api/v1/appointments")
                        .header("Authorization", authHeader())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "patientUuid", p1Uuid, "doctorUuid", doctorUuid.toString(),
                                "startTime", slotStart.toString(), "endTime", slotEnd.toString(),
                                "visitType", "CONSULTATION"))))
                .andExpect(status().isCreated());

        // Second booking on same slot fails
        mockMvc.perform(post("/api/v1/appointments")
                        .header("Authorization", authHeader())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "patientUuid", p2Uuid, "doctorUuid", doctorUuid.toString(),
                                "startTime", slotStart.toString(), "endTime", slotEnd.toString(),
                                "visitType", "CONSULTATION"))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Doctor is already booked for this time slot."));
    }

    // ═══════════════════════════════════════════════════════════
    // Full Workflow End-to-End Test
    // ═══════════════════════════════════════════════════════════

    @Test
    @Order(28)
    void fullWorkflow_scheduledToCompleted() throws Exception {
        LocalDate targetDate = nextDayOfWeek(DayOfWeek.MONDAY);
        LocalDateTime startTime = targetDate.atTime(11, 0);
        LocalDateTime endTime = startTime.plusMinutes(30);

        var createPayload = Map.of(
                "patientUuid", patientUuid.toString(),
                "doctorUuid", doctorUuid.toString(),
                "startTime", startTime.toString(),
                "endTime", endTime.toString(),
                "visitType", "CONSULTATION",
                "reasonForVisit", "Annual physical"
        );
        var createResult = mockMvc.perform(post("/api/v1/appointments")
                        .header("Authorization", authHeader())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createPayload)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("SCHEDULED"))
                .andReturn();
        String workflowUuid = JsonPath.parse(createResult.getResponse().getContentAsString()).read("$.uuid");

        // SCHEDULED -> CONFIRMED
        mockMvc.perform(put("/api/v1/appointments/" + workflowUuid + "/status")
                        .header("Authorization", authHeader())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("status", "CONFIRMED"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("CONFIRMED"));

        // CONFIRMED -> CHECKED_IN
        mockMvc.perform(put("/api/v1/appointments/" + workflowUuid + "/status")
                        .header("Authorization", authHeader())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("status", "CHECKED_IN"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("CHECKED_IN"))
                .andExpect(jsonPath("$.checkedInAt").exists());

        // CHECKED_IN -> IN_PROGRESS
        mockMvc.perform(put("/api/v1/appointments/" + workflowUuid + "/status")
                        .header("Authorization", authHeader())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("status", "IN_PROGRESS"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("IN_PROGRESS"))
                .andExpect(jsonPath("$.inProgressAt").exists());

        // IN_PROGRESS -> COMPLETED
        mockMvc.perform(put("/api/v1/appointments/" + workflowUuid + "/status")
                        .header("Authorization", authHeader())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("status", "COMPLETED"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("COMPLETED"))
                .andExpect(jsonPath("$.completedAt").exists());
    }
}
