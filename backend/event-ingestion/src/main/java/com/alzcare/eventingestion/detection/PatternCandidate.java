package com.alzcare.eventingestion.detection;

import com.alzcare.eventingestion.domain.CameraZone;
import com.alzcare.eventingestion.domain.PatternType;

import java.time.Instant;
import java.util.LinkedHashSet;

/** A pattern found around one event, before it is merged with already stored patterns. */
public record PatternCandidate(PatternType type, Instant start, Instant end, int eventCount,
                               LinkedHashSet<CameraZone> zones) {
}
