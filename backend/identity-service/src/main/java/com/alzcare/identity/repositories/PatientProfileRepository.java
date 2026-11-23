package com.alzcare.identity.repositories;

import com.alzcare.identity.entities.PatientProfile;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Repository for managing {@link PatientProfile} entities.
 * Provides CRUD operations and custom query methods for patient profile data.
 */
@Repository
public interface PatientProfileRepository extends JpaRepository<PatientProfile, UUID> {

    /**
     * Find a patient profile by their User ID (Keycloak ID).
     *
     * @param userId the Keycloak user ID
     * @return an Optional containing the patient profile if found
     */
    Optional<PatientProfile> findByUserId(UUID userId);

    /**
     * Find all patient profiles by their active status.
     *
     * @param isActive the active status to filter by
     * @return a list of patient profiles matching the status
     */
    List<PatientProfile> findByIsActive(Boolean isActive);
}
