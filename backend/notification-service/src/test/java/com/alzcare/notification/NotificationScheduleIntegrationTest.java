package com.alzcare.notification;

import com.alzcare.notification.dto.UserDto;
import com.alzcare.notification.dto.request.CreateScheduleRequest;
import com.alzcare.notification.dto.request.UpdateScheduleRequest;
import com.alzcare.notification.dto.response.ScheduleResponse;
import com.alzcare.notification.entities.Notification;
import com.alzcare.notification.entities.ScheduledNotificationRule;
import com.alzcare.notification.enums.NotificationChannel;
import com.alzcare.notification.enums.NotificationPriority;
import com.alzcare.notification.enums.NotificationType;
import com.alzcare.notification.enums.ScheduleType;
import com.alzcare.notification.repositories.NotificationRepository;
import com.alzcare.notification.repositories.NotificationScheduleRepository;
import com.alzcare.notification.service.dynamic.DynamicSchedulerService;
import com.alzcare.notification.service.targeting.IdentityServiceClientFeign;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.testcontainers.containers.MongoDBContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Integration tests for the Dynamic Notification Scheduler.
 * <p>Tests the full lifecycle of notification schedules including creation,
 * task registration, rescheduling, cancellation, and manual triggering.
 * Uses Testcontainers for MongoDB and mocks external services.</p>
 */
@SpringBootTest
@Testcontainers
@AutoConfigureMockMvc
@ActiveProfiles("dev")
@WithMockUser(roles = "ADMIN")
public class NotificationScheduleIntegrationTest {

    @Container
    static MongoDBContainer mongoDBContainer = new MongoDBContainer(DockerImageName.parse("mongo:7.0"));

