package com.alzcare.careteam.service;

import com.alzcare.careteam.entity.DoctorAssignment;
import com.alzcare.careteam.repository.DoctorAssignmentRepository;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Service
public class DoctorAssignmentService {

    private final DoctorAssignmentRepository repo;

    public DoctorAssignmentService(DoctorAssignmentRepository repo) {
        this.repo = repo;
    }

    public DoctorAssignment createAssignment(String doctorId, String patientId) {
        repo.findByPatientIdAndStatus(patientId, DoctorAssignment.DoctorAssignmentStatus.ACTIVE)
                .ifPresent(a -> {
                    throw new IllegalStateException(
                            "Patient already has an active doctor assignment (FR5: only one active doctor per patient)");
                });
        DoctorAssignment assignment = DoctorAssignment.builder()
                .doctorId(doctorId)
                .patientId(patientId)
                .createdBy(doctorId)
                .status(DoctorAssignment.DoctorAssignmentStatus.ACTIVE)
                .assignedAt(LocalDateTime.now())
                .build();
        return repo.save(assignment);
    }

    public List<DoctorAssignment> getByDoctor(String doctorId) {
        return repo.findByDoctorId(doctorId);
    }

    public DoctorAssignment getActiveByPatient(String patientId) {
        return repo.findByPatientIdAndStatus(
                patientId,
                DoctorAssignment.DoctorAssignmentStatus.ACTIVE
        ).orElseThrow(() ->
                new RuntimeException("No active doctor assignment for this patient")
        );
    }

    public DoctorAssignment deactivate(UUID id) {
        DoctorAssignment a = repo.findById(id).orElseThrow();
        a.setStatus(DoctorAssignment.DoctorAssignmentStatus.INACTIVE);
        return repo.save(a);
    }

    public List<DoctorAssignment> findAll(String doctorId, String patientId, Boolean active) {
        List<DoctorAssignment> all = repo.findAll();
        
        return all.stream()
            .filter(a -> doctorId == null || a.getDoctorId().equals(doctorId))
            .filter(a -> patientId == null || a.getPatientId().equals(patientId))
            .filter(a -> active == null || 
                (active ? a.getStatus() == DoctorAssignment.DoctorAssignmentStatus.ACTIVE : true))
            .toList();
    }
}