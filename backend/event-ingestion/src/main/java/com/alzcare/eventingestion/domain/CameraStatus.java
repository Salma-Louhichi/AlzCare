package com.alzcare.eventingestion.domain;

/**
 * ACTIVE  - sending events.
 * OFFLINE - no event for longer than {@code camera-offline-after}; set by the health scheduler.
 * PAUSED  - switched off by the care team (privacy); its events are rejected.
 */
public enum CameraStatus {
    ACTIVE, OFFLINE, PAUSED
}