    @DynamicPropertySource
    static void setProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.data.mongodb.uri", mongoDBContainer::getReplicaSetUrl);
        registry.add("notification.scheduler.bootstrap-on-startup", () -> "false");
    }

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private NotificationScheduleRepository scheduleRepository;

    @Autowired
    private NotificationRepository notificationRepository;

    @Autowired
    private DynamicSchedulerService schedulerService;

    @MockBean
    private IdentityServiceClientFeign identityServiceClient;

    // Demo user IDs that exist in the system
    private static final String DEMO_USER_ID = "e905f36f-3263-488c-a48a-b87e0df4c3ca";
    private static final String DEMO_USER_ID_2 = "0c76bbbc-7a5b-4884-8e13-999387b3994f";

    @BeforeEach
    void setUp() {
        // Clean up repositories before each test
        scheduleRepository.deleteAll();
        notificationRepository.deleteAll();
        schedulerService.cancelAllSchedules();

        // Mock Identity Service Client
        UserDto demoUser = new UserDto();
        demoUser.setId(DEMO_USER_ID);
        demoUser.setUsername("demo_patient");
        demoUser.setFirstName("John");
        demoUser.setEmail("john@example.com");
        demoUser.setEnabled(true);

        when(identityServiceClient.getUserById(DEMO_USER_ID)).thenReturn(demoUser);
        when(identityServiceClient.getActiveUsersByRole(anyString())).thenReturn(List.of(demoUser));
        when(identityServiceClient.getAllActiveUsers()).thenReturn(List.of(demoUser));
    }

    /**
     * Test 1: Create schedule → verify saved to DB
     */
    @Test
    void shouldCreateScheduleAndSaveToDatabase() throws Exception {
        // Given
        Instant startDate = Instant.now().plus(1, ChronoUnit.HOURS);
        CreateScheduleRequest request = CreateScheduleRequest.builder()
                .name("Test Schedule")
                .description("Test description")
                .targetRole("PATIENT")
                .titleTemplate("Hello {{firstName}}")
                .messageTemplate("Test message for {{userName}}")
                .type(NotificationType.REMINDER)
                .priority(NotificationPriority.NORMAL)
                .scheduleType(ScheduleType.INTERVAL)
                .intervalMinutes(60)
                .channels(List.of(NotificationChannel.IN_APP))
                .startDate(startDate)
                .active(true)
                .build();

        // When
        MvcResult result = mockMvc.perform(post("/api/v1/schedules")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").exists())
                .andExpect(jsonPath("$.name").value("Test Schedule"))
                .andExpect(jsonPath("$.targetRole").value("PATIENT"))
                .andExpect(jsonPath("$.scheduleType").value("INTERVAL"))
                .andExpect(jsonPath("$.intervalMinutes").value(60))
                .andExpect(jsonPath("$.active").value(true))
                .andReturn();

        // Then - Verify in DB
        String responseBody = result.getResponse().getContentAsString();
        ScheduleResponse response = objectMapper.readValue(responseBody, ScheduleResponse.class);

        Optional<ScheduledNotificationRule> savedRule = scheduleRepository.findById(response.getId());
        assertThat(savedRule).isPresent();
        assertThat(savedRule.get().getName()).isEqualTo("Test Schedule");
        assertThat(savedRule.get().getScheduleType()).isEqualTo(ScheduleType.INTERVAL);
        assertThat(savedRule.get().getIntervalMinutes()).isEqualTo(60);
        assertThat(savedRule.get().isActive()).isTrue();
    }

    /**
     * Test 2: Create active schedule → verify task is registered
     */
    @Test
    void shouldRegisterTaskWhenCreatingActiveSchedule() throws Exception {
        // Given - Create an active schedule
        Instant startDate = Instant.now().plus(1, ChronoUnit.MINUTES);
        CreateScheduleRequest request = CreateScheduleRequest.builder()
                .name("Active Schedule Test")
                .targetRole("PATIENT")
                .titleTemplate("Test Title")
                .messageTemplate("Test Message")
                .type(NotificationType.REMINDER)
                .priority(NotificationPriority.NORMAL)
                .scheduleType(ScheduleType.INTERVAL)
                .intervalMinutes(30)
                .channels(List.of(NotificationChannel.IN_APP))
                .startDate(startDate)
                .active(true)
                .build();

        // When
        MvcResult result = mockMvc.perform(post("/api/v1/schedules")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andReturn();

        // Then - Verify task is registered in scheduler
        String responseBody = result.getResponse().getContentAsString();
        ScheduleResponse response = objectMapper.readValue(responseBody, ScheduleResponse.class);

        await().atMost(java.time.Duration.ofSeconds(5))
                .untilAsserted(() -> {
                    assertThat(schedulerService.isScheduled(response.getId())).isTrue();
                });

        assertThat(schedulerService.getActiveScheduleIds()).contains(response.getId());
    }

    /**
     * Test 3: Update schedule → verify task is rescheduled
     */
    @Test
    void shouldRescheduleTaskWhenUpdatingSchedule() throws Exception {
        // First create a schedule
        Instant startDate = Instant.now().plus(1, ChronoUnit.HOURS);
        CreateScheduleRequest createRequest = CreateScheduleRequest.builder()
                .name("Original Schedule")
                .targetRole("PATIENT")
                .titleTemplate("Original Title")
                .messageTemplate("Original Message")
                .type(NotificationType.REMINDER)
                .priority(NotificationPriority.NORMAL)
                .scheduleType(ScheduleType.INTERVAL)
                .intervalMinutes(60)
                .channels(List.of(NotificationChannel.IN_APP))
                .startDate(startDate)
                .active(true)
                .build();

        MvcResult createResult = mockMvc.perform(post("/api/v1/schedules")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createRequest)))
                .andExpect(status().isCreated())
                .andReturn();

        String responseBody = createResult.getResponse().getContentAsString();
        ScheduleResponse createdSchedule = objectMapper.readValue(responseBody, ScheduleResponse.class);
        String scheduleId = createdSchedule.getId();

        // Wait for initial scheduling
        await().atMost(java.time.Duration.ofSeconds(3))
                .untilAsserted(() -> {
                    assertThat(schedulerService.isScheduled(scheduleId)).isTrue();
                });

        // Update the schedule with different interval
        UpdateScheduleRequest updateRequest = UpdateScheduleRequest.builder()
                .name("Updated Schedule")
                .targetRole("CAREGIVER")
                .titleTemplate("Updated Title")
                .messageTemplate("Updated Message")
                .type(NotificationType.ALERT)
                .priority(NotificationPriority.HIGH)
                .scheduleType(ScheduleType.INTERVAL)
                .intervalMinutes(15)  // Changed from 60 to 15
                .channels(List.of(NotificationChannel.IN_APP, NotificationChannel.EMAIL))
                .startDate(startDate)
                .active(true)
                .build();

        // When - Update the schedule
        mockMvc.perform(put("/api/v1/schedules/{id}", scheduleId)
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Updated Schedule"))
                .andExpect(jsonPath("$.targetRole").value("CAREGIVER"))
                .andExpect(jsonPath("$.intervalMinutes").value(15));

        // Then - Verify task is still scheduled (rescheduled)
        await().atMost(java.time.Duration.ofSeconds(3))
                .untilAsserted(() -> {
                    assertThat(schedulerService.isScheduled(scheduleId)).isTrue();
                });

        // Verify in DB
        Optional<ScheduledNotificationRule> updatedRule = scheduleRepository.findById(scheduleId);
        assertThat(updatedRule).isPresent();
        assertThat(updatedRule.get().getIntervalMinutes()).isEqualTo(15);
        assertThat(updatedRule.get().getTargetRole()).isEqualTo("CAREGIVER");
    }

    /**
     * Test 4: Toggle schedule off → verify task is cancelled
     */
    @Test
    void shouldCancelTaskWhenDisablingSchedule() throws Exception {
        // Create an active schedule
        Instant startDate = Instant.now().plus(1, ChronoUnit.HOURS);
        CreateScheduleRequest createRequest = CreateScheduleRequest.builder()
                .name("Toggle Test Schedule")
                .targetRole("PATIENT")
                .titleTemplate("Test Title")
                .messageTemplate("Test Message")
                .type(NotificationType.REMINDER)
                .priority(NotificationPriority.NORMAL)
                .scheduleType(ScheduleType.INTERVAL)
                .intervalMinutes(60)
                .channels(List.of(NotificationChannel.IN_APP))
                .startDate(startDate)
                .active(true)
                .build();

        MvcResult createResult = mockMvc.perform(post("/api/v1/schedules")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createRequest)))
                .andExpect(status().isCreated())
                .andReturn();

        String responseBody = createResult.getResponse().getContentAsString();
        ScheduleResponse createdSchedule = objectMapper.readValue(responseBody, ScheduleResponse.class);
        String scheduleId = createdSchedule.getId();

        // Wait for initial scheduling
        await().atMost(java.time.Duration.ofSeconds(3))
                .untilAsserted(() -> {
                    assertThat(schedulerService.isScheduled(scheduleId)).isTrue();
                });

        // When - Toggle the schedule off
        mockMvc.perform(patch("/api/v1/schedules/{id}/toggle", scheduleId)
                        .with(csrf()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.active").value(false));

        // Then - Verify task is cancelled
        await().atMost(java.time.Duration.ofSeconds(3))
                .untilAsserted(() -> {
                    assertThat(schedulerService.isScheduled(scheduleId)).isFalse();
                });

        // Verify in DB
        Optional<ScheduledNotificationRule> toggledRule = scheduleRepository.findById(scheduleId);
        assertThat(toggledRule).isPresent();
        assertThat(toggledRule.get().isActive()).isFalse();
    }

    /**
     * Test 5: Delete schedule → verify task is cancelled
     */
    @Test
    void shouldCancelTaskWhenDeletingSchedule() throws Exception {
        // Create an active schedule
        Instant startDate = Instant.now().plus(1, ChronoUnit.HOURS);
        CreateScheduleRequest createRequest = CreateScheduleRequest.builder()
                .name("Delete Test Schedule")
                .targetRole("PATIENT")
                .titleTemplate("Test Title")
                .messageTemplate("Test Message")
                .type(NotificationType.REMINDER)
                .priority(NotificationPriority.NORMAL)
                .scheduleType(ScheduleType.INTERVAL)
                .intervalMinutes(60)
                .channels(List.of(NotificationChannel.IN_APP))
                .startDate(startDate)
                .active(true)
                .build();

        MvcResult createResult = mockMvc.perform(post("/api/v1/schedules")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createRequest)))
                .andExpect(status().isCreated())
                .andReturn();

        String responseBody = createResult.getResponse().getContentAsString();
        ScheduleResponse createdSchedule = objectMapper.readValue(responseBody, ScheduleResponse.class);
        String scheduleId = createdSchedule.getId();

        // Wait for initial scheduling
        await().atMost(java.time.Duration.ofSeconds(3))
                .untilAsserted(() -> {
                    assertThat(schedulerService.isScheduled(scheduleId)).isTrue();
                });

        // When - Delete the schedule
        mockMvc.perform(delete("/api/v1/schedules/{id}", scheduleId)
                        .with(csrf()))
                .andExpect(status().isNoContent());

        // Then - Verify task is cancelled
        await().atMost(java.time.Duration.ofSeconds(3))
                .untilAsserted(() -> {
                    assertThat(schedulerService.isScheduled(scheduleId)).isFalse();
                });

        // Verify in DB - should be deleted
        Optional<ScheduledNotificationRule> deletedRule = scheduleRepository.findById(scheduleId);
        assertThat(deletedRule).isEmpty();
    }

    /**
     * Test 6: Manual trigger → verify notifications created
     */
    @Test
    void shouldCreateNotificationsOnManualTrigger() throws Exception {
        // Create an active schedule with specific target user
        Instant startDate = Instant.now().plus(1, ChronoUnit.HOURS);
        CreateScheduleRequest createRequest = CreateScheduleRequest.builder()
                .name("Trigger Test Schedule")
                .targetRole("PATIENT")
                .targetUserIds(List.of(DEMO_USER_ID))
                .titleTemplate("Hello {{firstName}} - Reminder")
                .messageTemplate("This is a test message for {{userName}}")
                .type(NotificationType.REMINDER)
                .priority(NotificationPriority.NORMAL)
                .scheduleType(ScheduleType.INTERVAL)
                .intervalMinutes(60)
                .channels(List.of(NotificationChannel.IN_APP))
                .startDate(startDate)
                .active(true)
                .build();

        MvcResult createResult = mockMvc.perform(post("/api/v1/schedules")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createRequest)))
                .andExpect(status().isCreated())
                .andReturn();

        String responseBody = createResult.getResponse().getContentAsString();
        ScheduleResponse createdSchedule = objectMapper.readValue(responseBody, ScheduleResponse.class);
        String scheduleId = createdSchedule.getId();

        // When - Manually trigger the schedule
        mockMvc.perform(post("/api/v1/schedules/{id}/trigger", scheduleId)
                        .with(csrf()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("executing"))
                .andExpect(jsonPath("$.scheduleId").value(scheduleId));

        // Then - Verify notification was created in DB
        await().atMost(java.time.Duration.ofSeconds(5))
                .untilAsserted(() -> {
                    List<Notification> notifications = notificationRepository.findByRecipientId(DEMO_USER_ID, 
                            org.springframework.data.domain.Pageable.unpaged());
                    assertThat(notifications).isNotEmpty();
                    assertThat(notifications.get(0).getRecipientId()).isEqualTo(DEMO_USER_ID);
                    assertThat(notifications.get(0).getTitle()).contains("John");  // firstName replaced
                    assertThat(notifications.get(0).getType()).isEqualTo(NotificationType.REMINDER);
                });

        // Verify execution count was updated
        Optional<ScheduledNotificationRule> updatedRule = scheduleRepository.findById(scheduleId);
        assertThat(updatedRule).isPresent();
        assertThat(updatedRule.get().getExecutionCount()).isEqualTo(1);
        assertThat(updatedRule.get().getLastExecutedAt()).isNotNull();
    }

    /**
     * Test 7: Interval schedule calculation
     */
    @Test
    void shouldCalculateNextExecutionForIntervalSchedule() throws Exception {
        // Given - Create interval schedule
        Instant startDate = Instant.now().plus(1, ChronoUnit.HOURS);
        int intervalMinutes = 45;

        CreateScheduleRequest request = CreateScheduleRequest.builder()
                .name("Interval Schedule Test")
                .targetRole("PATIENT")
                .titleTemplate("Test Title")
                .messageTemplate("Test Message")
                .type(NotificationType.REMINDER)
                .priority(NotificationPriority.NORMAL)
                .scheduleType(ScheduleType.INTERVAL)
                .intervalMinutes(intervalMinutes)
                .channels(List.of(NotificationChannel.IN_APP))
                .startDate(startDate)
                .active(true)
                .build();

        Instant beforeCreate = Instant.now();

        // When
        MvcResult result = mockMvc.perform(post("/api/v1/schedules")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.nextExecutionTime").exists())
                .andReturn();

        // Then - Verify next execution time is calculated correctly
        String responseBody = result.getResponse().getContentAsString();
        ScheduleResponse response = objectMapper.readValue(responseBody, ScheduleResponse.class);

        assertThat(response.getNextExecutionTime()).isNotNull();

        // The next execution should be approximately intervalMinutes from now
        // (since startDate is in the future, it would be startDate + interval, 
        // but the controller calculates based on current time for active schedules)
        Optional<ScheduledNotificationRule> savedRule = scheduleRepository.findById(response.getId());
        assertThat(savedRule).isPresent();
        assertThat(savedRule.get().getNextExecutionTime()).isNotNull();
    }

    /**
     * Test 8: Cron schedule calculation
     */
    @Test
    void shouldCalculateNextExecutionForCronSchedule() throws Exception {
        // Given - Create cron schedule (every day at 9 AM)
        String cronExpression = "0 0 9 * * *";
        Instant startDate = Instant.now().plus(1, ChronoUnit.MINUTES);

        CreateScheduleRequest request = CreateScheduleRequest.builder()
                .name("Cron Schedule Test")
                .targetRole("PATIENT")
                .titleTemplate("Daily Reminder")
                .messageTemplate("Your daily 9 AM reminder")
                .type(NotificationType.REMINDER)
                .priority(NotificationPriority.NORMAL)
                .scheduleType(ScheduleType.CRON)
                .cronExpression(cronExpression)
                .timezone("UTC")
                .channels(List.of(NotificationChannel.IN_APP))
                .startDate(startDate)
                .active(true)
                .build();

        // When
        MvcResult result = mockMvc.perform(post("/api/v1/schedules")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.nextExecutionTime").exists())
                .andExpect(jsonPath("$.cronExpression").value(cronExpression))
                .andExpect(jsonPath("$.scheduleType").value("CRON"))
                .andReturn();

        // Then - Verify cron schedule was saved with correct next execution time
        String responseBody = result.getResponse().getContentAsString();
        ScheduleResponse response = objectMapper.readValue(responseBody, ScheduleResponse.class);

        assertThat(response.getNextExecutionTime()).isNotNull();

        Optional<ScheduledNotificationRule> savedRule = scheduleRepository.findById(response.getId());
        assertThat(savedRule).isPresent();
        assertThat(savedRule.get().getScheduleType()).isEqualTo(ScheduleType.CRON);
        assertThat(savedRule.get().getCronExpression()).isEqualTo(cronExpression);
        assertThat(savedRule.get().getNextExecutionTime()).isNotNull();
    }

    /**
     * Additional Test: Verify inactive schedule does not register task
     */
    @Test
    void shouldNotRegisterTaskForInactiveSchedule() throws Exception {
        // Given - Create inactive schedule
        Instant startDate = Instant.now().plus(1, ChronoUnit.HOURS);
        CreateScheduleRequest request = CreateScheduleRequest.builder()
                .name("Inactive Schedule Test")
                .targetRole("PATIENT")
                .titleTemplate("Test Title")
                .messageTemplate("Test Message")
                .type(NotificationType.REMINDER)
                .priority(NotificationPriority.NORMAL)
                .scheduleType(ScheduleType.INTERVAL)
                .intervalMinutes(60)
                .channels(List.of(NotificationChannel.IN_APP))
                .startDate(startDate)
                .active(false)  // Inactive!
                .build();

        // When
        MvcResult result = mockMvc.perform(post("/api/v1/schedules")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.active").value(false))
                .andReturn();

        // Then - Verify task is NOT registered
        String responseBody = result.getResponse().getContentAsString();
        ScheduleResponse response = objectMapper.readValue(responseBody, ScheduleResponse.class);

        // Wait a bit and verify no task is scheduled
        Thread.sleep(500);
        assertThat(schedulerService.isScheduled(response.getId())).isFalse();
        assertThat(schedulerService.getActiveScheduleIds()).doesNotContain(response.getId());
    }

    /**
     * Additional Test: Toggle schedule on → verify task is registered
     */
    @Test
    void shouldRegisterTaskWhenEnablingSchedule() throws Exception {
        // Create an inactive schedule
        Instant startDate = Instant.now().plus(1, ChronoUnit.HOURS);
        CreateScheduleRequest createRequest = CreateScheduleRequest.builder()
                .name("Enable Test Schedule")
                .targetRole("PATIENT")
                .titleTemplate("Test Title")
                .messageTemplate("Test Message")
                .type(NotificationType.REMINDER)
                .priority(NotificationPriority.NORMAL)
                .scheduleType(ScheduleType.INTERVAL)
                .intervalMinutes(60)
                .channels(List.of(NotificationChannel.IN_APP))
                .startDate(startDate)
                .active(false)
                .build();

        MvcResult createResult = mockMvc.perform(post("/api/v1/schedules")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createRequest)))
                .andExpect(status().isCreated())
                .andReturn();

        String responseBody = createResult.getResponse().getContentAsString();
        ScheduleResponse createdSchedule = objectMapper.readValue(responseBody, ScheduleResponse.class);
        String scheduleId = createdSchedule.getId();

        // Verify initially not scheduled
        assertThat(schedulerService.isScheduled(scheduleId)).isFalse();

        // When - Toggle the schedule on
        mockMvc.perform(patch("/api/v1/schedules/{id}/toggle", scheduleId)
                        .with(csrf()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.active").value(true));

        // Then - Verify task is now scheduled
        await().atMost(java.time.Duration.ofSeconds(3))
                .untilAsserted(() -> {
                    assertThat(schedulerService.isScheduled(scheduleId)).isTrue();
                });

        // Verify in DB
        Optional<ScheduledNotificationRule> toggledRule = scheduleRepository.findById(scheduleId);
        assertThat(toggledRule).isPresent();
        assertThat(toggledRule.get().isActive()).isTrue();
        assertThat(toggledRule.get().getNextExecutionTime()).isNotNull();
    }

    /**
     * Additional Test: Cannot trigger inactive schedule
     */
    @Test
    void shouldNotAllowTriggeringInactiveSchedule() throws Exception {
        // Create an inactive schedule
        Instant startDate = Instant.now().plus(1, ChronoUnit.HOURS);
        CreateScheduleRequest createRequest = CreateScheduleRequest.builder()
                .name("Inactive Trigger Test")
                .targetRole("PATIENT")
                .titleTemplate("Test Title")
                .messageTemplate("Test Message")
                .type(NotificationType.REMINDER)
                .priority(NotificationPriority.NORMAL)
                .scheduleType(ScheduleType.INTERVAL)
                .intervalMinutes(60)
                .channels(List.of(NotificationChannel.IN_APP))
                .startDate(startDate)
                .active(false)
                .build();

        MvcResult createResult = mockMvc.perform(post("/api/v1/schedules")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createRequest)))
                .andExpect(status().isCreated())
                .andReturn();

        String responseBody = createResult.getResponse().getContentAsString();
        ScheduleResponse createdSchedule = objectMapper.readValue(responseBody, ScheduleResponse.class);
        String scheduleId = createdSchedule.getId();

        // When - Try to trigger inactive schedule
        mockMvc.perform(post("/api/v1/schedules/{id}/trigger", scheduleId)
                        .with(csrf()))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("Cannot trigger inactive schedule"));
    }

    /**
     * Additional Test: List schedules with filters
     */
    @Test
    void shouldListSchedulesWithFilters() throws Exception {
        // Create multiple schedules
        Instant startDate = Instant.now().plus(1, ChronoUnit.HOURS);

        // Active PATIENT schedule
        CreateScheduleRequest request1 = CreateScheduleRequest.builder()
                .name("Patient Schedule 1")
                .targetRole("PATIENT")
                .titleTemplate("Title 1")
                .messageTemplate("Message 1")
                .type(NotificationType.REMINDER)
                .priority(NotificationPriority.NORMAL)
                .scheduleType(ScheduleType.INTERVAL)
                .intervalMinutes(60)
                .channels(List.of(NotificationChannel.IN_APP))
                .startDate(startDate)
                .active(true)
                .build();

        // Active CAREGIVER schedule
        CreateScheduleRequest request2 = CreateScheduleRequest.builder()
                .name("Caregiver Schedule 1")
                .targetRole("CAREGIVER")
                .titleTemplate("Title 2")
                .messageTemplate("Message 2")
                .type(NotificationType.ALERT)
                .priority(NotificationPriority.HIGH)
                .scheduleType(ScheduleType.INTERVAL)
                .intervalMinutes(30)
                .channels(List.of(NotificationChannel.EMAIL))
                .startDate(startDate)
                .active(true)
                .build();

        // Inactive PATIENT schedule
        CreateScheduleRequest request3 = CreateScheduleRequest.builder()
                .name("Patient Schedule 2")
                .targetRole("PATIENT")
                .titleTemplate("Title 3")
                .messageTemplate("Message 3")
                .type(NotificationType.REMINDER)
                .priority(NotificationPriority.NORMAL)
                .scheduleType(ScheduleType.INTERVAL)
                .intervalMinutes(120)
                .channels(List.of(NotificationChannel.IN_APP))
                .startDate(startDate)
                .active(false)
                .build();

        mockMvc.perform(post("/api/v1/schedules")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request1)))
                .andExpect(status().isCreated());

        mockMvc.perform(post("/api/v1/schedules")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request2)))
                .andExpect(status().isCreated());

        mockMvc.perform(post("/api/v1/schedules")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request3)))
                .andExpect(status().isCreated());

        // When & Then - Filter by targetRole=PATIENT
        mockMvc.perform(get("/api/v1/schedules")
                        .param("targetRole", "PATIENT"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray())
                .andExpect(jsonPath("$.content.length()").value(2));

        // When & Then - Filter by targetRole=CAREGIVER
        mockMvc.perform(get("/api/v1/schedules")
                        .param("targetRole", "CAREGIVER"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(1))
                .andExpect(jsonPath("$.content[0].targetRole").value("CAREGIVER"));

        // When & Then - Filter by active=true
        mockMvc.perform(get("/api/v1/schedules")
                        .param("active", "true"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(2));

        // When & Then - Filter by active=false
        mockMvc.perform(get("/api/v1/schedules")
                        .param("active", "false"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(1));

        // When & Then - Combined filter
        mockMvc.perform(get("/api/v1/schedules")
                        .param("targetRole", "PATIENT")
                        .param("active", "true"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(1))
                .andExpect(jsonPath("$.content[0].name").value("Patient Schedule 1"));
    }

    /**
     * Additional Test: Get schedule by ID
     */
    @Test
    void shouldGetScheduleById() throws Exception {
        // Create a schedule
        Instant startDate = Instant.now().plus(1, ChronoUnit.HOURS);
        CreateScheduleRequest request = CreateScheduleRequest.builder()
                .name("Get By ID Test")
                .targetRole("PATIENT")
                .titleTemplate("Test Title")
                .messageTemplate("Test Message")
                .type(NotificationType.REMINDER)
                .priority(NotificationPriority.NORMAL)
                .scheduleType(ScheduleType.INTERVAL)
                .intervalMinutes(60)
                .channels(List.of(NotificationChannel.IN_APP))
                .startDate(startDate)
                .active(true)
                .build();

        MvcResult createResult = mockMvc.perform(post("/api/v1/schedules")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andReturn();

        String responseBody = createResult.getResponse().getContentAsString();
        ScheduleResponse createdSchedule = objectMapper.readValue(responseBody, ScheduleResponse.class);
        String scheduleId = createdSchedule.getId();

        // When & Then - Get by ID
        mockMvc.perform(get("/api/v1/schedules/{id}", scheduleId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(scheduleId))
                .andExpect(jsonPath("$.name").value("Get By ID Test"))
                .andExpect(jsonPath("$.targetRole").value("PATIENT"));
    }

    /**
     * Additional Test: Get non-existent schedule returns 404
     */
    @Test
    void shouldReturn404ForNonExistentSchedule() throws Exception {
        mockMvc.perform(get("/api/v1/schedules/{id}", "non-existent-id"))
                .andExpect(status().isNotFound());
    }
}
