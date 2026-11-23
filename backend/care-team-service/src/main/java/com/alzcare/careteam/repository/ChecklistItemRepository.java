package com.alzcare.careteam.repository;

import com.alzcare.careteam.entity.ChecklistItem;
import org.springframework.data.jpa.repository.JpaRepository;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public interface ChecklistItemRepository
        extends JpaRepository<ChecklistItem, UUID> {

    List<ChecklistItem> findByDoctorIdAndPatientIdAndDate(
            String doctorId, String patientId, LocalDate date
    );
    List<ChecklistItem> findByDoctorId(String doctorId);
}