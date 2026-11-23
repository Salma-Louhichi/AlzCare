package com.alzcare.notification;

import com.alzcare.notification.dto.request.NotificationRequest;
import com.alzcare.notification.dto.response.NotificationResponse;
import com.alzcare.notification.enums.NotificationChannel;
import com.alzcare.notification.enums.NotificationPriority;
import com.alzcare.notification.enums.NotificationStatus;
import com.alzcare.notification.enums.NotificationType;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.testcontainers.containers.MongoDBContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Integration tests for the Notification Service.
 * Uses Testcontainers for MongoDB and tests the full API flow.
 */
@SpringBootTest
@Testcontainers
@AutoConfigureMockMvc
@ActiveProfiles("dev")
public class NotificationServiceIntegrationTest {

    @Container
    static MongoDBContainer mongoDBContainer = new MongoDBContainer(DockerImageName.parse("mongo:7.0"));

    @DynamicPropertySource
    static void setProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.data.mongodb.uri", mongoDBContainer::getReplicaSetUrl);
    }

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        // Clean up could be done here if needed
    }

    @Test
    void testCreateNotificationViaApi() throws Exception {
        // Given
        NotificationRequest request = NotificationRequest.builder()
                .recipientId("user-123")
                .type(NotificationType.ALERT)
                .channel(NotificationChannel.IN_APP)
                .priority(NotificationPriority.HIGH)
                .title("Test Alert")
                .message("This is a test notification")
                .build();

        // When & Then
        MvcResult result = mockMvc.perform(post("/api/v1/notifications")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.recipientId").value("user-123"))
                .andExpect(jsonPath("$.title").value("Test Alert"))
                .andExpect(jsonPath("$.message").value("This is a test notification"))
                .andExpect(jsonPath("$.type").value("ALERT"))
                .andExpect(jsonPath("$.channel").value("IN_APP"))
                .andExpect(jsonPath("$.status").value("PENDING"))
                .andReturn();

        String responseBody = result.getResponse().getContentAsString();
        NotificationResponse response = objectMapper.readValue(responseBody, NotificationResponse.class);
        assertThat(response.getId()).isNotNull();
    }

    @Test
    void testGetNotificationsByUser() throws Exception {
        // First create a notification
        NotificationRequest request = NotificationRequest.builder()
                .recipientId("user-456")
                .type(NotificationType.REMINDER)
                .channel(NotificationChannel.IN_APP)
                .priority(NotificationPriority.NORMAL)
                .title("Medication Reminder")
                .message("Time to take your medication")
                .build();

        mockMvc.perform(post("/api/v1/notifications")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated());

        // Then get notifications for that user
        mockMvc.perform(get("/api/v1/notifications/user/{userId}", "user-456"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray())
                .andExpect(jsonPath("$.content[0].recipientId").value("user-456"))
                .andExpect(jsonPath("$.content[0].title").value("Medication Reminder"));
    }

    @Test
    void testMarkAsRead() throws Exception {
        // First create a notification
        NotificationRequest request = NotificationRequest.builder()
                .recipientId("user-789")
                .type(NotificationType.SYSTEM)
                .channel(NotificationChannel.IN_APP)
                .priority(NotificationPriority.LOW)
                .title("System Update")
                .message("System has been updated")
                .build();

        MvcResult createResult = mockMvc.perform(post("/api/v1/notifications")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andReturn();

        String responseBody = createResult.getResponse().getContentAsString();
        NotificationResponse createdNotification = objectMapper.readValue(responseBody, NotificationResponse.class);
        String notificationId = createdNotification.getId();

        // Then mark it as read
        mockMvc.perform(put("/api/v1/notifications/{id}/read", notificationId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(notificationId))
                .andExpect(jsonPath("$.status").value("READ"))
                .andExpect(jsonPath("$.readAt").isNotEmpty());
    }

    @Test
    void testGetAllNotificationsWithFilters() throws Exception {
        // Create a notification
        NotificationRequest request = NotificationRequest.builder()
                .recipientId("user-filter")
                .type(NotificationType.ALERT)
                .channel(NotificationChannel.IN_APP)
                .priority(NotificationPriority.HIGH)
                .title("Filtered Alert")
                .message("Test filter message")
                .build();

        mockMvc.perform(post("/api/v1/notifications")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated());

        // Get all notifications with filter
        mockMvc.perform(get("/api/v1/notifications")
                        .param("userId", "user-filter")
                        .param("type", "ALERT"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray())
                .andExpect(jsonPath("$.content[0].recipientId").value("user-filter"));
    }

    @Test
    void testGetUnreadCount() throws Exception {
        // Create a notification
        NotificationRequest request = NotificationRequest.builder()
                .recipientId("user-count")
                .type(NotificationType.REMINDER)
                .channel(NotificationChannel.IN_APP)
                .priority(NotificationPriority.NORMAL)
                .title("Count Test")
                .message("Count test message")
                .build();

        mockMvc.perform(post("/api/v1/notifications")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated());

        // Get unread count
        mockMvc.perform(get("/api/v1/notifications/user/{userId}/unread/count", "user-count"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.userId").value("user-count"))
                .andExpect(jsonPath("$.unreadCount").isNumber())
                .andExpect(jsonPath("$.totalCount").isNumber());
    }

    @Test
    void testValidationErrorOnInvalidRequest() throws Exception {
        // Missing required fields
        NotificationRequest invalidRequest = NotificationRequest.builder()
                .recipientId("")  // Blank - should fail
                .type(NotificationType.ALERT)
                .channel(NotificationChannel.IN_APP)
                .title("Test")
                .message("Test message")
                .build();

        mockMvc.perform(post("/api/v1/notifications")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalidRequest)))
                .andExpect(status().isBadRequest());
    }
}
