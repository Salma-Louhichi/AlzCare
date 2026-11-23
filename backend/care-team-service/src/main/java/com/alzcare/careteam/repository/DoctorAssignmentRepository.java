package com.alzcare.careteam.repository;

import com.alzcare.careteam.entity.DoctorAssignment;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface DoctorAssignmentRepository
        extends JpaRepository<DoctorAssignment, UUID> {

    List<DoctorAssignment> findByDoctorId(String doctorId);
    Optional<DoctorAssignment> findByPatientIdAndStatus(
            String patientId,
            DoctorAssignment.DoctorAssignmentStatus status
    );

    List<DoctorAssignment> findByPatientId(String patientId);
}