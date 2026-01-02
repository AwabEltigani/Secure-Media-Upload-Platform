package com.example.cloud_file_storage.controllers;


import com.fasterxml.jackson.databind.ObjectMapper;
import com.jayway.jsonpath.JsonPath;


import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.test.web.servlet.MockMvc;

import com.example.cloud_file_storage.dto.request.LoginRequest;
import com.example.cloud_file_storage.dto.request.RegisterRequest;
import com.example.cloud_file_storage.dto.response.AuthResponse;
import com.example.cloud_file_storage.services.AuthService;


import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;

import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;

@SpringBootTest
@AutoConfigureMockMvc


class AuthIntegrationTest {

    @Autowired
    private MockMvc mockMvc;
    private AuthService authService;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void shouldDenyAccessToUserAfterLogout() throws Exception {
        RegisterRequest request = new RegisterRequest("john dow", "john", "doe", "johndow123@gmail.com", "password123");

        String registerResponseString = mockMvc.perform(post("/api/auth/register")
        .contentType(MediaType.APPLICATION_JSON)
        .content(objectMapper.writeValueAsString(request)))
        .andExpect(status().isOk())
        .andReturn()
        .getResponse()
        .getContentAsString();

        LoginRequest login = new LoginRequest("johndow123@gmail.com", "password123");
        
        String loginResponseString = mockMvc.perform(post("/api/auth/login")
        .contentType(MediaType.APPLICATION_JSON)
        .content(objectMapper.writeValueAsString(login)))
        .andExpect(status().isOk())
        .andReturn()
        .getResponse()
        .getContentAsString();

        String token = JsonPath.read(loginResponseString, "$.data.token");

        String bearerToken = "Bearer " + token;

        String profileResponseString = mockMvc.perform(get("/api/auth/me")
        .header("Authorization", bearerToken))
        .andReturn()
        .getResponse()
        .getContentAsString();

        String loginId = JsonPath.read(loginResponseString, "$.data.id").toString();
        String profileId = JsonPath.read(profileResponseString, "$.data.id").toString();

        Assertions.assertThat(loginId).isEqualTo(profileId);

        mockMvc.perform(post("/api/auth/logout")
            .header("Authorization", bearerToken))
            .andExpect(status().isOk());

        mockMvc.perform(get("/api/auth/me")
            .header("Authorization", bearerToken))
            .andExpect(status().isForbidden());




        



        


        // 2. Call /api/auth/logout with that token
        // 3. Call /api/auth/me with the same token
        // 4. Assert that status is 401 or 403
    }
}