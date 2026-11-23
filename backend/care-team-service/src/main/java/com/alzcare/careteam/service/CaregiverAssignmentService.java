package com.alzcare.careteam.service;

import com.alzcare.careteam.dto.CaregiverAvailabilityResponse;
import com.alzcare.careteam.dto.CaregiverPermissionsResponse;
import com.alzcare.careteam.entity.CaregiverAssignment;
import com.alzcare.careteam.repository.CaregiverAssignmentRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
public class CaregiverAssignmentService {

    private final CaregiverAssignmentRepository repo;

    public CaregiverAssignmentService(CaregiverAssignmentRepository repo) {
        this.repo = repo;
    }

    public CaregiverAssignment generateInvite(String patientId, CaregiverAssignment.Role role) {
        if (role == CaregiverAssignment.Role.PRIMARY) {
            // Unused PRIMARY invites (no caregiver linked yet) block "Generate link" forever;
            // revoke them so a doctor can issue a fresh link. Active PRIMARY is still exclusive.
            for (CaregiverAssignment a : repo.findByPatientId(patientId)) {
                if (a.getRole() == CaregiverAssignment.Role.PRIMARY
                        && a.getStatus() == CaregiverAssignment.AssignmentStatus.PENDING
                        && (a.getCaregiverId() == null || a.getCaregiverId().isBlank())) {
                    a.setStatus(CaregiverAssignment.AssignmentStatus.REVOKED);
                    a.setRevokedAt(LocalDateTime.now());
                    repo.save(a);
                }
            }
            boolean activePrimary = repo.existsByPatientIdAndRoleAndStatusAndCaregiverIdIsNotNull(
                    patientId, CaregiverAssignment.Role.PRIMARY, CaregiverAssignment.AssignmentStatus.ACTIVE);
            if (activePrimary) {
                throw new IllegalStateException(
                        "Patient already has an active PRIMARY caregiver");
            }
        }

        CaregiverAssignment assignment = CaregiverAssignment.builder()
                .patientId(patientId)
                .role(role)
                .status(CaregiverAssignment.AssignmentStatus.PENDING)
                .assignedAt(LocalDateTime.now())
                .inviteToken(UUID.randomUUID().toString())
                .inviteExpiresAt(LocalDateTime.now().plusDays(7))
                .build();
        return repo.save(assignment);
    }

