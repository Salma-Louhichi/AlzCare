package com.alzcare.eventingestion.service;

import com.alzcare.eventingestion.domain.CameraDevice;
import com.alzcare.eventingestion.domain.CameraStatus;
import com.alzcare.eventingestion.domain.CameraZone;
import com.alzcare.eventingestion.repository.CameraDeviceRepository;
import com.alzcare.eventingestion.security.DeviceKeys;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.util.List;
import java.util.Locale;
import java.util.UUID;
import java.util.regex.Pattern;

@Service
public class CameraService {

    private static final Pattern MAC = Pattern.compile("^([0-9A-F]{2}:){5}[0-9A-F]{2}$");

    private final CameraDeviceRepository cameras;
    private final Clock clock;

    public CameraService(CameraDeviceRepository cameras, Clock clock) {
        this.cameras = cameras;
        this.clock = clock;
    }

    /** Result of pairing: the camera and its device key, which is returned this one time only. */
    public record Pairing(CameraDevice camera, String deviceKey) {
    }

    @Transactional
    public Pairing pair(String patientId, String macAddress, CameraZone zone, String pairedBy) {
        String mac = normaliseMac(macAddress);
        if (cameras.existsByMacAddress(mac)) {
            throw new ApiException(HttpStatus.CONFLICT, "A camera with MAC address " + mac + " is already paired");
        }
        String key = DeviceKeys.generate();
        CameraDevice camera = cameras.save(CameraDevice.builder()
                .id(UUID.randomUUID())
                .patientId(patientId)
                .macAddress(mac)
                .zone(zone)
                .status(CameraStatus.OFFLINE)          // becomes ACTIVE with its first event
                .deviceKeyHash(DeviceKeys.hash(key))
                .pairedAt(clock.instant())
                .pairedBy(pairedBy)
                .build());
        return new Pairing(camera, key);
    }

    @Transactional(readOnly = true)
    public List<CameraDevice> forPatient(String patientId) {
        return cameras.findByPatientIdOrderByPairedAtAsc(patientId);
    }

    @Transactional(readOnly = true)
    public CameraDevice get(UUID id) {
        return cameras.findById(id).orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Camera not found"));
    }

    /**
     * Only PAUSED/ACTIVE can be set by people: OFFLINE is decided by the health monitor
     * (a resumed camera that stays silent goes OFFLINE after {@code camera-offline-after}).
     */
    @Transactional
    public CameraDevice updateStatus(UUID id, CameraStatus status) {
        CameraDevice camera = get(id);
        if (status == CameraStatus.OFFLINE) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "OFFLINE is set automatically; use PAUSED or ACTIVE");
        }
        camera.setStatus(status);
        return camera;
    }

    @Transactional
    public void unpair(UUID id) {
        cameras.delete(get(id));   // its events are deleted by ON DELETE CASCADE
    }

    static String normaliseMac(String raw) {
        String mac = raw == null ? "" : raw.trim().toUpperCase(Locale.ROOT).replace('-', ':');
        if (!MAC.matcher(mac).matches()) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Invalid MAC address, expected AA:BB:CC:DD:EE:FF");
        }
        return mac;
    }
}
