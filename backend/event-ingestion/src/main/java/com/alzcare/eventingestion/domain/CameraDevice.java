package com.alzcare.eventingestion.domain;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;
import java.util.UUID;

/** An ESP32 camera paired with one patient. The device key itself is never stored, only its hash. */
@Entity
@Table(name = "camera_devices")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CameraDevice {

    @Id
    private UUID id;

    @Column(name = "patient_id", nullable = false)
    private String patientId;

    @Column(name = "mac_address", nullable = false, unique = true)
    private String macAddress;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private CameraZone zone;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private CameraStatus status;

    @Column(name = "device_key_hash", nullable = false)
    private String deviceKeyHash;

    @Column(name = "last_seen")
    private Instant lastSeen;

    @Column(name = "paired_at", nullable = false)
    private Instant pairedAt;

    @Column(name = "paired_by", nullable = false)
    private String pairedBy;
}
