package com.alzcare.careteam.controller;

import com.alzcare.careteam.dto.CaregiverAvailabilityResponse;
import com.alzcare.careteam.dto.CaregiverPermissionsResponse;
import com.alzcare.careteam.dto.InviteCreatedResponse;
import com.alzcare.careteam.dto.InviteValidationResponse;
import com.alzcare.careteam.entity.CaregiverAssignment;
import com.alzcare.careteam.security.JwtRealmRoles;
import com.alzcare.careteam.service.CaregiverAssignmentService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/care-team")
public class CaregiverAssignmentController {

    private final CaregiverAssignmentService service;
    private final String inviteFrontendBaseUrl;

    public CaregiverAssignmentController(
            CaregiverAssignmentService service,
            @Value("${alzcare.invite.frontend-base-url:http://localhost:4200}") String inviteFrontendBaseUrl) {
        this.service = service;
        this.inviteFrontendBaseUrl = inviteFrontendBaseUrl;
    }

    // Generate invite link — doctor, admin, or active PRIMARY caregiver (PDF FR1)
    @PostMapping("/caregivers/generate-invite")
    public ResponseEntity<InviteCreatedResponse> generateInvite(
            @RequestBody Map<String, String> body,
            @AuthenticationPrincipal Jwt jwt) {
        if (jwt == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        String patientId = body.get("patientId");
        CaregiverAssignment.Role role =
                CaregiverAssignment.Role.valueOf(body.get("role"));
        CaregiverAssignment a = service.generateInviteForActor(
                jwt.getSubject(), JwtRealmRoles.extract(jwt), patientId, role);
        String base = inviteFrontendBaseUrl.replaceAll("/$", "");
        String url = base + "/accept/" + a.getInviteToken();
        return ResponseEntity.ok(new InviteCreatedResponse(
                a.getId(),
                a.getInviteToken(),
                url,
                a.getInviteExpiresAt() != null ? a.getInviteExpiresAt().toString() : "",
                a.getPatientId(),
                a.getRole().name()
        ));
    }

    // Validate invite token before accepting
    @GetMapping("/invitations/{token}/validate")
    public ResponseEntity<InviteValidationResponse> validateToken(
            @PathVariable String token) {
        CaregiverAssignment a = service.validateToken(token);
        return ResponseEntity.ok(new InviteValidationResponse(
                true,
                a.getPatientId(),
                a.getRole().name(),
                a.getInviteExpiresAt() != null ? a.getInviteExpiresAt().toString() : ""
        ));
    }

    // Caregiver accepts invite and gets linked to patient
    @PostMapping("/invitations/{token}/accept")
    public ResponseEntity<CaregiverAssignment> acceptInvite(
            @PathVariable String token,
            @RequestBody Map<String, String> body) {
        String caregiverId = body.get("caregiverId");
        return ResponseEntity.ok(service.acceptInvite(token, caregiverId));
    }

    // List all caregivers for a patient
    @GetMapping("/patients/{patientId}/caregivers")
    public ResponseEntity<List<CaregiverAssignment>> getByPatient(
            @PathVariable String patientId) {
        return ResponseEntity.ok(service.getByPatient(patientId));
    }

    // List all assignments for a caregiver
    @GetMapping("/caregivers/{caregiverId}/assignments")
    public ResponseEntity<List<CaregiverAssignment>> getByCaregiver(
            @PathVariable String caregiverId) {
        return ResponseEntity.ok(service.getByCaregiver(caregiverId));
    }

    // Change a caregiver's role
    @PutMapping("/caregivers/assignments/{id}/role")
    public ResponseEntity<CaregiverAssignment> updateRole(
            @PathVariable UUID id,
            @RequestBody Map<String, String> body) {
        CaregiverAssignment.Role role =
                CaregiverAssignment.Role.valueOf(body.get("role"));
        return ResponseEntity.ok(service.updateRole(id, role));
    }

    // Revoke a caregiver's access
    @DeleteMapping("/caregivers/assignments/{id}")
    public ResponseEntity<Void> revoke(@PathVariable UUID id) {
        service.revoke(id);
        return ResponseEntity.noContent().build();
    }

    // Mark caregiver as unavailable for a period
    @PutMapping("/caregivers/assignments/{id}/availability")
    public ResponseEntity<CaregiverAssignment> setAvailability(
            @PathVariable UUID id,
            @RequestBody Map<String, String> body) {
        String from = body.containsKey("unavailableFrom")
                ? body.get("unavailableFrom") : body.get("from");
        String to = body.containsKey("unavailableTo")
                ? body.get("unavailableTo") : body.get("to");
        return ResponseEntity.ok(service.setUnavailability(
                id,
                LocalDateTime.parse(from),
                LocalDateTime.parse(to),
                body.get("reason")
        ));
    }

    // Admin: List all caregiver assignments with optional filtering
    @GetMapping("/caregivers/assignments")
    public ResponseEntity<List<CaregiverAssignment>> getAllAssignments(
            @RequestParam(required = false) String patientId,
            @RequestParam(required = false) String caregiverId,
            @RequestParam(required = false) String role,
            @RequestParam(required = false) Boolean active) {
        return ResponseEntity.ok(service.findAll(patientId, caregiverId, role, active));
    }

    /** FR2 — effective permissions for caregiver on a patient */
    @GetMapping("/caregivers/{caregiverId}/permissions")
    public ResponseEntity<CaregiverPermissionsResponse> getPermissions(
            @PathVariable String caregiverId,
            @RequestParam String patientId) {
        return ResponseEntity.ok(service.getPermissions(caregiverId, patientId));
    }

    /** FR3 — aggregated unavailability windows across assignments */
    @GetMapping("/caregivers/{caregiverId}/availability")
    public ResponseEntity<List<CaregiverAvailabilityResponse>> getAvailability(
            @PathVariable String caregiverId) {
        return ResponseEntity.ok(service.getAvailabilityCalendar(caregiverId));
    }
}