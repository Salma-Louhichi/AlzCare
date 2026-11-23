package com.alzcare.careteam.service;

import com.alzcare.careteam.entity.CaregiverHandover;
import com.alzcare.careteam.repository.CaregiverHandoverRepository;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Service
public class CaregiverHandoverService {

    private final CaregiverHandoverRepository repo;

    public CaregiverHandoverService(CaregiverHandoverRepository repo) {
        this.repo = repo;
    }

    public CaregiverHandover create(CaregiverHandover handover) {
        handover.setAcknowledged(false);
        handover.setCreatedAt(LocalDateTime.now());
        return repo.save(handover);
    }

    public CaregiverHandover acknowledge(UUID id) {
        CaregiverHandover h = repo.findById(id).orElseThrow();
        h.setAcknowledged(true);
        h.setAcknowledgedAt(LocalDateTime.now());
        return repo.save(h);
    }

    public List<CaregiverHandover> getByPatient(String patientId) {
        return repo.findByPatientId(patientId);
    }

    public List<CaregiverHandover> findFiltered(
            String patientId,
            String fromCaregiverId,
            String toCaregiverId,
            Boolean acknowledged) {
        return repo.findAll().stream()
                .filter(h -> patientId == null || patientId.equals(h.getPatientId()))
                .filter(h -> fromCaregiverId == null || fromCaregiverId.equals(h.getFromCaregiverId()))
                .filter(h -> toCaregiverId == null || toCaregiverId.equals(h.getToCaregiverId()))
                .filter(h -> acknowledged == null
                        || Boolean.TRUE.equals(h.getAcknowledged()) == acknowledged)
                .toList();
    }
}