    /**
     * PDF FR1 — doctor/admin or active PRIMARY caregiver may generate invites; primary may not add another PRIMARY here.
     */
    public CaregiverAssignment generateInviteForActor(
            String actingUserId,
            List<String> realmRoles,
            String patientId,
            CaregiverAssignment.Role role) {
        boolean doctorOrAdmin = realmRoles != null
                && realmRoles.stream().anyMatch(r -> {
                    if (r == null) {
                        return false;
                    }
                    String u = r.trim().toUpperCase();
                    return "DOCTOR".equals(u) || "ADMIN".equals(u);
                });
        if (!doctorOrAdmin) {
            if (realmRoles == null || !realmRoles.contains("CAREGIVER")) {
                throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Not allowed to generate invites");
            }
            boolean isPrimary = repo.findByPatientId(patientId).stream()
                    .anyMatch(a -> actingUserId != null && actingUserId.equals(a.getCaregiverId())
                            && a.getRole() == CaregiverAssignment.Role.PRIMARY
                            && a.getStatus() == CaregiverAssignment.AssignmentStatus.ACTIVE);
            if (!isPrimary) {
                throw new ResponseStatusException(HttpStatus.FORBIDDEN,
                        "Only the active primary caregiver or a doctor may generate invites");
            }
            if (role == CaregiverAssignment.Role.PRIMARY) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                        "Use a doctor to invite the primary caregiver");
            }
        }
        return generateInvite(patientId, role);
    }

    public CaregiverAssignment validateToken(String token) {
        CaregiverAssignment a = repo.findByInviteToken(token)
                .orElseThrow(() -> new IllegalArgumentException("Invalid invite token"));
        if (a.getInviteExpiresAt() != null && a.getInviteExpiresAt().isBefore(LocalDateTime.now())) {
            throw new IllegalArgumentException("Invite token has expired");
        }
        return a;
    }

    public CaregiverAssignment acceptInvite(String token, String caregiverId) {
        CaregiverAssignment assignment = repo.findByInviteToken(token)
                .orElseThrow(() -> new IllegalArgumentException("Invalid token"));

        if (assignment.getInviteExpiresAt() != null && assignment.getInviteExpiresAt().isBefore(LocalDateTime.now())) {
            throw new IllegalArgumentException("Token has expired");
        }

        if (assignment.getRole() == CaregiverAssignment.Role.PRIMARY) {
            boolean otherActivePrimary = repo
                    .findByPatientId(assignment.getPatientId())
                    .stream()
                    .anyMatch(x -> x.getRole() == CaregiverAssignment.Role.PRIMARY
                            && x.getStatus() == CaregiverAssignment.AssignmentStatus.ACTIVE
                            && x.getCaregiverId() != null
                            && !x.getId().equals(assignment.getId()));
            if (otherActivePrimary) {
                throw new IllegalStateException(
                        "Another PRIMARY caregiver is already active; use handover flow before replacing");
            }
        }

        assignment.setCaregiverId(caregiverId);
        assignment.setStatus(CaregiverAssignment.AssignmentStatus.ACTIVE);
        assignment.setInviteAcceptedAt(LocalDateTime.now());
        return repo.save(assignment);
    }

    public List<CaregiverAssignment> getByPatient(String patientId) {
        return repo.findByPatientId(patientId);
    }

    public List<CaregiverAssignment> getByCaregiver(String caregiverId) {
        return repo.findByCaregiverId(caregiverId);
    }

    public CaregiverAssignment updateRole(UUID id, CaregiverAssignment.Role role) {
        CaregiverAssignment a = repo.findById(id).orElseThrow();
        if (role == CaregiverAssignment.Role.PRIMARY) {
            boolean pendingPrimary = repo.existsByPatientIdAndRoleAndStatus(
                    a.getPatientId(), CaregiverAssignment.Role.PRIMARY, CaregiverAssignment.AssignmentStatus.PENDING);
            boolean activeOther = repo.findByPatientId(a.getPatientId()).stream()
                    .anyMatch(x -> !x.getId().equals(id)
                            && x.getRole() == CaregiverAssignment.Role.PRIMARY
                            && x.getStatus() == CaregiverAssignment.AssignmentStatus.ACTIVE
                            && x.getCaregiverId() != null);
            if (pendingPrimary || activeOther) {
                throw new IllegalStateException("Cannot set PRIMARY while another PRIMARY exists for this patient");
            }
        }
        a.setRole(role);
        return repo.save(a);
    }

    public void revoke(UUID id) {
        CaregiverAssignment a = repo.findById(id).orElseThrow();
        a.setStatus(CaregiverAssignment.AssignmentStatus.REVOKED);
        a.setRevokedAt(LocalDateTime.now());
        repo.save(a);
    }

    public CaregiverAssignment setUnavailability(
            UUID id, LocalDateTime from, LocalDateTime to, String reason) {
        CaregiverAssignment a = repo.findById(id).orElseThrow();
        a.setUnavailableFrom(from);
        a.setUnavailableTo(to);
        a.setUnavailabilityReason(reason);
        return repo.save(a);
    }

    public List<CaregiverAssignment> findAll(String patientId, String caregiverId, String role, Boolean active) {
        List<CaregiverAssignment> all = repo.findAll();

        return all.stream()
                .filter(a -> patientId == null || patientId.equals(a.getPatientId()))
                .filter(a -> caregiverId == null || caregiverId.equals(a.getCaregiverId()))
                .filter(a -> role == null || a.getRole().name().equalsIgnoreCase(role))
                .filter(a -> active == null
                        || (active
                        ? a.getStatus() == CaregiverAssignment.AssignmentStatus.ACTIVE
                        : true))
                .toList();
    }

    /**
     * PDF: GET /caregivers/{id}/permissions — effective permissions for one patient context.
     */
    public CaregiverPermissionsResponse getPermissions(String caregiverId, String patientId) {
        List<CaregiverAssignment> matches = repo.findByCaregiverIdAndPatientId(caregiverId, patientId);
        CaregiverAssignment active = matches.stream()
                .filter(a -> a.getStatus() == CaregiverAssignment.AssignmentStatus.ACTIVE)
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("No active assignment for caregiver on this patient"));

        Map<String, Boolean> perms = switch (active.getRole()) {
            case PRIMARY -> Map.of(
                    "patientDataFullAccess", true,
                    "dailyActivitiesView", true,
                    "notifications", true,
                    "emergencyAlertsOnly", false
            );
            case FAMILY -> new LinkedHashMap<>(Map.of(
                    "patientDataFullAccess", false,
                    "dailyActivitiesView", true,
                    "notifications", true,
                    "emergencyAlertsOnly", false
            ));
            case EMERGENCY -> Map.of(
                    "patientDataFullAccess", false,
                    "dailyActivitiesView", false,
                    "notifications", true,
                    "emergencyAlertsOnly", true
            );
        };

        return new CaregiverPermissionsResponse(
                caregiverId, patientId, active.getRole().name(), perms);
    }

    /**
     * PDF: GET /caregivers/{id}/availability — aggregate availability windows from assignments.
     */
    public List<CaregiverAvailabilityResponse> getAvailabilityCalendar(String caregiverId) {
        return repo.findByCaregiverId(caregiverId).stream()
                .filter(a -> a.getUnavailableFrom() != null || a.getUnavailableTo() != null)
                .map(a -> new CaregiverAvailabilityResponse(
                        a.getId(),
                        a.getPatientId(),
                        a.getUnavailableFrom(),
                        a.getUnavailableTo(),
                        a.getUnavailabilityReason()
                ))
                .toList();
    }
}
