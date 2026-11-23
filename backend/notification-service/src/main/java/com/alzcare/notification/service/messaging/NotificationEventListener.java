package com.alzcare.notification.service.messaging;

import com.alzcare.notification.dto.NotificationEvent;
import com.alzcare.notification.service.NotificationProcessor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Component;

/**
 * RabbitMQ listener for notification events.
 * <p>Consumes messages from the notification queue and dead letter queue,
 * delegating processing to the {@link NotificationProcessor}.</p>
 */
@Component
@Slf4j
public class NotificationEventListener {

    private final NotificationProcessor notificationProcessor;

    public NotificationEventListener(NotificationProcessor notificationProcessor) {
        this.notificationProcessor = notificationProcessor;
    }

    /**
     * Handles incoming notification events from the main queue.
     * <p>Validates the event and delegates processing to the notification processor.</p>
     *
     * @param event the notification event payload
     */
    @RabbitListener(queues = "${rabbitmq.queue.notification:notification.queue}")
    public void handleNotificationEvent(@Payload NotificationEvent event) {
        log.info("Received notification event - eventId: {}, eventType: {}, source: {}",
                event.getEventId(), event.getEventType(), event.getSource());

        // Validate the event
        if (!isValidEvent(event)) {
            log.error("Invalid notification event received - eventId: {}", event.getEventId());
            throw new IllegalArgumentException("Invalid notification event: missing required fields");
        }

        try {
            // Delegate to processor
            notificationProcessor.processEvent(event);
            log.info("Successfully processed notification event - eventId: {}", event.getEventId());
        } catch (Exception e) {
            log.error("Failed to process notification event - eventId: {}, error: {}",
                    event.getEventId(), e.getMessage(), e);
            throw e; // Re-throw to trigger retry/DLX mechanism
        }
    }

    /**
     * Handles messages from the dead letter queue.
     * <p>Logs failed events for manual investigation and potential reprocessing.</p>
     *
     * @param failedEvent the failed notification event
     */
    @RabbitListener(queues = "${rabbitmq.queue.notification-dlq:notification.queue.dlq}")
    public void handleDeadLetter(@Payload NotificationEvent failedEvent) {
        log.warn("Dead letter message received - eventId: {}, eventType: {}, source: {}",
                failedEvent.getEventId(), failedEvent.getEventType(), failedEvent.getSource());

        if (failedEvent.getPayload() != null) {
            log.warn("Dead letter details - alertId: {}, recipientId: {}, severity: {}, channel: {}",
                    failedEvent.getPayload().getAlertId(),
                    failedEvent.getPayload().getRecipientId(),
                    failedEvent.getPayload().getSeverity(),
                    failedEvent.getPayload().getChannel());
        }

        // TODO: Persist to a failed events collection for manual investigation
        // TODO: Send alert to operations team for critical failed notifications
        // TODO: Implement reprocessing capability
    }

    /**
     * Validates the notification event has required fields.
     *
     * @param event the event to validate
     * @return true if valid, false otherwise
     */
    private boolean isValidEvent(NotificationEvent event) {
        if (event == null) {
            return false;
        }
        if (event.getEventId() == null || event.getEventId().isBlank()) {
            return false;
        }
        if (event.getEventType() == null || event.getEventType().isBlank()) {
            return false;
        }
        if (event.getPayload() == null) {
            return false;
        }
        if (event.getPayload().getRecipientId() == null || event.getPayload().getRecipientId().isBlank()) {
            return false;
        }
        return true;
    }
}
