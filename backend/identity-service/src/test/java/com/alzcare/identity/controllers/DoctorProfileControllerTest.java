package com.alzcare.identity.controllers;

import com.alzcare.identity.dtos.responses.DoctorProfileResponse;
import com.alzcare.identity.exceptions.DoctorNotFoundException;
import com.alzcare.identity.services.DoctorProfileService;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.web.servlet.MockMvc;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.UUID;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(DoctorProfileController.class)
@ContextConfiguration(classes = {DoctorProfileController.class, GlobalExceptionHandler.class})
class DoctorProfileControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private DoctorProfileService doctorProfileService;

    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule());
        objectMapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
    }

    @Test
    @DisplayName("Should return doctor profile when valid ID is provided")
    void shouldReturnDoctorProfileWhenValidIdIsProvided() throws Exception {
        // Given
        UUID doctorId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        DoctorProfileResponse response = createDoctorProfileResponse(doctorId, userId);

        when(doctorProfileService.getDoctorById(doctorId)).thenReturn(response);

        // When & Then
        mockMvc.perform(get("/api/v1/doctors/{id}", doctorId)
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.id").value(doctorId.toString()))
                .andExpect(jsonPath("$.userId").value(userId.toString()))
                .andExpect(jsonPath("$.firstName").value("John"))
                .andExpect(jsonPath("$.lastName").value("Doe"))
                .andExpect(jsonPath("$.photoUrl").value("https://example.com/photo.jpg"))
                .andExpect(jsonPath("$.speciality").value("Neurology"))
                .andExpect(jsonPath("$.licenseNumber").value("LIC-12345"))
                .andExpect(jsonPath("$.phone").value("+1-555-123-4567"))
                .andExpect(jsonPath("$.contact").value("doctor@example.com"))
                .andExpect(jsonPath("$.address").value("123 Medical Center Drive"))
                .andExpect(jsonPath("$.isAvailable").value(true));

        verify(doctorProfileService).getDoctorById(doctorId);
    }

    @Test
    @DisplayName("Should return 404 when doctor ID is not found")
    void shouldReturn404WhenDoctorIdIsNotFound() throws Exception {
        // Given
        UUID doctorId = UUID.randomUUID();

        when(doctorProfileService.getDoctorById(doctorId))
                .thenThrow(new DoctorNotFoundException(doctorId));

        // When & Then
        mockMvc.perform(get("/api/v1/doctors/{id}", doctorId)
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.title").value("Doctor Not Found"))
                .andExpect(jsonPath("$.detail").value("Doctor profile not found with id: " + doctorId));

        verify(doctorProfileService).getDoctorById(doctorId);
    }

    @Test
    @DisplayName("Should return doctor profile when valid user ID is provided")
    void shouldReturnDoctorProfileWhenValidUserIdIsProvided() throws Exception {
        // Given
        UUID userId = UUID.randomUUID();
        UUID doctorId = UUID.randomUUID();
        DoctorProfileResponse response = createDoctorProfileResponse(doctorId, userId);

        when(doctorProfileService.getDoctorByUserId(userId)).thenReturn(response);

        // When & Then
        mockMvc.perform(get("/api/v1/doctors/user/{userId}", userId)
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.id").value(doctorId.toString()))
                .andExpect(jsonPath("$.userId").value(userId.toString()))
                .andExpect(jsonPath("$.firstName").value("John"))
                .andExpect(jsonPath("$.lastName").value("Doe"))
                .andExpect(jsonPath("$.photoUrl").value("https://example.com/photo.jpg"))
                .andExpect(jsonPath("$.speciality").value("Neurology"))
                .andExpect(jsonPath("$.licenseNumber").value("LIC-12345"))
                .andExpect(jsonPath("$.phone").value("+1-555-123-4567"))
                .andExpect(jsonPath("$.contact").value("doctor@example.com"))
                .andExpect(jsonPath("$.address").value("123 Medical Center Drive"))
                .andExpect(jsonPath("$.isAvailable").value(true));

        verify(doctorProfileService).getDoctorByUserId(userId);
    }

    @Test
    @DisplayName("Should return 404 when user ID is not found")
    void shouldReturn404WhenUserIdIsNotFound() throws Exception {
        // Given
        UUID userId = UUID.randomUUID();

        when(doctorProfileService.getDoctorByUserId(userId))
                .thenThrow(new DoctorNotFoundException("Doctor profile not found with userId: " + userId, true));

        // When & Then
        mockMvc.perform(get("/api/v1/doctors/user/{userId}", userId)
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.title").value("Doctor Not Found"))
                .andExpect(jsonPath("$.detail").value("Doctor profile not found with userId: " + userId));

        verify(doctorProfileService).getDoctorByUserId(userId);
    }

    private DoctorProfileResponse createDoctorProfileResponse(UUID id, UUID userId) {
        DoctorProfileResponse response = new DoctorProfileResponse();
        response.setId(id);
        response.setUserId(userId);
        response.setFirstName("John");
        response.setLastName("Doe");
        response.setPhotoUrl("https://example.com/photo.jpg");
        response.setSpeciality("Neurology");
        response.setLicenseNumber("LIC-12345");
        response.setPhone("+1-555-123-4567");
        response.setContact("doctor@example.com");
        response.setAddress("123 Medical Center Drive");
        response.setIsAvailable(true);
        response.setCreatedAt(OffsetDateTime.of(2024, 1, 15, 10, 0, 0, 0, ZoneOffset.UTC));
        response.setUpdatedAt(OffsetDateTime.of(2024, 1, 16, 14, 30, 0, 0, ZoneOffset.UTC));
        
        return response;
    }
}
