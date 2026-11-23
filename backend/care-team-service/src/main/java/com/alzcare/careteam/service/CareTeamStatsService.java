package com.alzcare.careteam.service;

import com.alzcare.careteam.dto.CareTeamStatsResponse;
import com.alzcare.careteam.entity.CaregiverAssignment;
import com.alzcare.careteam.repository.CaregiverAssignmentRepository;
import com.alzcare.careteam.repository.DoctorAssignmentRepository;
import org.springframework.stereotype.Service;

import java.util.Objects;

@Service
public class CareTeamStatsService {

    private final CaregiverAssignmentRepository caregiverRepo;
    private final DoctorAssignmentRepository doctorRepo;

    public CareTeamStatsService(
            CaregiverAssignmentRepository caregiverRepo,
            DoctorAssignmentRepository doctorRepo) {
        this.caregiverRepo = caregiverRepo;
        this.doctorRepo = doctorRepo;
    }

    public CareTeamStatsResponse aggregate() {
        var assignments = caregiverRepo.findAll();
        int pendingInvites = (int) assignments.stream()
                .filter(a -> a.getStatus() == CaregiverAssignment.AssignmentStatus.PENDING)
                .count();
        int activeAssignments = (int) assignments.stream()
                .filter(a -> a.getStatus() == CaregiverAssignment.AssignmentStatus.ACTIVE
                        && a.getCaregiverId() != null && !a.getCaregiverId().isBlank())
                .count();
        int totalCaregivers = (int) assignments.stream()
                .map(CaregiverAssignment::getCaregiverId)
                .filter(Objects::nonNull)
                .filter(id -> !id.isBlank())
                .distinct()
                .count();
        int totalDoctors = (int) doctorRepo.findAll().stream()
                .map(com.alzcare.careteam.entity.DoctorAssignment::getDoctorId)
                .filter(Objects::nonNull)
                .distinct()
                .count();

        return new CareTeamStatsResponse(totalCaregivers, totalDoctors, activeAssignments, pendingInvites);
    }
}
