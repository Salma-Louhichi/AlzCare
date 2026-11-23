package com.alzcare.careteam.controller;

import com.alzcare.careteam.config.SecurityConfig;
import com.alzcare.careteam.entity.DoctorAssignment;
import com.alzcare.careteam.service.DoctorAssignmentService;
import com.alzcare.careteam.service.PatientCareProfileService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Regression tests for SEC-1 (IMPROVEMENTS.md): creating a doctor–patient assignment used to be
 * reachable without any authentication.
 */
@WebMvcTest(DoctorAssignmentController.class)
@Import(SecurityConfig.class)
class DoctorAssignmentControllerSecurityTest {

    private static final String DOCTOR_ID = "doctor-123";
    private static final String URL = "/api/v1/care-team/doctors/" + DOCTOR_ID + "/patients/create";
    private static final String BODY = "{\"patientId\":\"patient-456\"}";

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private DoctorAssignmentService service;

    @MockBean
    private PatientCareProfileService patientCareProfileService;

    @MockBean
    private JwtDecoder jwtDecoder;

    @Test
    void rejectsAnonymousRequests() throws Exception {
        mockMvc.perform(post(URL).contentType(MediaType.APPLICATION_JSON).content(BODY))
                .andExpect(status().isUnauthorized());
        verify(service, never()).createAssignment(anyString(), anyString());
    }

    @Test
    void rejectsDoctorAssigningToAnotherDoctor() throws Exception {
        mockMvc.perform(post(URL).contentType(MediaType.APPLICATION_JSON).content(BODY)
                        .with(jwt().jwt(j -> j.subject("someone-else")
                                .claim("realm_access", java.util.Map.of("roles", List.of("DOCTOR"))))))
                .andExpect(status().isForbidden());
        verify(service, never()).createAssignment(anyString(), anyString());
    }

    @Test
    void rejectsNonDoctorRoles() throws Exception {
        mockMvc.perform(post(URL).contentType(MediaType.APPLICATION_JSON).content(BODY)
                        .with(jwt().jwt(j -> j.subject(DOCTOR_ID)
                                .claim("realm_access", java.util.Map.of("roles", List.of("PATIENT"))))))
                .andExpect(status().isForbidden());
    }

    @Test
    void allowsDoctorAssigningToThemselves() throws Exception {
        when(service.createAssignment(DOCTOR_ID, "patient-456")).thenReturn(new DoctorAssignment());
        mockMvc.perform(post(URL).contentType(MediaType.APPLICATION_JSON).content(BODY)
                        .with(jwt().jwt(j -> j.subject(DOCTOR_ID)
                                .claim("realm_access", java.util.Map.of("roles", List.of("DOCTOR"))))))
                .andExpect(status().isOk());
        verify(service).createAssignment(DOCTOR_ID, "patient-456");
    }

    @Test
    void allowsAdminForAnyDoctor() throws Exception {
        when(service.createAssignment(DOCTOR_ID, "patient-456")).thenReturn(new DoctorAssignment());
        mockMvc.perform(post(URL).contentType(MediaType.APPLICATION_JSON).content(BODY)
                        .with(jwt().jwt(j -> j.subject("admin-1")
                                .claim("realm_access", java.util.Map.of("roles", List.of("ADMIN"))))))
                .andExpect(status().isOk());
    }
}
