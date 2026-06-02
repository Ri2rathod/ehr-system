package com.example.ehrsystem.modules.appointment.integration;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
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
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(patientPayload)))
                .andExpect(status().isCreated())
                .andReturn();
        String patientUuidStr = JsonPath.parse(patientResult.getResponse().getContentAsString()).read("$.uuid");
        patientUuid = UUID.fromString(patientUuidStr);

        // Create Doctor
        var doctorPayload = Map.of(
                "firstName", "ApptDoctor",
                "lastName", "Test",
                "email", "appt.doctor." + System.currentTimeMillis() + "@test.com",
                "specialization", "CARDIOLOGY"
        );
        var doctorResult = mockMvc.perform(post("/api/v1/doctors")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(doctorPayload)))
                .andExpect(status().isCreated())
                .andReturn();
        String doctorUuidStr = JsonPath.parse(doctorResult.getResponse().getContentAsString()).read("$.uuid");
        doctorUuid = UUID.fromString(doctorUuidStr);
    }

    @Test
    @Order(1)
    void createAppointment_success() throws Exception {
        LocalDateTime startTime = LocalDateTime.now().plusDays(2).withHour(10).withMinute(0).withSecond(0).withNano(0);
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
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(payload)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.uuid").exists())
                .andExpect(jsonPath("$.appointmentNumber").exists())
                .andExpect(jsonPath("$.status").value("SCHEDULED"))
                .andReturn();

        String apptUuidStr = JsonPath.parse(result.getResponse().getContentAsString()).read("$.uuid");
        appointmentUuid = UUID.fromString(apptUuidStr);
    }

    @Test
    @Order(2)
    void createAppointment_doctorOverlap_fails() throws Exception {
        LocalDateTime startTime = LocalDateTime.now().plusDays(2).withHour(10).withMinute(15).withSecond(0).withNano(0);
        LocalDateTime endTime = startTime.plusMinutes(30);

        // Create second patient to request same doctor
        var patientPayload = Map.of(
                "firstName", "ApptPatientTwo",
                "lastName", "Test",
                "email", "appt.patient.two." + System.currentTimeMillis() + "@test.com"
        );
        var patientResult = mockMvc.perform(post("/api/v1/patients")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(patientPayload)))
                .andExpect(status().isCreated())
                .andReturn();
        String patientTwoUuidStr = JsonPath.parse(patientResult.getResponse().getContentAsString()).read("$.uuid");

        var payload = Map.of(
                "patientUuid", patientTwoUuidStr,
                "doctorUuid", doctorUuid.toString(),
                "startTime", startTime.toString(),
                "endTime", endTime.toString(),
                "visitType", "FOLLOW_UP",
                "reasonForVisit", "Another checkup"
        );

        mockMvc.perform(post("/api/v1/appointments")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(payload)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Doctor is already booked for this time slot."));
    }

    @Test
    @Order(3)
    void updateAppointmentStatus_invalidTransition_fails() throws Exception {
        // Can't transition SCHEDULED to IN_PROGRESS directly (must be CONFIRMED or CHECKED_IN first)
        var payload = Map.of("status", "IN_PROGRESS");

        mockMvc.perform(put("/api/v1/appointments/" + appointmentUuid + "/status")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(payload)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Invalid status transition from SCHEDULED to IN_PROGRESS"));
    }

    @Test
    @Order(4)
    void updateAppointmentStatus_success() throws Exception {
        // SCHEDULED -> CONFIRMED
        var payload = Map.of("status", "CONFIRMED");

        mockMvc.perform(put("/api/v1/appointments/" + appointmentUuid + "/status")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(payload)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("CONFIRMED"));
    }

    @Test
    @Order(5)
    void rescheduleAppointment_success() throws Exception {
        // Move to different day/time
        LocalDateTime newStartTime = LocalDateTime.now().plusDays(3).withHour(14).withMinute(0).withSecond(0).withNano(0);
        LocalDateTime newEndTime = newStartTime.plusMinutes(45);

        var payload = Map.of(
                "startTime", newStartTime.toString(),
                "endTime", newEndTime.toString()
        );

        mockMvc.perform(put("/api/v1/appointments/" + appointmentUuid + "/reschedule")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(payload)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("SCHEDULED")); // Status resets to SCHEDULED
    }

    @Test
    @Order(6)
    void getAppointmentByUuid_success() throws Exception {
        mockMvc.perform(get("/api/v1/appointments/" + appointmentUuid)
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.uuid").value(appointmentUuid.toString()));
    }

    @Test
    @Order(7)
    void getAppointmentByNumber_success() throws Exception {
        var result = mockMvc.perform(get("/api/v1/appointments/" + appointmentUuid)
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andReturn();

        String appointmentNumber = JsonPath.parse(result.getResponse().getContentAsString()).read("$.appointmentNumber");

        mockMvc.perform(get("/api/v1/appointments/number/" + appointmentNumber)
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.appointmentNumber").value(appointmentNumber));
    }

    @Test
    @Order(8)
    void getAppointmentByUuid_notFound() throws Exception {
        mockMvc.perform(get("/api/v1/appointments/" + UUID.randomUUID())
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isNotFound());
    }

    @Test
    @Order(9)
    void createAppointment_patientOverlap_fails() throws Exception {
        LocalDateTime startTime = LocalDateTime.now().plusDays(5).withHour(9).withMinute(0).withSecond(0).withNano(0);
        LocalDateTime endTime = startTime.plusMinutes(30);

        // Create second doctor to request same patient
        var doctorPayload = Map.of(
                "firstName", "OverlapDoctor",
                "lastName", "Test",
                "email", "overlap.doctor." + System.currentTimeMillis() + "@test.com",
                "specialization", "DERMATOLOGY"
        );
        var doctorResult = mockMvc.perform(post("/api/v1/doctors")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(doctorPayload)))
                .andExpect(status().isCreated())
                .andReturn();
        String doctorTwoUuidStr = JsonPath.parse(doctorResult.getResponse().getContentAsString()).read("$.uuid");

        // Create first appointment
        var firstPayload = Map.of(
                "patientUuid", patientUuid.toString(),
                "doctorUuid", doctorTwoUuidStr,
                "startTime", startTime.toString(),
                "endTime", endTime.toString(),
                "visitType", "CONSULTATION"
        );
        mockMvc.perform(post("/api/v1/appointments")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(firstPayload)))
                .andExpect(status().isCreated());

        // Create a third doctor so patient overlap is tested (not doctor overlap)
        var doctorThreePayload = Map.of(
                "firstName", "OverlapDoctorThree",
                "lastName", "Test",
                "email", "overlap.doc.three." + System.currentTimeMillis() + "@test.com",
                "specialization", "PEDIATRICS"
        );
        var doctorThreeResult = mockMvc.perform(post("/api/v1/doctors")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(doctorThreePayload)))
                .andExpect(status().isCreated())
                .andReturn();
        String doctorThreeUuidStr = JsonPath.parse(doctorThreeResult.getResponse().getContentAsString()).read("$.uuid");

        // Try to create another with same patient, different doctor, overlapping time
        var overlapPayload = Map.of(
                "patientUuid", patientUuid.toString(),
                "doctorUuid", doctorThreeUuidStr,
                "startTime", startTime.plusMinutes(15).toString(),
                "endTime", endTime.plusMinutes(15).toString(),
                "visitType", "FOLLOW_UP"
        );
        mockMvc.perform(post("/api/v1/appointments")
                        .header("Authorization", "Bearer " + adminToken)
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
                        .header("Authorization", "Bearer " + adminToken)
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
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(patientPayload)))
                .andExpect(status().isCreated())
                .andReturn();
        String conflictPatientUuid = JsonPath.parse(patientResult.getResponse().getContentAsString()).read("$.uuid");

        // Create first appointment
        LocalDateTime slotStart = LocalDateTime.now().plusDays(7).withHour(10).withMinute(0).withSecond(0).withNano(0);
        LocalDateTime slotEnd = slotStart.plusMinutes(30);

        var firstPayload = Map.of(
                "patientUuid", conflictPatientUuid,
                "doctorUuid", conflictDoctorUuid,
                "startTime", slotStart.toString(),
                "endTime", slotEnd.toString(),
                "visitType", "CONSULTATION"
        );
        var firstResult = mockMvc.perform(post("/api/v1/appointments")
                        .header("Authorization", "Bearer " + adminToken)
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
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(secondPayload)))
                .andExpect(status().isCreated())
                .andReturn();
        String secondApptUuid = JsonPath.parse(secondResult.getResponse().getContentAsString()).read("$.uuid");

        // Try to reschedule second appointment into the first's time slot
        var reschedulePayload = Map.of(
                "startTime", slotStart.toString(),
                "endTime", slotEnd.toString()
        );
        mockMvc.perform(put("/api/v1/appointments/" + secondApptUuid + "/reschedule")
                        .header("Authorization", "Bearer " + adminToken)
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
    @Order(12)
    void listAppointments_paginated() throws Exception {
        mockMvc.perform(get("/api/v1/appointments")
                        .header("Authorization", "Bearer " + adminToken)
                        .param("page", "0")
                        .param("size", "5"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray())
                .andExpect(jsonPath("$.page").isNumber())
                .andExpect(jsonPath("$.totalElements").isNumber());
    }
}
