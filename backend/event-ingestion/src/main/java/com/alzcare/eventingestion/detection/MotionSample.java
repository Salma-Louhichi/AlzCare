package com.alzcare.eventingestion.detection;

import com.alzcare.eventingestion.domain.CameraZone;

import java.time.Instant;

/** The part of an activity event that pattern detection needs. */
public record MotionSample(Instant at, CameraZone zone, int durationSeconds) {

    public Instant end() {
        return at.plusSeconds(durationSeconds);
    }
}
