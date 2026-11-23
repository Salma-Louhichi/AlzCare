package com.alzcare.identity.repositories;

import com.alzcare.identity.entities.AutonomyAssessment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Repository for managing {@link AutonomyAssessment} entities.
 * Provides CRUD operations and custom query methods for patient autonomy assessments.
 */
@Repository
public interface AutonomyAssessmentRepository extends JpaRepository<AutonomyAssessment, UUID> {

    /**
     * Find all autonomy assessments for a specific patient.
     *
     * @param patientId the patient profile ID
     * @return a list of autonomy assessments for the patient
     */
    List<AutonomyAssessment> findByPatient_Id(UUID patientId);

    /**
     * Find the most recent autonomy assessment for a specific patient.
     *
     * @param patientId the patient profile ID
     * @return an Optional containing the most recent assessment if found
     */
    Optional<AutonomyAssessment> findTopByPatient_IdOrderByCreatedAtDesc(UUID patientId);
}
