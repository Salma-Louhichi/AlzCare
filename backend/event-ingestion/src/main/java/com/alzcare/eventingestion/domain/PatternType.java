package com.alzcare.eventingestion.domain;

public enum PatternType {
    /** Continuous movement across several rooms for a long time. */
    WANDERING(PatternSeverity.HIGH),
    /** Repeated movement outside the bedroom during the night. */
    NIGHT_MOTION(PatternSeverity.MEDIUM),
    /** Movement at the front door during the night. */
    NIGHT_EXIT_ATTEMPT(PatternSeverity.HIGH);

    private final PatternSeverity severity;

    PatternType(PatternSeverity severity) {
        this.severity = severity;
    }

    public PatternSeverity severity() {
        return severity;
    }
}
