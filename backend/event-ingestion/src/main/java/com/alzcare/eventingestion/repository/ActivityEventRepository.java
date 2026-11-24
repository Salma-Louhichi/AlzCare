package com.alzcare.eventingestion.repository;

import com.alzcare.eventingestion.domain.ActivityEvent;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public interface ActivityEventRepository extends JpaRepository<ActivityEvent, UUID> {

    List<ActivityEvent> findByPatientIdAndOccurredAtBetweenOrderByOccurredAtAsc(
            String patientId, Instant from, Instant to);

    List<ActivityEvent> findTop200ByPatientIdAndOccurredAtAfterOrderByOccurredAtDesc(String patientId, Instant after);
}
