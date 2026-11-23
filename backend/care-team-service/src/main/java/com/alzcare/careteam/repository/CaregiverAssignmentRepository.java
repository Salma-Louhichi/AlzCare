package com.alzcare.careteam.repository;

import com.alzcare.careteam.entity.CaregiverAssignment;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface CaregiverAssignmentRepository
        extends JpaRepository<CaregiverAssignment, UUID> {

    List<CaregiverAssignment> findByPatientId(String patientId);
    List<CaregiverAssignment> findByCaregiverId(String caregiverId);
    Optional<CaregiverAssignment> findByInviteToken(String token);

    boolean existsByPatientIdAndRoleAndStatus(
            String patientId,
            CaregiverAssignment.Role role,
            CaregiverAssignment.AssignmentStatus status);

    boolean existsByPatientIdAndRoleAndStatusAndCaregiverIdIsNotNull(
            String patientId,
            CaregiverAssignment.Role role,
            CaregiverAssignment.AssignmentStatus status);

    List<CaregiverAssignment> findByCaregiverIdAndPatientId(
            String caregiverId, String patientId);
}