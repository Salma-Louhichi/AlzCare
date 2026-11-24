package com.alzcare.eventingestion.repository;

import com.alzcare.eventingestion.domain.CameraDevice;
import com.alzcare.eventingestion.domain.CameraStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public interface CameraDeviceRepository extends JpaRepository<CameraDevice, UUID> {

    List<CameraDevice> findByPatientIdOrderByPairedAtAsc(String patientId);

    boolean existsByMacAddress(String macAddress);

    List<CameraDevice> findByStatusAndLastSeenBefore(CameraStatus status, Instant before);

    List<CameraDevice> findByStatusAndLastSeenIsNullAndPairedAtBefore(CameraStatus status, Instant before);
}
