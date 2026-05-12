package com.example.ehrsystem.modules.patient.integration;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.*;

import java.util.Map;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import static org.assertj.core.api.Assertions.assertThat;

@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
@org.springframework.boot.test.context.SpringBootTest
class PatientIntegrationTest {

    @org.springframework.beans.factory.annotation.Autowired
    private MockMvc mockMvc;

    @org.springframework.beans.factory.annotation.Autowired
    private ObjectMapper objectMapper;

    private String accessToken;
    private String refreshToken;

    private String registerAndLogin(String suffix) throws Exception {
        String email = "patient.test." + suffix + "@example.com";
        String username = "patientuser" + suffix;

        var registerPayload = Map.of(
                "firstName", "Patient",
                "lastName", "Test" + suffix,
                "email", email,
                "password", "Secret123!",
                "username", username
        );

        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(registerPayload)))
                .andExpect(status().isOk());

        var loginPayload = Map.of("email", email, "password", "Secret123!");

        var result = mockMvc.perform(post("/api/v1/auth/login")
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(loginPayload)))
                .andExpect(status().isOk())
                .andReturn();

        String body = result.getResponse().getContentAsString();
        return JsonPath.parse(body).read("$.data.accessToken");
    }

    private String getAdminToken() throws Exception {
        var loginPayload = Map.of("email", "admin@ehr.com", "password", "Admin@123!");
        var result = mockMvc.perform(post("/api/v1/auth/login")
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(loginPayload)))
                .andExpect(status().isOk())
                .andReturn();
        return JsonPath.parse(result.getResponse().getContentAsString()).read("$.data.accessToken");
    }

    @BeforeEach
    void setUp() throws Exception {
        accessToken = getAdminToken();
    }

    @Test
    @Order(1)
    void createPatient_success() throws Exception {
        var payload = Map.of(
                "firstName", "John",
                "lastName", "Doe",
                "gender", "MALE",
                "dateOfBirth", "1990-05-15",
                "bloodGroup", "A_POSITIVE",
                "email", "john.doe." + System.currentTimeMillis() + "@test.com"
        );

        mockMvc.perform(post("/api/v1/patients")
                        .header("Authorization", "Bearer " + accessToken)
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(payload)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.mrn").exists())
                .andExpect(jsonPath("$.data.uuid").exists())
                .andExpect(jsonPath("$.data.firstName").value("John"));
    }

    @Test
    @Order(2)
    void createPatient_validationError() throws Exception {
        var payload = Map.of(
                "lastName", "Doe"
        );

        mockMvc.perform(post("/api/v1/patients")
                        .header("Authorization", "Bearer " + accessToken)
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(payload)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.validationErrors.firstName").exists());
    }

    @Test
    @Order(3)
    void createPatient_duplicateEmail_returns409() throws Exception {
        String uniqueEmail = "dup.test." + System.currentTimeMillis() + "@test.com";

        var payload = Map.of(
                "firstName", "First",
                "lastName", "Patient",
                "email", uniqueEmail
        );

        mockMvc.perform(post("/api/v1/patients")
                        .header("Authorization", "Bearer " + accessToken)
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(payload)))
                .andExpect(status().isCreated());

        mockMvc.perform(post("/api/v1/patients")
                        .header("Authorization", "Bearer " + accessToken)
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(payload)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").value("Email already exists for another patient"));
    }

    @Test
    @Order(4)
    void getPatient_notFound() throws Exception {
        mockMvc.perform(get("/api/v1/patients/999999")
                        .header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isNotFound());
    }

    @Test
    @Order(5)
    void listPatients_paginated() throws Exception {
        mockMvc.perform(get("/api/v1/patients")
                        .header("Authorization", "Bearer " + accessToken)
                        .param("page", "0")
                        .param("size", "5"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray())
                .andExpect(jsonPath("$.page").value(0))
                .andExpect(jsonPath("$.totalElements").isNumber());
    }

    @Test
    @Order(6)
    void listPatients_maxPageSizeEnforced() throws Exception {
        mockMvc.perform(get("/api/v1/patients")
                        .header("Authorization", "Bearer " + accessToken)
                        .param("size", "999999"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.size").value(100));
    }

    @Test
    @Order(7)
    void searchPatients_paginated() throws Exception {
        mockMvc.perform(get("/api/v1/patients/search")
                        .header("Authorization", "Bearer " + accessToken)
                        .param("q", "John")
                        .param("page", "0")
                        .param("size", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray());
    }

    @Test
    @Order(8)
    void getPatientsByStatus_paginated() throws Exception {
        mockMvc.perform(get("/api/v1/patients/status/ACTIVE")
                        .header("Authorization", "Bearer " + accessToken)
                        .param("page", "0")
                        .param("size", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray())
                .andExpect(jsonPath("$.page").value(0));
    }

    @Test
    @Order(9)
    void updatePatient_success() throws Exception {
        var createPayload = Map.of(
                "firstName", "UpdateMe",
                "lastName", "Patient",
                "email", "update.test." + System.currentTimeMillis() + "@test.com"
        );

        var createResult = mockMvc.perform(post("/api/v1/patients")
                        .header("Authorization", "Bearer " + accessToken)
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(createPayload)))
                .andExpect(status().isCreated())
                .andReturn();

        Long patientId = JsonPath.parse(createResult.getResponse().getContentAsString()).read("$.data.id");

        var updatePayload = Map.of(
                "firstName", "UpdatedName",
                "lastName", "Patient",
                "city", "NewCity"
        );

        mockMvc.perform(put("/api/v1/patients/" + patientId)
                        .header("Authorization", "Bearer " + accessToken)
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(updatePayload)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.firstName").value("UpdatedName"))
                .andExpect(jsonPath("$.data.city").value("NewCity"));
    }

    @Test
    @Order(10)
    void deletePatient_softDeletes() throws Exception {
        var createPayload = Map.of(
                "firstName", "DeleteMe",
                "lastName", "Patient",
                "email", "delete.test." + System.currentTimeMillis() + "@test.com"
        );

        var createResult = mockMvc.perform(post("/api/v1/patients")
                        .header("Authorization", "Bearer " + accessToken)
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(createPayload)))
                .andExpect(status().isCreated())
                .andReturn();

        Long patientId = JsonPath.parse(createResult.getResponse().getContentAsString()).read("$.data.id");

        mockMvc.perform(delete("/api/v1/patients/" + patientId)
                        .header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isNoContent());

        mockMvc.perform(get("/api/v1/patients/" + patientId)
                        .header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isNotFound());
    }

    @Test
    @Order(11)
    void patientEndpoints_requireAuthentication() throws Exception {
        mockMvc.perform(get("/api/v1/patients"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @Order(12)
    void patientEndpoints_requirePermission() throws Exception {
        String noPermToken = registerAndLogin("_noperm" + System.currentTimeMillis());

        mockMvc.perform(get("/api/v1/patients")
                        .header("Authorization", "Bearer " + noPermToken))
                .andExpect(status().isForbidden());
    }

    @Test
    @Order(13)
    void invalidEnumValue_rejected() throws Exception {
        var payload = Map.of(
                "firstName", "Test",
                "lastName", "Patient",
                "gender", "INVALID_GENDER"
        );

        mockMvc.perform(post("/api/v1/patients")
                        .header("Authorization", "Bearer " + accessToken)
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(payload)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @Order(14)
    void getPatientByUuid_success() throws Exception {
        var createPayload = Map.of(
                "firstName", "UuidTest",
                "lastName", "Patient",
                "email", "uuid.test." + System.currentTimeMillis() + "@test.com"
        );

        var createResult = mockMvc.perform(post("/api/v1/patients")
                        .header("Authorization", "Bearer " + accessToken)
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(createPayload)))
                .andExpect(status().isCreated())
                .andReturn();

        String uuid = JsonPath.parse(createResult.getResponse().getContentAsString()).read("$.data.uuid");

        mockMvc.perform(get("/api/v1/patients/uuid/" + uuid)
                        .header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.firstName").value("UuidTest"));
    }

    @Test
    @Order(15)
    void getPatientByMrn_success() throws Exception {
        var createPayload = Map.of(
                "firstName", "MrnTest",
                "lastName", "Patient",
                "email", "mrn.test." + System.currentTimeMillis() + "@test.com"
        );

        var createResult = mockMvc.perform(post("/api/v1/patients")
                        .header("Authorization", "Bearer " + accessToken)
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(createPayload)))
                .andExpect(status().isCreated())
                .andReturn();

        String mrn = JsonPath.parse(createResult.getResponse().getContentAsString()).read("$.data.mrn");

        mockMvc.perform(get("/api/v1/patients/mrn/" + mrn)
                        .header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.firstName").value("MrnTest"));
    }
}