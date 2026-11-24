-- =============================================================================
-- event-ingestion: home cameras, motion events and detected patterns
-- Only motion metadata is stored - never images or video.
-- =============================================================================

CREATE TABLE camera_devices (
    id              UUID PRIMARY KEY,
    patient_id      VARCHAR(64)  NOT NULL,              -- Keycloak user id of the patient
    mac_address     VARCHAR(17)  NOT NULL UNIQUE,       -- normalised AA:BB:CC:DD:EE:FF
    zone            VARCHAR(20)  NOT NULL,
    status          VARCHAR(10)  NOT NULL,
    device_key_hash VARCHAR(64)  NOT NULL,              -- SHA-256 (hex) of the device key
    last_seen       TIMESTAMPTZ,
    paired_at       TIMESTAMPTZ  NOT NULL,
    paired_by       VARCHAR(64)  NOT NULL,
    CONSTRAINT chk_camera_zone CHECK (zone IN ('BEDROOM','HALLWAY','BATHROOM','FRONT_DOOR','KITCHEN','LIVING_ROOM')),
    CONSTRAINT chk_camera_status CHECK (status IN ('ACTIVE','OFFLINE','PAUSED'))
);
CREATE INDEX idx_camera_patient ON camera_devices(patient_id);

CREATE TABLE activity_events (
    id               UUID PRIMARY KEY,
    camera_id        UUID         NOT NULL REFERENCES camera_devices(id) ON DELETE CASCADE,
    patient_id       VARCHAR(64)  NOT NULL,
    zone             VARCHAR(20)  NOT NULL,
    occurred_at      TIMESTAMPTZ  NOT NULL,
    received_at      TIMESTAMPTZ  NOT NULL,
    motion_intensity DOUBLE PRECISION NOT NULL CHECK (motion_intensity >= 0 AND motion_intensity <= 1),
    duration_seconds INTEGER      NOT NULL CHECK (duration_seconds BETWEEN 1 AND 3600)
);
-- Pattern detection reads a patient's recent events in time order
CREATE INDEX idx_event_patient_time ON activity_events(patient_id, occurred_at);

CREATE TABLE detected_patterns (
    id               UUID PRIMARY KEY,
    patient_id       VARCHAR(64)  NOT NULL,
    type             VARCHAR(30)  NOT NULL,
    severity         VARCHAR(10)  NOT NULL,
    status           VARCHAR(15)  NOT NULL,
    started_at       TIMESTAMPTZ  NOT NULL,
    ended_at         TIMESTAMPTZ  NOT NULL,
    event_count      INTEGER      NOT NULL,
    zones            VARCHAR(200) NOT NULL,             -- comma-separated zones involved
    detected_at      TIMESTAMPTZ  NOT NULL,
    notified         BOOLEAN      NOT NULL DEFAULT FALSE,
    acknowledged_by  VARCHAR(64),
    acknowledged_at  TIMESTAMPTZ,
    CONSTRAINT chk_pattern_type CHECK (type IN ('WANDERING','NIGHT_MOTION','NIGHT_EXIT_ATTEMPT')),
    CONSTRAINT chk_pattern_severity CHECK (severity IN ('MEDIUM','HIGH')),
    CONSTRAINT chk_pattern_status CHECK (status IN ('OPEN','ACKNOWLEDGED'))
);
CREATE INDEX idx_pattern_patient_time ON detected_patterns(patient_id, detected_at DESC);
CREATE INDEX idx_pattern_not_notified ON detected_patterns(notified) WHERE notified = FALSE;
