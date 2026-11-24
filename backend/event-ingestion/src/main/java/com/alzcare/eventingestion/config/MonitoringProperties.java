package com.alzcare.eventingestion.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;
import java.time.LocalTime;
import java.time.ZoneId;

/** Detection thresholds (alzcare.monitoring.*), taken from the team's Axis 3/4 design. */
@ConfigurationProperties(prefix = "alzcare.monitoring")
public record MonitoringProperties(
        ZoneId timezone,
        LocalTime nightStart,
        LocalTime nightEnd,
        Duration wanderingMinDuration,
        Duration wanderingMaxGap,
        int wanderingMinZones,
        int wanderingMinEvents,
        int nightMotionMinEvents,
        Duration nightMotionWindow,
        Duration cameraOfflineAfter,
        Duration maxEventAge,
        Duration maxClockSkew
) {
    public static MonitoringProperties defaults() {
        return new MonitoringProperties(ZoneId.of("Africa/Tunis"), LocalTime.of(22, 0), LocalTime.of(6, 0),
                Duration.ofMinutes(120), Duration.ofMinutes(10), 2, 6, 3, Duration.ofMinutes(30),
                Duration.ofMinutes(60), Duration.ofHours(24), Duration.ofMinutes(5));
    }
}
