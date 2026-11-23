package com.alzcare.notification;

import com.alzcare.notification.config.SecurityConfig;
import org.springframework.context.annotation.Import;
import com.alzcare.notification.controller.NotificationController;
import com.alzcare.notification.dto.request.NotificationRequest;
import com.alzcare.notification.dto.response.NotificationCountResponse;
import com.alzcare.notification.dto.response.NotificationResponse;
import com.alzcare.notification.enums.NotificationChannel;
import com.alzcare.notification.enums.NotificationPriority;
import com.alzcare.notification.enums.NotificationStatus;
import com.alzcare.notification.enums.NotificationType;
import com.alzcare.notification.mapper.NotificationMapper;
import com.alzcare.notification.repositories.NotificationRepository;
import com.alzcare.notification.service.NotificationRoutingService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import com.alzcare.notification.entities.Notification;
import com.alzcare.notification.enums.NotificationChannel;
import com.alzcare.notification.enums.NotificationPriority;
import com.alzcare.notification.enums.NotificationStatus;
import com.alzcare.notification.enums.NotificationType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.hamcrest.Matchers.hasSize;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isA;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Unit tests for the NotificationController using WebMvcTest.
 * Tests controller endpoints with mocked dependencies.
 */
@WebMvcTest(NotificationController.class)
// Load the service's own (stateless) security rules instead of Boot's default form/basic security
@Import(SecurityConfig.class)
public class NotificationControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private NotificationRepository notificationRepository;

    @MockBean
    private NotificationMapper notificationMapper;

    @MockBean
    private NotificationRoutingService routingService;

    private NotificationResponse sampleNotification;

    @BeforeEach
    void setUp() {
        sampleNotification = NotificationResponse.builder()
                .id("notif-123")
                .recipientId("user-123")
                .type("ALERT")
                .channel("IN_APP")
                .status("SENT")
                .priority("HIGH")
                .title("Test Notification")
                .message("Test message")
                .retryCount(0)
                .createdAt(Instant.now())
                .build();
    }

    @Test
    void testGetAllNotifications() throws Exception {
        // Given
        List<NotificationResponse> notifications = Collections.singletonList(sampleNotification);
        Page<NotificationResponse> page = new PageImpl<>(notifications);

        when(notificationRepository.findAll(any(org.springframework.data.domain.Example.class), any(Pageable.class)))
                .thenReturn(new PageImpl<>(Collections.emptyList()));
        when(notificationMapper.toResponse(isA(Notification.class))).thenReturn(sampleNotification);

        // When & Then
        mockMvc.perform(get("/api/v1/notifications")
                        .param("page", "0")
                        .param("size", "10"))
                .andExpect(status().isOk());
    }

    @Test
    void testGetNotificationById() throws Exception {
        // Given
        when(notificationRepository.findById("notif-123")).thenReturn(Optional.empty());

        // When & Then
        mockMvc.perform(get("/api/v1/notifications/{id}", "notif-123"))
                .andExpect(status().isNotFound());
    }

    @Test
    void testGetUserNotifications() throws Exception {
        // Given
        String userId = "user-123";
        when(notificationRepository.findByRecipientId(eq(userId), any(Pageable.class)))
                .thenReturn(Collections.emptyList());

        // When & Then
        mockMvc.perform(get("/api/v1/notifications/user/{userId}", userId))
                .andExpect(status().isOk());
    }

    @Test
    void testSendNotification() throws Exception {
        // Given
        NotificationRequest request = NotificationRequest.builder()
                .recipientId("user-123")
                .type(NotificationType.ALERT)
                .channel(NotificationChannel.IN_APP)
                .priority(NotificationPriority.HIGH)
                .title("New Alert")
                .message("This is a test alert")
                .build();

        Notification notification = Notification.builder()
                .id("notif-123")
                .recipientId("user-123")
                .type(NotificationType.ALERT)
                .channel(NotificationChannel.IN_APP)
                .priority(NotificationPriority.HIGH)
                .title("New Alert")
                .message("This is a test alert")
                .status(NotificationStatus.PENDING)
                .retryCount(0)
                .build();

        Notification savedNotification = Notification.builder()
                .id("notif-123")
                .recipientId("user-123")
                .type(NotificationType.ALERT)
                .channel(NotificationChannel.IN_APP)
                .priority(NotificationPriority.HIGH)
                .title("New Alert")
                .message("This is a test alert")
                .status(NotificationStatus.SENT)
                .retryCount(0)
                .build();

        // NotificationRequest has no equals(): match any instance deserialised from the JSON body
        when(notificationMapper.toEntity(any(NotificationRequest.class))).thenReturn(notification);
        when(notificationRepository.save(notification)).thenReturn(savedNotification);
        when(notificationRepository.findById("notif-123")).thenReturn(java.util.Optional.of(savedNotification));
        when(notificationMapper.toResponse(savedNotification)).thenReturn(sampleNotification);

        // When & Then
        mockMvc.perform(post("/api/v1/notifications")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated());
    }

    @Test
    void testSendNotificationWithInvalidRequest() throws Exception {
        // Given - Missing required fields
        NotificationRequest invalidRequest = NotificationRequest.builder()
                .recipientId("")  // Blank - invalid
                .type(NotificationType.ALERT)
                .channel(NotificationChannel.IN_APP)
                .title("")  // Blank - invalid
                .message("")  // Blank - invalid
                .build();

        // When & Then
        mockMvc.perform(post("/api/v1/notifications")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalidRequest)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void testGetUnreadCount() throws Exception {
        // Given
        String userId = "user-123";
        NotificationCountResponse countResponse = NotificationCountResponse.builder()
                .userId(userId)
                .unreadCount(5L)
                .totalCount(10L)
                .build();

        when(notificationRepository.countByRecipientIdAndStatus(userId, NotificationStatus.SENT)).thenReturn(5L);
        when(notificationRepository.findByRecipientId(userId, Pageable.unpaged())).thenReturn(Collections.emptyList());
        when(notificationMapper.toCountResponse(userId, 5L, 0L)).thenReturn(countResponse);

        // When & Then
        mockMvc.perform(get("/api/v1/notifications/user/{userId}/unread/count", userId))
                .andExpect(status().isOk());
    }

    @Test
    void testDeleteNotification() throws Exception {
        // Given
        when(notificationRepository.findById("notif-123")).thenReturn(Optional.empty());

        // When & Then
        mockMvc.perform(delete("/api/v1/notifications/{id}", "notif-123"))
                .andExpect(status().isNotFound());
    }

    @Test
    void testRetryNotification() throws Exception {
        // Given
        when(notificationRepository.findById("notif-123")).thenReturn(Optional.empty());

        // When & Then
        mockMvc.perform(post("/api/v1/notifications/{id}/retry", "notif-123"))
                .andExpect(status().isNotFound());
    }

    @Test
    void testGetNotificationsWithStatusFilter() throws Exception {
        // Given
        String userId = "user-123";
        when(notificationRepository.findByRecipientIdAndStatus(userId, NotificationStatus.SENT))
                .thenReturn(Collections.emptyList());

        // When & Then
        mockMvc.perform(get("/api/v1/notifications/user/{userId}", userId)
                        .param("status", "SENT"))
                .andExpect(status().isOk());
    }

    @Test
    void testGetNotificationsWithPagination() throws Exception {
        // Given
        when(notificationRepository.findAll(any(org.springframework.data.domain.Example.class), any(Pageable.class)))
                .thenReturn(new PageImpl<>(Collections.emptyList(), PageRequest.of(0, 5), 0));

        // When & Then
        mockMvc.perform(get("/api/v1/notifications")
                        .param("page", "0")
                        .param("size", "5"))
                .andExpect(status().isOk());
    }
}
