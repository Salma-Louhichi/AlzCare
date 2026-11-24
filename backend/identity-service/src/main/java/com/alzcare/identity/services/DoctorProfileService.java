package com.alzcare.identity.services;

import com.alzcare.identity.dtos.requests.DoctorCreateRequest;
import com.alzcare.identity.dtos.requests.DoctorUpdateRequest;
import com.alzcare.identity.dtos.responses.DoctorProfileResponse;
import com.alzcare.identity.entities.DoctorProfile;
import com.alzcare.identity.exceptions.DoctorNotFoundException;
import com.alzcare.identity.mappers.DoctorProfileMapper;
import com.alzcare.identity.repositories.DoctorProfileRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

/**
 * Service for managing doctor profiles.
 * Provides business logic for retrieving and updating doctor profile information.
 */
@Service
public class DoctorProfileService {

    private final DoctorProfileRepository doctorProfileRepository;
    private final DoctorProfileMapper doctorProfileMapper;

    /**
     * Constructs a new DoctorProfileService with required dependencies.
     *
     * @param doctorProfileRepository the repository for doctor profile data access
     * @param doctorProfileMapper     the mapper for converting between entities and DTOs
     */
    public DoctorProfileService(DoctorProfileRepository doctorProfileRepository,
                                DoctorProfileMapper doctorProfileMapper) {
        this.doctorProfileRepository = doctorProfileRepository;
        this.doctorProfileMapper = doctorProfileMapper;
    }

    /**
     * Creates a new doctor profile.
     *
     * @param request the doctor creation request containing profile details
     * @return the created doctor profile response
     * @throws IllegalArgumentException if a doctor with the same userId already exists
     */
    @Transactional
    public DoctorProfileResponse createDoctor(DoctorCreateRequest request) {
        doctorProfileRepository.findByUserId(request.getUserId())
                .ifPresent(existing -> {
                    throw new IllegalArgumentException(
                            String.format("Doctor profile already exists with userId: %s", request.getUserId()));
                });

        DoctorProfile doctorProfile = doctorProfileMapper.toEntity(request);
        DoctorProfile savedProfile = doctorProfileRepository.saveAndFlush(doctorProfile);
        return doctorProfileMapper.toResponse(savedProfile);
    }

    /**
     * Retrieves a doctor profile by their internal ID.
     *
     * @param id the doctor profile UUID
     * @return the doctor profile response
     * @throws DoctorNotFoundException if no doctor is found with the given ID
     */
    @Transactional(readOnly = true)
    public DoctorProfileResponse getDoctorById(UUID id) {
        DoctorProfile doctorProfile = doctorProfileRepository.findById(id)
                .orElseThrow(() -> new DoctorNotFoundException(id));
        return doctorProfileMapper.toResponse(doctorProfile);
    }

    /**
     * Retrieves a doctor profile by their user ID.
     *
     * @param userId the user ID (typically from Keycloak)
     * @return the doctor profile response
     * @throws DoctorNotFoundException if no doctor is found with the given user ID
     */
    @Transactional(readOnly = true)
    public DoctorProfileResponse getDoctorByUserId(UUID userId) {
        DoctorProfile doctorProfile = doctorProfileRepository.findByUserId(userId)
                .orElseThrow(() -> new DoctorNotFoundException("Doctor profile not found with userId: " + userId, true));
        return doctorProfileMapper.toResponse(doctorProfile);
    }

    /**
     * Updates an existing doctor profile.
     * Only non-null fields from the request will be updated.
     *
     * @param id      the doctor profile UUID to update
     * @param request the update request containing fields to modify
     * @return the updated doctor profile response
     * @throws DoctorNotFoundException if no doctor is found with the given ID
     */
    @Transactional
    public DoctorProfileResponse updateDoctor(UUID id, DoctorUpdateRequest request) {
        DoctorProfile doctorProfile = doctorProfileRepository.findById(id)
                .orElseThrow(() -> new DoctorNotFoundException(id));

        doctorProfileMapper.updateEntityFromRequest(request, doctorProfile);
        DoctorProfile updatedProfile = doctorProfileRepository.save(doctorProfile);
        return doctorProfileMapper.toResponse(updatedProfile);
    }

    /**
     * Updates an existing doctor profile by user ID.
     * Only non-null fields from the request will be updated.
     *
     * @param userId  the user ID (typically from Keycloak)
     * @param request the update request containing fields to modify
     * @return the updated doctor profile response
     * @throws DoctorNotFoundException if no doctor is found with the given user ID
     */
    @Transactional
    public DoctorProfileResponse updateDoctorByUserId(UUID userId, DoctorUpdateRequest request) {
        DoctorProfile doctorProfile = doctorProfileRepository.findByUserId(userId)
                .orElseThrow(() -> new DoctorNotFoundException("Doctor profile not found with userId: " + userId, true));

        doctorProfileMapper.updateEntityFromRequest(request, doctorProfile);
        DoctorProfile updatedProfile = doctorProfileRepository.save(doctorProfile);
        return doctorProfileMapper.toResponse(updatedProfile);
    }
}
