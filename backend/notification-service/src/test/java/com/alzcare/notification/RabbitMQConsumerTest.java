package com.alzcare.notification;

import com.alzcare.notification.dto.NotificationEvent;
import com.alzcare.notification.dto.NotificationPayload;
import com.alzcare.notification.entities.Notification;
import com.alzcare.notification.repositories.NotificationRepository;
import org.junit.jupiter.api.Test;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.MongoDBContainer;
import org.testcontainers.containers.RabbitMQContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

/**
 * Integration tests for RabbitMQ consumer functionality.
 * Tests that messages sent to RabbitMQ are consumed and notifications are created in MongoDB.
 */
@SpringBootTest
@Testcontainers
@ActiveProfiles("dev")
public class RabbitMQConsumerTest {

    @Container
    static MongoDBContainer mongoDBContainer = new MongoDBContainer(DockerImageName.parse("mongo:7.0"));

    @Container
    static RabbitMQContainer rabbitMQContainer = new RabbitMQContainer(DockerImageName.parse("rabbitmq:3.12-management"));

    @DynamicPropertySource
    static void setProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.data.mongodb.uri", mongoDBContainer::getReplicaSetUrl);
        registry.add("spring.rabbitmq.host", rabbitMQContainer::getHost);
        registry.add("spring.rabbitmq.port", rabbitMQContainer::getAmqpPort);
        registry.add("spring.rabbitmq.username", rabbitMQContainer::getAdminUsername);
        registry.add("spring.rabbitmq.password", rabbitMQContainer::getAdminPassword);
    }

    @Autowired
    private RabbitTemplate rabbitTemplate;

    @Autowired
    private NotificationRepository notificationRepository;

    @Test
    void testSendMessageToQueueAndVerifyNotificationCreated() {
        // Clear any existing notifications
        notificationRepository.deleteAll();

        // Given - Create a notification event
        String eventId = UUID.randomUUID().toString();
        String recipientId = "patient-123";

        NotificationPayload payload = NotificationPayload.builder()
                .alertId("alert-001")
                .patientId("patient-123")
                .recipientId(recipientId)
                .severity("HIGH")
                .ruleCode("FALL_DETECTED")
                .message("Fall detected in living room")
                .channel("IN_APP")
                .build();

        NotificationEvent event = NotificationEvent.builder()
                .eventId(eventId)
                .eventType("AlertCreated")
                .source("safety-alert-engine")
                .timestamp(Instant.now())
                .payload(payload)
                .build();

        // When - Send message to exchange
        rabbitTemplate.convertAndSend("alert.notifications", "notification.routing.key", event);

        // Then - Wait for and verify notification was created in MongoDB
        await().atMost(Duration.ofSeconds(10))
                .pollInterval(Duration.ofMillis(500))
                .untilAsserted(() -> {
                    List<Notification> notifications = notificationRepository.findByRecipientId(recipientId, org.springframework.data.domain.Pageable.unpaged());
                    assertThat(notifications).isNotEmpty();
                    assertThat(notifications).anyMatch(n -> 
                        n.getTitle() != null && n.getTitle().contains("Fall")
                    );
                });
    }

    @Test
    void testSendMultipleMessagesAndVerifyAllProcessed() {
        // Clear existing notifications
        notificationRepository.deleteAll();

        String recipientId = "patient-multi";

        // Send multiple events
        for (int i = 0; i < 3; i++) {
            NotificationPayload payload = NotificationPayload.builder()
                    .alertId("alert-" + i)
                    .patientId(recipientId)
                    .recipientId(recipientId)
                    .severity("MEDIUM")
                    .ruleCode("WANDERING")
                    .message("Wandering detected - instance " + i)
                    .channel("IN_APP")
                    .build();

            NotificationEvent event = NotificationEvent.builder()
                    .eventId(UUID.randomUUID().toString())
                    .eventType("AlertCreated")
                    .source("safety-alert-engine")
                    .timestamp(Instant.now())
                    .payload(payload)
                    .build();

            rabbitTemplate.convertAndSend("alert.notifications", "notification.routing.key", event);
        }

        // Wait and verify all notifications were created
        await().atMost(Duration.ofSeconds(15))
                .pollInterval(Duration.ofMillis(500))
                .untilAsserted(() -> {
                    List<Notification> notifications = notificationRepository.findByRecipientId(recipientId, org.springframework.data.domain.Pageable.unpaged());
                    assertThat(notifications).hasSizeGreaterThanOrEqualTo(3);
                });
    }

    @Test
    void testSendCriticalAlertAndVerifyNotificationCreated() {
        // Clear existing notifications
        notificationRepository.deleteAll();

        String recipientId = "patient-critical";

        NotificationPayload payload = NotificationPayload.builder()
                .alertId("alert-critical-001")
                .patientId(recipientId)
                .recipientId(recipientId)
                .severity("CRITICAL")
                .ruleCode("EMERGENCY_BUTTON")
                .message("Emergency button pressed!")
                .channel("SMS")
                .build();

        NotificationEvent event = NotificationEvent.builder()
                .eventId(UUID.randomUUID().toString())
                .eventType("EscalationNotice")
                .source("safety-alert-engine")
                .timestamp(Instant.now())
                .payload(payload)
                .build();

        rabbitTemplate.convertAndSend("alert.notifications", "notification.routing.key", event);

        // Wait and verify notification was created
        await().atMost(Duration.ofSeconds(10))
                .pollInterval(Duration.ofMillis(500))
                .untilAsserted(() -> {
                    List<Notification> notifications = notificationRepository.findByRecipientId(recipientId, org.springframework.data.domain.Pageable.unpaged());
                    assertThat(notifications).isNotEmpty();
                    
                    Notification notification = notifications.get(0);
                    assertThat(notification.getMessage()).contains("Emergency");
                });
    }
}
