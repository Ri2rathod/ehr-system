package com.example.ehrsystem.modules.auth.integration;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
class AuthIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    private final String registerUrl = "/api/v1/auth/register";
    private final String loginUrl = "/api/v1/auth/login";
    private final String secureUrl = "/api/v1/test/secure";
    private final String adminUrl = "/api/v1/test/admin";

    private String registerAndLoginAndGetToken() throws Exception {
        // Register
        var registerPayload = Map.of(
                "firstName", "John",
                "lastName", "Doe",
                "email", "john.doe@example.com",
                "password", "Secret123!",
                "username", "johndoe"
        );
        mockMvc.perform(post(registerUrl)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(registerPayload)))
                .andExpect(status().isCreated());

        // Login
        var loginPayload = Map.of(
                "email", "john.doe@example.com",
                "password", "Secret123!"
        );
        MvcResult loginResult = mockMvc.perform(post(loginUrl)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginPayload)))
                .andExpect(status().isOk())
                .andReturn();
        String responseBody = loginResult.getResponse().getContentAsString();
        Map<?, ?> respMap = objectMapper.readValue(responseBody, Map.class);
        return (String) respMap.get("accessToken");
    }

    @Test
    void testSecureEndpointAccess() throws Exception {
        String token = registerAndLoginAndGetToken();
        mockMvc.perform(get(secureUrl)
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.authorities").isArray())
                .andExpect(jsonPath("$.authorities[?(@ == 'ROLE_PATIENT')]").exists());
    }

    @Test
    void testAdminEndpointForbidden() throws Exception {
        String token = registerAndLoginAndGetToken();
        mockMvc.perform(get(adminUrl)
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isForbidden());
    }
}
