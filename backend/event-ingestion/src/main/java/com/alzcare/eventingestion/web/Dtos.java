package com.alzcare.eventingestion.web;

import com.alzcare.eventingestion.domain.*;
import jakarta.validation.constraints.*;

import java.time.Instant;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

/** Request/response bodies (JSON field names match the Angular CameraDevice model). */
public final class Dtos {

    private Dtos() {
    }

    public record PairCameraRequest(
            @NotBlank String patientId,
            @NotBlank String macAddress,
            @NotNull CameraZone zone) {
    }

    public record CameraResponse(UUID id, String patientId, String macAddress, CameraZone zone,
                                 CameraStatus status, Instant lastSeen, Instant pairedAt, String pairedBy) {
        public static CameraResponse of(CameraDevice c) {
            return new CameraResponse(c.getId(), c.getPatientId(), c.getMacAddress(), c.getZone(), c.getStatus(),
                    c.getLastSeen(), c.getPairedAt(), c.getPairedBy());
        }
    }

    /** Returned once, at pairing: the device key must be flashed on the camera now. */
    public record PairingResponse(UUID id, String patientId, String macAddress, CameraZone zone,
                                  CameraStatus status, Instant lastSeen, Instant pairedAt, String pairedBy,
                                  String deviceKey) {
    }

    /** Sent by a camera. Patient and zone come from its pairing, never from the device. */
    public record MotionEventRequest(
            Instant timestamp,
            @NotNull @DecimalMin("0.0") @DecimalMax("1.0") Double motionIntensity,
            @NotNull @Min(1) @Max(3600) Integer durationSeconds) {
    }

    public record IngestResponse(UUID eventId, List<PatternType> patternsDetected) {
    }

    public record EventResponse(UUID id, UUID cameraId, CameraZone zone, Instant occurredAt,
                                double motionIntensity, int durationSeconds) {
        public static EventResponse of(ActivityEvent e) {
            return new EventResponse(e.getId(), e.getCameraId(), e.getZone(), e.getOccurredAt(),
                    e.getMotionIntensity(), e.getDurationSeconds());
        }
    }

    public record PatternResponse(UUID id, String patientId, PatternType type, PatternSeverity severity,
                                  PatternStatus status, Instant startedAt, Instant endedAt, int eventCount,
                                  List<CameraZone> zones, Instant detectedAt, boolean notified,
                                  String acknowledgedBy, Instant acknowledgedAt) {
        public static PatternResponse of(DetectedPattern p) {
            List<CameraZone> zones = Arrays.stream(p.getZones().split(",")).map(CameraZone::valueOf).toList();
            return new PatternResponse(p.getId(), p.getPatientId(), p.getType(), p.getSeverity(), p.getStatus(),
                    p.getStartedAt(), p.getEndedAt(), p.getEventCount(), zones, p.getDetectedAt(), p.isNotified(),
                    p.getAcknowledgedBy(), p.getAcknowledgedAt());
        }
    }
}
