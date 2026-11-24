package com.alzcare.identity.services;

import com.alzcare.identity.dtos.requests.AutonomyAssessmentRequest;
import com.alzcare.identity.dtos.responses.AutonomyAssessmentResponse;
import com.alzcare.identity.entities.AutonomyAssessment;
import com.alzcare.identity.entities.PatientProfile;
import com.alzcare.identity.exceptions.PatientNotFoundException;
import com.alzcare.identity.mappers.AutonomyAssessmentMapper;
import com.alzcare.identity.repositories.AutonomyAssessmentRepository;
import com.alzcare.identity.repositories.PatientProfileRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Service for managing patient autonomy assessments.
 * Provides business logic for creating and retrieving assessments across different domains.
 */
@Service
public class AutonomyAssessmentService {

    private final AutonomyAssessmentRepository autonomyAssessmentRepository;
    private final PatientProfileRepository patientProfileRepository;
    private final AutonomyAssessmentMapper autonomyAssessmentMapper;

    /**
     * Constructs a new AutonomyAssessmentService with required dependencies.
     *
     * @param autonomyAssessmentRepository the repository for autonomy assessment data access
     * @param patientProfileRepository     the repository for patient profile data access
     * @param autonomyAssessmentMapper     the mapper for converting between entities and DTOs
     */
    public AutonomyAssessmentService(AutonomyAssessmentRepository autonomyAssessmentRepository,
                                     PatientProfileRepository patientProfileRepository,
                                     AutonomyAssessmentMapper autonomyAssessmentMapper) {
        this.autonomyAssessmentRepository = autonomyAssessmentRepository;
        this.patientProfileRepository = patientProfileRepository;
        this.autonomyAssessmentMapper = autonomyAssessmentMapper;
    }

    /**
     * Creates a new autonomy assessment for a patient.
     * The overall score is automatically calculated based on domain levels.
     *
     * @param patientId the patient profile UUID
     * @param request   the assessment request containing domain levels
     * @return the created autonomy assessment response
     * @throws PatientNotFoundException if no patient is found with the given ID
     */
    @Transactional
    public AutonomyAssessmentResponse createAssessment(UUID patientId, AutonomyAssessmentRequest request) {
        PatientProfile patient = patientProfileRepository.findById(patientId)
                .orElseThrow(() -> new PatientNotFoundException(patientId));

        AutonomyAssessment assessment = autonomyAssessmentMapper.toEntity(request);
        assessment.setPatient(patient);
        assessment.calculateOverallScore();

        AutonomyAssessment savedAssessment = autonomyAssessmentRepository.saveAndFlush(assessment);
        return autonomyAssessmentMapper.toResponse(savedAssessment);
    }

    /**
     * Retrieves the most recent autonomy assessment for a patient.
     *
     * @param patientId the patient profile UUID
     * @return an Optional containing the latest assessment response, or empty if none exists
     * @throws PatientNotFoundException if no patient is found with the given ID
     */
    @Transactional(readOnly = true)
    public Optional<AutonomyAssessmentResponse> getLatestAssessment(UUID patientId) {
        // Verify patient exists
        if (!patientProfileRepository.existsById(patientId)) {
            throw new PatientNotFoundException(patientId);
        }

        return autonomyAssessmentRepository.findTopByPatient_IdOrderByCreatedAtDesc(patientId)
                .map(autonomyAssessmentMapper::toResponse);
    }

    /**
     * Retrieves the complete history of autonomy assessments for a patient,
     * ordered from most recent to oldest.
     *
     * @param patientId the patient profile UUID
     * @return a list of autonomy assessment responses
     * @throws PatientNotFoundException if no patient is found with the given ID
     */
    @Transactional(readOnly = true)
    public List<AutonomyAssessmentResponse> getAssessmentHistory(UUID patientId) {
        // Verify patient exists
        if (!patientProfileRepository.existsById(patientId)) {
            throw new PatientNotFoundException(patientId);
        }

        List<AutonomyAssessment> assessments = autonomyAssessmentRepository.findByPatient_Id(patientId);
        return assessments.stream()
                .map(autonomyAssessmentMapper::toResponse)
                .collect(Collectors.toList());
    }
}
