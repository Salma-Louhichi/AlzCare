package com.alzcare.identity.services;

import com.alzcare.identity.dtos.requests.PatientCreateRequest;
import com.alzcare.identity.dtos.requests.PatientUpdateRequest;
import com.alzcare.identity.dtos.responses.PatientProfileResponse;
import com.alzcare.identity.entities.PatientProfile;
import com.alzcare.identity.exceptions.PatientNotFoundException;
import com.alzcare.identity.mappers.PatientProfileMapper;
import com.alzcare.identity.repositories.PatientProfileRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

/**
 * Service for managing patient profiles.
 * Provides business logic for creating, retrieving, and updating patient profiles.
 */
@Slf4j
@Service
public class PatientProfileService {

    private final PatientProfileRepository patientProfileRepository;
    private final PatientProfileMapper patientProfileMapper;
    private final KeycloakRealmUserService keycloakRealmUserService;

    /**
     * Constructs a new PatientProfileService with required dependencies.
     *
     * @param patientProfileRepository the repository for patient profile data access
     * @param patientProfileMapper     the mapper for converting between entities and DTOs
     * @param keycloakRealmUserService verifies Keycloak user still exists (profile alone is not enough)
     */
    public PatientProfileService(PatientProfileRepository patientProfileRepository,
                                 PatientProfileMapper patientProfileMapper,
                                 KeycloakRealmUserService keycloakRealmUserService) {
        this.patientProfileRepository = patientProfileRepository;
        this.patientProfileMapper = patientProfileMapper;
        this.keycloakRealmUserService = keycloakRealmUserService;
    }

    /**
     * Creates a new patient profile.
     *
     * @param request the patient creation request containing profile details
     * @return the created patient profile response
     * @throws IllegalArgumentException if a patient with the same userId already exists
     */
    @Transactional
    public PatientProfileResponse createPatient(PatientCreateRequest request) {
        patientProfileRepository.findByUserId(request.getUserId())
                .ifPresent(existing -> {
                    throw new IllegalArgumentException(
                            String.format("Patient profile already exists with userId: %s", request.getUserId()));
                });

        PatientProfile patientProfile = patientProfileMapper.toEntity(request);
        PatientProfile savedProfile = patientProfileRepository.saveAndFlush(patientProfile);
        return patientProfileMapper.toResponse(savedProfile);
    }

    /**
     * Retrieves a patient profile by their User ID (Keycloak ID).
     *
     * @param userId the Keycloak user ID
     * @return the patient profile response
     * @throws PatientNotFoundException if no patient is found with the given userId
     */
    @Transactional(readOnly = true)
    public PatientProfileResponse getPatientByUserId(UUID userId) {
        PatientProfile patientProfile = patientProfileRepository.findByUserId(userId)
                .orElseThrow(() -> PatientNotFoundException.byUserId(userId));
        if (!keycloakRealmUserService.existsInRealm(userId.toString())) {
            log.warn("Patient profile exists for userId {} but Keycloak user was removed — returning not found", userId);
            throw PatientNotFoundException.byUserId(userId);
        }
        return patientProfileMapper.toResponse(patientProfile);
    }

    /**
     * Retrieves a patient profile by their internal ID.
     *
     * @param id the patient profile UUID
     * @return the patient profile response
     * @throws PatientNotFoundException if no patient is found with the given ID
     */
    @Transactional(readOnly = true)
    public PatientProfileResponse getPatientById(UUID id) {
        PatientProfile patientProfile = patientProfileRepository.findById(id)
                .orElseThrow(() -> new PatientNotFoundException(id));
        if (!keycloakRealmUserService.existsInRealm(patientProfile.getUserId().toString())) {
            log.warn("Patient profile {} points to removed Keycloak user {} — returning not found", id, patientProfile.getUserId());
            throw new PatientNotFoundException(id);
        }
        return patientProfileMapper.toResponse(patientProfile);
    }

    /**
     * Updates an existing patient profile.
     * Only non-null fields from the request will be updated.
     *
     * @param id      the patient profile UUID to update
     * @param request the update request containing fields to modify
     * @return the updated patient profile response
     * @throws PatientNotFoundException if no patient is found with the given ID
     */
    @Transactional
    public PatientProfileResponse updatePatient(UUID id, PatientUpdateRequest request) {
        PatientProfile patientProfile = patientProfileRepository.findById(id)
                .orElseThrow(() -> new PatientNotFoundException(id));

        patientProfileMapper.updateEntityFromRequest(request, patientProfile);
        PatientProfile updatedProfile = patientProfileRepository.save(patientProfile);
        return patientProfileMapper.toResponse(updatedProfile);
    }

    /**
     * Updates an existing patient profile by user ID.
     * Only non-null fields from the request will be updated.
     *
     * @param userId  the Keycloak user ID
     * @param request the update request containing fields to modify
     * @return the updated patient profile response
     * @throws PatientNotFoundException if no patient is found with the given user ID
     */
    @Transactional
    public PatientProfileResponse updatePatientByUserId(UUID userId, PatientUpdateRequest request) {
        PatientProfile patientProfile = patientProfileRepository.findByUserId(userId)
                .orElseThrow(() -> PatientNotFoundException.byUserId(userId));

        patientProfileMapper.updateEntityFromRequest(request, patientProfile);
        PatientProfile updatedProfile = patientProfileRepository.save(patientProfile);
        return patientProfileMapper.toResponse(updatedProfile);
    }

    /**
     * Retrieves all patient profiles, optionally filtered by active status.
     *
     * @param isActive optional filter for active status
     * @return a list of patient profile responses
     */
    @Transactional(readOnly = true)
    public List<PatientProfileResponse> getAllPatients(Boolean isActive) {
        List<PatientProfile> patients;
        if (isActive != null) {
            patients = patientProfileRepository.findByIsActive(isActive);
        } else {
            patients = patientProfileRepository.findAll();
        }
        return patients.stream()
                .map(patientProfileMapper::toResponse)
                .toList();
    }
}
