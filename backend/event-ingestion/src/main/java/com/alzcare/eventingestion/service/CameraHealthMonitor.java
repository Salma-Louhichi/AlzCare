package com.alzcare.eventingestion.service;

import com.alzcare.eventingestion.config.MonitoringProperties;
import com.alzcare.eventingestion.domain.CameraDevice;
import com.alzcare.eventingestion.domain.CameraStatus;
import com.alzcare.eventingestion.repository.CameraDeviceRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Instant;
import java.util.List;

/** Marks ACTIVE cameras OFFLINE when they have been silent for longer than camera-offline-after. */
@Slf4j
@Component
public class CameraHealthMonitor {

    private final CameraDeviceRepository cameras;
    private final MonitoringProperties props;
    private final Clock clock;

    public CameraHealthMonitor(CameraDeviceRepository cameras, MonitoringProperties props, Clock clock) {
        this.cameras = cameras;
        this.props = props;
        this.clock = clock;
    }

    @Scheduled(fixedDelayString = "PT5M", initialDelayString = "PT1M")
    @Transactional
    public int markSilentCamerasOffline() {
        Instant threshold = clock.instant().minus(props.cameraOfflineAfter());
        List<CameraDevice> silent = cameras.findByStatusAndLastSeenBefore(CameraStatus.ACTIVE, threshold);
        silent.forEach(c -> c.setStatus(CameraStatus.OFFLINE));
        if (!silent.isEmpty()) {
            log.info("Marked {} silent camera(s) OFFLINE", silent.size());
        }
        return silent.size();
    }
}
