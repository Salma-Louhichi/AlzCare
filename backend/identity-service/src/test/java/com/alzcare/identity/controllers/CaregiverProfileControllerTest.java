package com.alzcare.identity.controllers;

import com.alzcare.identity.dtos.responses.CaregiverProfileResponse;
import com.alzcare.identity.exceptions.CaregiverNotFoundException;
import com.alzcare.identity.services.CaregiverProfileService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

import java.time.OffsetDateTime;
import java.util.UUID;

import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Unit tests for {@link CaregiverProfileController}.
 * Uses {@link WebMvcTest} to test the controller in isolation with mocked service layer.
 */
@WebMvcTest(CaregiverProfileController.class)
@ContextConfiguration(classes = {CaregiverProfileController.class, GlobalExceptionHandler.class})
@DisplayName("CaregiverProfileController Unit Tests")
class CaregiverProfileControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private CaregiverProfileService caregiverProfileService;

    private CaregiverProfileResponse caregiverResponse;
    private UUID caregiverId;
    private UUID userId;

    @BeforeEach
    void setUp() {
        caregiverId = UUID.fromString("550e8400-e29b-41d4-a716-446655440000");
        userId = UUID.fromString("550e8400-e29b-41d4-a716-446655440001");

        caregiverResponse = new CaregiverProfileResponse();
        caregiverResponse.setId(caregiverId);
        caregiverResponse.setUserId(userId);
        caregiverResponse.setFirstName("John");
        caregiverResponse.setLastName("Doe");
        caregiverResponse.setPhotoUrl("https://example.com/photo.jpg");
        caregiverResponse.setPhone("+1234567890");
        caregiverResponse.setAddress("123 Main St, City");
        caregiverResponse.setContact("john.doe@example.com");
        caregiverResponse.setIsAvailable(true);
        caregiverResponse.setIsProfessional(false);
        caregiverResponse.setCreatedAt(OffsetDateTime.parse("2024-01-15T10:30:00+01:00"));
        caregiverResponse.setUpdatedAt(OffsetDateTime.parse("2024-01-20T14:45:00+01:00"));
    }

    @Test
    @DisplayName("Should return caregiver profile when GET /api/v1/caregivers/{id} is called with valid ID")
    void shouldReturnCaregiverProfileWhenGetByIdWithValidId() throws Exception {
        // Given
        when(caregiverProfileService.getCaregiverById(caregiverId)).thenReturn(caregiverResponse);

        // When
        ResultActions result = mockMvc.perform(get("/api/v1/caregivers/{id}", caregiverId)
                .contentType(MediaType.APPLICATION_JSON));

        // Then
        result.andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.id").value(caregiverId.toString()))
                .andExpect(jsonPath("$.userId").value(userId.toString()))
                .andExpect(jsonPath("$.firstName").value("John"))
                .andExpect(jsonPath("$.lastName").value("Doe"))
                .andExpect(jsonPath("$.photoUrl").value("https://example.com/photo.jpg"))
                .andExpect(jsonPath("$.phone").value("+1234567890"))
                .andExpect(jsonPath("$.address").value("123 Main St, City"))
                .andExpect(jsonPath("$.contact").value("john.doe@example.com"))
                .andExpect(jsonPath("$.isAvailable").value(true))
                .andExpect(jsonPath("$.isProfessional").value(false))
                .andExpect(jsonPath("$.createdAt").value("2024-01-15T10:30:00+01:00"))
                .andExpect(jsonPath("$.updatedAt").value("2024-01-20T14:45:00+01:00"));

        verify(caregiverProfileService, times(1)).getCaregiverById(caregiverId);
    }

    @Test
    @DisplayName("Should return 404 when GET /api/v1/caregivers/{id} is called with non-existent ID")
    void shouldReturn404WhenGetByIdWithNonExistentId() throws Exception {
        // Given
        UUID nonExistentId = UUID.fromString("550e8400-e29b-41d4-a716-446655440999");
        when(caregiverProfileService.getCaregiverById(nonExistentId))
                .thenThrow(new CaregiverNotFoundException(nonExistentId));

        // When
        ResultActions result = mockMvc.perform(get("/api/v1/caregivers/{id}", nonExistentId)
                .contentType(MediaType.APPLICATION_JSON));

        // Then
        result.andExpect(status().isNotFound())
                .andExpect(content().contentType(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.title").value("Caregiver Not Found"))
                .andExpect(jsonPath("$.detail").value("Caregiver profile not found with id: " + nonExistentId))
                .andExpect(jsonPath("$.type").value("https://alzcare.com/errors/caregiver-not-found"))
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.timestamp").exists());

        verify(caregiverProfileService, times(1)).getCaregiverById(nonExistentId);
    }

    @Test
    @DisplayName("Should return caregiver profile when GET /api/v1/caregivers/user/{userId} is called with valid user ID")
    void shouldReturnCaregiverProfileWhenGetByUserIdWithValidUserId() throws Exception {
        // Given
        when(caregiverProfileService.getCaregiverByUserId(userId)).thenReturn(caregiverResponse);

        // When
        ResultActions result = mockMvc.perform(get("/api/v1/caregivers/user/{userId}", userId)
                .contentType(MediaType.APPLICATION_JSON));

        // Then
        result.andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.id").value(caregiverId.toString()))
                .andExpect(jsonPath("$.userId").value(userId.toString()))
                .andExpect(jsonPath("$.firstName").value("John"))
                .andExpect(jsonPath("$.lastName").value("Doe"))
                .andExpect(jsonPath("$.photoUrl").value("https://example.com/photo.jpg"))
                .andExpect(jsonPath("$.phone").value("+1234567890"))
                .andExpect(jsonPath("$.address").value("123 Main St, City"))
                .andExpect(jsonPath("$.contact").value("john.doe@example.com"))
                .andExpect(jsonPath("$.isAvailable").value(true))
                .andExpect(jsonPath("$.isProfessional").value(false))
                .andExpect(jsonPath("$.createdAt").value("2024-01-15T10:30:00+01:00"))
                .andExpect(jsonPath("$.updatedAt").value("2024-01-20T14:45:00+01:00"));

        verify(caregiverProfileService, times(1)).getCaregiverByUserId(userId);
    }

    @Test
    @DisplayName("Should return 404 when GET /api/v1/caregivers/user/{userId} is called with non-existent user ID")
    void shouldReturn404WhenGetByUserIdWithNonExistentUserId() throws Exception {
        // Given
        UUID nonExistentUserId = UUID.fromString("550e8400-e29b-41d4-a716-446655440998");
        when(caregiverProfileService.getCaregiverByUserId(nonExistentUserId))
                .thenThrow(new CaregiverNotFoundException(nonExistentUserId));

        // When
        ResultActions result = mockMvc.perform(get("/api/v1/caregivers/user/{userId}", nonExistentUserId)
                .contentType(MediaType.APPLICATION_JSON));

        // Then
        result.andExpect(status().isNotFound())
                .andExpect(content().contentType(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.title").value("Caregiver Not Found"))
                .andExpect(jsonPath("$.detail").value("Caregiver profile not found with id: " + nonExistentUserId))
                .andExpect(jsonPath("$.type").value("https://alzcare.com/errors/caregiver-not-found"))
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.timestamp").exists());

        verify(caregiverProfileService, times(1)).getCaregiverByUserId(nonExistentUserId);
    }
}
