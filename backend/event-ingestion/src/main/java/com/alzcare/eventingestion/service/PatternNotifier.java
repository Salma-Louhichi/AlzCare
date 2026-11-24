package com.alzcare.eventingestion.service;

import com.alzcare.eventingestion.client.CareTeamClient;
import com.alzcare.eventingestion.config.MonitoringProperties;
import com.alzcare.eventingestion.config.NotificationProperties;
import com.alzcare.eventingestion.domain.DetectedPattern;
import com.alzcare.eventingestion.messaging.NotificationEvent;
import com.alzcare.eventingestion.repository.DetectedPatternRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import java.time.Clock;
import java.time.Duration;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

/**
 * Sends one notification per recipient (the patient's doctor and active caregivers) through
 * RabbitMQ when a pattern is detected.
 *
 * <ul>
 *   <li>Runs after the ingest transaction commits, so no alert is sent for data that was rolled back,
 *       and asynchronously, so the camera's HTTP request is not slowed down by other services.</li>
 *   <li>A pattern is marked {@code notified} only once every message was published; failed ones are
 *       retried every minute for 24 h (care-team or RabbitMQ temporarily down).</li>
 * </ul>
 */
@Slf4j
@Component
public class PatternNotifier {

    private static final DateTimeFormatter HOUR = DateTimeFormatter.ofPattern("HH:mm");

    private final DetectedPatternRepository patterns;
    private final CareTeamClient careTeam;
    private final RabbitTemplate rabbit;
    private final NotificationProperties notifications;
    private final MonitoringProperties monitoring;
    private final Clock clock;

    public PatternNotifier(DetectedPatternRepository patterns, CareTeamClient careTeam, RabbitTemplate rabbit,
                           NotificationProperties notifications, MonitoringProperties monitoring, Clock clock) {
        this.patterns = patterns;
        this.careTeam = careTeam;
        this.rabbit = rabbit;
        this.notifications = notifications;
        this.monitoring = monitoring;
        this.clock = clock;
    }

    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onPatternDetected(PatternDetectedEvent event) {
        patterns.findById(event.patternId()).ifPresent(this::notifySafely);
    }

    @Scheduled(fixedDelayString = "PT1M", initialDelayString = "PT1M")
    public void retryPending() {
        patterns.findByNotifiedFalseAndDetectedAtAfter(clock.instant().minus(Duration.ofHours(24)))
                .forEach(this::notifySafely);
    }

    private void notifySafely(DetectedPattern pattern) {
        try {
            notifyCareTeam(pattern);
        } catch (RuntimeException e) {
            log.warn("Could not notify care team for pattern {} (will retry): {}", pattern.getId(), e.getMessage());
        }
    }

    void notifyCareTeam(DetectedPattern pattern) {
        Set<String> recipients = new LinkedHashSet<>();
        careTeam.activeDoctorId(pattern.getPatientId()).ifPresent(recipients::add);
        recipients.addAll(careTeam.activeCaregiverIds(pattern.getPatientId()));
        String patientName = careTeam.patientName(pattern.getPatientId()).orElse("your patient");
        String message = message(pattern, patientName);

        for (String recipient : recipients) {
            rabbit.convertAndSend(notifications.exchange(), notifications.routingKey(), new NotificationEvent(
                    UUID.randomUUID().toString(), "AlertCreated", "event-ingestion", clock.instant(),
                    new NotificationEvent.Payload(pattern.getId().toString(), pattern.getPatientId(), recipient,
                            pattern.getSeverity().name(), pattern.getType().name(), message, "IN_APP",
                            Map.of("patternType", pattern.getType().name(), "zones", pattern.getZones(),
                                    "startedAt", pattern.getStartedAt().toString(),
                                    "endedAt", pattern.getEndedAt().toString()))));
        }
        if (recipients.isEmpty()) {
            log.warn("Pattern {} detected for patient {} but nobody is assigned to them",
                    pattern.getId(), pattern.getPatientId());
        }
        DetectedPattern fresh = patterns.findById(pattern.getId()).orElse(pattern);
        fresh.setNotified(true);
        patterns.save(fresh);
        log.info("Notified {} recipient(s) about {} {}", recipients.size(), pattern.getType(), pattern.getId());
    }

    String message(DetectedPattern p, String patientName) {
        String zones = Arrays.stream(p.getZones().split(","))
                .map(z -> z.replace('_', ' ').toLowerCase(Locale.ROOT)).reduce((a, b) -> a + ", " + b).orElse("");
        LocalTime start = p.getStartedAt().atZone(monitoring.timezone()).toLocalTime();
        LocalTime end = p.getEndedAt().atZone(monitoring.timezone()).toLocalTime();
        return switch (p.getType()) {
            case WANDERING -> "Possible wandering: %s has been moving between %s since %s (%d min)."
                    .formatted(patientName, zones, HOUR.format(start),
                            Duration.between(p.getStartedAt(), p.getEndedAt()).toMinutes());
            case NIGHT_MOTION -> "Night activity: %s has been moving around (%s) between %s and %s."
                    .formatted(patientName, zones, HOUR.format(start), HOUR.format(end));
            case NIGHT_EXIT_ATTEMPT -> "Possible night exit: movement at the front door of %s's home at %s."
                    .formatted(patientName, HOUR.format(start));
        };
    }

}
