package com.alzcare.notification.service;

import com.alzcare.notification.dto.NotificationEvent;
import com.alzcare.notification.dto.NotificationPayload;
import com.alzcare.notification.entities.Notification;
import com.alzcare.notification.enums.NotificationChannel;
import com.alzcare.notification.enums.NotificationPriority;
import com.alzcare.notification.enums.NotificationStatus;
import com.alzcare.notification.enums.NotificationType;
import com.alzcare.notification.repositories.NotificationRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Service for processing notification events.
 * <p>Determines priority, creates notification documents, and routes
 * to appropriate channel handlers for delivery.</p>
 */
@Service
@Slf4j
public class NotificationProcessor {

    private final NotificationRepository notificationRepository;
    private final NotificationRoutingService routingService;

    /**
     * Constructs a new NotificationProcessor.
     *
     * @param notificationRepository the notification repository
     * @param routingService the notification routing service
     */
    public NotificationProcessor(NotificationRepository notificationRepository,
                                 NotificationRoutingService routingService) {
        this.notificationRepository = notificationRepository;
        this.routingService = routingService;
    }

    /**
     * Processes a notification event end-to-end.
     * <p>Determines priority, persists the notification, and routes for delivery.</p>
     *
     * @param event the notification event to process
     */
    public void processEvent(NotificationEvent event) {
        log.info("Processing notification event - eventId: {}, eventType: {}",
                event.getEventId(), event.getEventType());

        // 1. Determine priority based on event type and severity
        NotificationPriority priority = determinePriority(event);
        log.debug("Determined priority: {} for event: {}", priority, event.getEventId());

        // 2. Determine notification type based on event type
        NotificationType type = determineNotificationType(event);

        // 3. Create Notification document
        Notification notification = createNotification(event, priority, type);
        log.debug("Created notification document - id: {}, recipient: {}",
                notification.getId(), notification.getRecipientId());

        // 4. Save to MongoDB
        Notification savedNotification = notificationRepository.save(notification);
        log.info("Saved notification to MongoDB - id: {}, recipient: {}",
                savedNotification.getId(), savedNotification.getRecipientId());

        // 5. Route to appropriate channel handler
        routingService.routeNotification(savedNotification);
    }

    /**
     * Determines the priority level based on event type and severity.
     * <p>AlertCreated events with CRITICAL severity get highest priority.</p>
     *
     * @param event the notification event
     * @return the determined priority level
     */
    private NotificationPriority determinePriority(NotificationEvent event) {
        NotificationPayload payload = event.getPayload();
        String eventType = event.getEventType();
        String severity = payload != null ? payload.getSeverity() : null;

        // AlertCreated + CRITICAL → CRITICAL
        if ("AlertCreated".equals(eventType) && "CRITICAL".equalsIgnoreCase(severity)) {
            return NotificationPriority.CRITICAL;
        }

        // AlertCreated + HIGH → HIGH
        if ("AlertCreated".equals(eventType) && "HIGH".equalsIgnoreCase(severity)) {
            return NotificationPriority.HIGH;
        }

        // EscalationNotice → HIGH (escalations are important)
        if ("EscalationNotice".equals(eventType)) {
            return NotificationPriority.HIGH;
        }

        // Others → NORMAL
        return NotificationPriority.NORMAL;
    }

    /**
     * Determines the notification type based on event type.
     *
     * @param event the notification event
     * @return the determined notification type
     */
    private NotificationType determineNotificationType(NotificationEvent event) {
        String eventType = event.getEventType();

        if ("AlertCreated".equals(eventType)) {
            return NotificationType.ALERT;
        }

        if ("EscalationNotice".equals(eventType)) {
            return NotificationType.EMERGENCY;
        }

        return NotificationType.SYSTEM;
    }

    /**
     * Creates a Notification entity from the event data.
     *
     * @param event the notification event
     * @param priority the determined priority
     * @param type the determined notification type
     * @return the created notification entity
     */
    private Notification createNotification(NotificationEvent event, NotificationPriority priority, NotificationType type) {
        NotificationPayload payload = event.getPayload();

        // Build metadata map with all relevant event data
        Map<String, Object> metadata = new HashMap<>();
        if (payload.getMetadata() != null) {
            metadata.putAll(payload.getMetadata());
        }
        metadata.put("eventId", event.getEventId());
        metadata.put("eventType", event.getEventType());
        metadata.put("source", event.getSource());
        metadata.put("eventTimestamp", event.getTimestamp());
        if (payload.getAlertId() != null) {
            metadata.put("alertId", payload.getAlertId());
        }
        if (payload.getPatientId() != null) {
            metadata.put("patientId", payload.getPatientId());
        }
        if (payload.getRuleCode() != null) {
            metadata.put("ruleCode", payload.getRuleCode());
        }

        // Parse channel from payload
        NotificationChannel channel = parseChannel(payload.getChannel());

        // Build title from rule code or use default
        String title = buildTitle(payload);

        return Notification.builder()
                .id(UUID.randomUUID().toString())
                .recipientId(payload.getRecipientId())
                .type(type)
                .channel(channel)
                .status(NotificationStatus.PENDING)
                .priority(priority)
                .title(title)
                .message(payload.getMessage())
                .retryCount(0)
                .metadata(metadata)
                .build();
    }

    /**
     * Parses the channel string into NotificationChannel enum.
     *
     * @param channelStr the channel string from payload
     * @return the parsed NotificationChannel, defaults to IN_APP if invalid
     */
    private NotificationChannel parseChannel(String channelStr) {
        if (channelStr == null || channelStr.isBlank()) {
            return NotificationChannel.IN_APP;
        }

        try {
            return NotificationChannel.valueOf(channelStr.toUpperCase());
        } catch (IllegalArgumentException e) {
            log.warn("Unknown channel type: {}, defaulting to IN_APP", channelStr);
            return NotificationChannel.IN_APP;
        }
    }

    /**
     * Builds a title for the notification based on payload data.
     *
     * @param payload the notification payload
     * @return the generated title
     */
    private String buildTitle(NotificationPayload payload) {
        if (payload.getRuleCode() != null && !payload.getRuleCode().isBlank()) {
            // Convert SNAKE_CASE to Title Case
            String title = payload.getRuleCode()
                    .replace("_", " ")
                    .toLowerCase();
            return title.substring(0, 1).toUpperCase() + title.substring(1);
        }

        if (payload.getSeverity() != null) {
            return payload.getSeverity() + " Alert";
        }

        return "New Notification";
    }
}
