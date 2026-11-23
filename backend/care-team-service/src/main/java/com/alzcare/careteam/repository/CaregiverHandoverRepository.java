package com.alzcare.careteam.repository;

import com.alzcare.careteam.entity.CaregiverHandover;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.UUID;

public interface CaregiverHandoverRepository
        extends JpaRepository<CaregiverHandover, UUID> {

    List<CaregiverHandover> findByPatientId(String patientId);
}