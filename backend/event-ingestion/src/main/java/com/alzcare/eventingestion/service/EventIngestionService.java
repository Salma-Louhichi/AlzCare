package com.alzcare.eventingestion.service;

import com.alzcare.eventingestion.config.MonitoringProperties;
import com.alzcare.eventingestion.detection.MotionSample;
import com.alzcare.eventingestion.detection.PatternCandidate;
import com.alzcare.eventingestion.detection.PatternDetector;
import com.alzcare.eventingestion.domain.*;
import com.alzcare.eventingestion.repository.ActivityEventRepository;
import com.alzcare.eventingestion.repository.CameraDeviceRepository;
import com.alzcare.eventingestion.repository.DetectedPatternRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.*;
import java.util.stream.Collectors;

/** Stores camera events and turns them into detected patterns. */
@Slf4j
@Service
public class EventIngestionService {

    /** Detection looks this far around a new event. */
    private static final Duration LOOKAROUND = Duration.ofHours(6);
    /** Night patterns of the same type closer than this are merged (one alert per night). */
    private static final Duration NIGHT_MERGE = Duration.ofHours(6);

    private final CameraDeviceRepository cameras;
    private final ActivityEventRepository events;
    private final DetectedPatternRepository patterns;
    private final MonitoringProperties props;
    private final PatternDetector detector;
    private final ApplicationEventPublisher publisher;
    private final Clock clock;

    public EventIngestionService(CameraDeviceRepository cameras, ActivityEventRepository events,
                                 DetectedPatternRepository patterns, MonitoringProperties props,
                                 ApplicationEventPublisher publisher, Clock clock) {
        this.cameras = cameras;
        this.events = events;
        this.patterns = patterns;
        this.props = props;
        this.detector = new PatternDetector(props);
        this.publisher = publisher;
        this.clock = clock;
    }

    public record Ingested(UUID eventId, List<PatternType> newPatterns) {
    }

    @Transactional
    public Ingested ingest(UUID cameraId, Instant occurredAt, double intensity, int durationSeconds) {
        CameraDevice camera = cameras.findById(cameraId)
                .orElseThrow(() -> new ApiException(HttpStatus.UNAUTHORIZED, "Unknown device"));
        if (camera.getStatus() == CameraStatus.PAUSED) {
            // Privacy: a paused camera's data is not stored at all
            throw new ApiException(HttpStatus.CONFLICT, "Camera is paused");
        }
        Instant now = clock.instant();
        Instant at = occurredAt != null ? occurredAt : now;
        if (at.isAfter(now.plus(props.maxClockSkew()))) {
            throw new ApiException(HttpStatus.UNPROCESSABLE_ENTITY, "Event timestamp is in the future");
        }
        if (at.isBefore(now.minus(props.maxEventAge()))) {
            throw new ApiException(HttpStatus.UNPROCESSABLE_ENTITY, "Event is older than " + props.maxEventAge());
        }

        ActivityEvent event = events.save(ActivityEvent.builder()
                .id(UUID.randomUUID())
                .cameraId(camera.getId())
                .patientId(camera.getPatientId())
                .zone(camera.getZone())
                .occurredAt(at)
                .receivedAt(now)
                .motionIntensity(intensity)
                .durationSeconds(durationSeconds)
                .build());

        if (camera.getLastSeen() == null || now.isAfter(camera.getLastSeen())) {
            camera.setLastSeen(now);
        }
        camera.setStatus(CameraStatus.ACTIVE);

        List<PatternType> created = detect(event);
        return new Ingested(event.getId(), created);
    }

    private List<PatternType> detect(ActivityEvent event) {
        List<MotionSample> recent = events.findByPatientIdAndOccurredAtBetweenOrderByOccurredAtAsc(
                        event.getPatientId(), event.getOccurredAt().minus(LOOKAROUND), event.getOccurredAt().plus(LOOKAROUND))
                .stream().map(EventIngestionService::sample).toList();
        MotionSample current = sample(event);

        List<PatternType> created = new ArrayList<>();
        for (PatternCandidate candidate : detector.detect(recent, current)) {
            if (merge(event.getPatientId(), candidate)) {
                continue;
            }
            DetectedPattern pattern = patterns.save(DetectedPattern.builder()
                    .id(UUID.randomUUID())
                    .patientId(event.getPatientId())
                    .type(candidate.type())
                    .severity(candidate.type().severity())
                    .status(PatternStatus.OPEN)
                    .startedAt(candidate.start())
                    .endedAt(candidate.end())
                    .eventCount(candidate.eventCount())
                    .zones(joinZones(candidate.zones()))
                    .detectedAt(clock.instant())
                    .notified(false)
                    .build());
            log.info("Detected {} for patient {} ({} events, zones {})",
                    pattern.getType(), pattern.getPatientId(), pattern.getEventCount(), pattern.getZones());
            publisher.publishEvent(new PatternDetectedEvent(pattern.getId()));
            created.add(pattern.getType());
        }
        return created;
    }

    /**
     * Extends an existing pattern instead of creating a duplicate (and a duplicate alert):
     * a wandering run that keeps going, or a second night event in the same night.
     */
    private boolean merge(String patientId, PatternCandidate candidate) {
        Duration overlap = candidate.type() == PatternType.WANDERING ? props.wanderingMaxGap() : NIGHT_MERGE;
        Optional<DetectedPattern> existing = patterns
                .findByPatientIdAndTypeAndEndedAtGreaterThanEqual(patientId, candidate.type(),
                        candidate.start().minus(overlap))
                .stream()
                .filter(p -> !p.getStartedAt().isAfter(candidate.end().plus(overlap)))
                .findFirst();
        existing.ifPresent(p -> {
            if (candidate.start().isBefore(p.getStartedAt())) {
                p.setStartedAt(candidate.start());
            }
            if (candidate.end().isAfter(p.getEndedAt())) {
                p.setEndedAt(candidate.end());
            }
            p.setEventCount(Math.max(p.getEventCount(), candidate.eventCount()));
            LinkedHashSet<String> zones = new LinkedHashSet<>(Arrays.asList(p.getZones().split(",")));
            candidate.zones().forEach(z -> zones.add(z.name()));
            p.setZones(String.join(",", zones));
        });
        return existing.isPresent();
    }

    private static MotionSample sample(ActivityEvent e) {
        return new MotionSample(e.getOccurredAt(), e.getZone(), e.getDurationSeconds());
    }

    private static String joinZones(Collection<CameraZone> zones) {
        return zones.stream().map(Enum::name).collect(Collectors.joining(","));
    }

    @Transactional(readOnly = true)
    public List<ActivityEvent> recentForPatient(String patientId, Duration window) {
        return events.findTop200ByPatientIdAndOccurredAtAfterOrderByOccurredAtDesc(
                patientId, clock.instant().minus(window));
    }
}
