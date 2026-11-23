package com.alzcare.careteam.controller;

import com.alzcare.careteam.entity.DoctorAssignment;
import com.alzcare.careteam.entity.PatientCareProfile;
import com.alzcare.careteam.service.DoctorAssignmentService;
import com.alzcare.careteam.service.PatientCareProfileService;
import com.alzcare.careteam.security.JwtRealmRoles;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/care-team")
public class DoctorAssignmentController {

    private final DoctorAssignmentService service;
    private final PatientCareProfileService patientCareProfileService;

    public DoctorAssignmentController(
            DoctorAssignmentService service,
            PatientCareProfileService patientCareProfileService) {
        this.service = service;
        this.patientCareProfileService = patientCareProfileService;
    }

    // Doctor creates patient and gets auto-assigned
    @PostMapping("/doctors/{doctorId}/patients/create")
    public ResponseEntity<DoctorAssignment> createAssignment(
            @PathVariable String doctorId,
            @RequestBody Map<String, String> body,
            @AuthenticationPrincipal Jwt jwt) {
        // A doctor may only assign patients to themselves; admins may assign to any doctor.
        List<String> roles = JwtRealmRoles.extract(jwt);
        boolean isAdmin = roles.contains("ADMIN");
        boolean isSelf = roles.contains("DOCTOR") && doctorId.equals(jwt.getSubject());
        if (!isAdmin && !isSelf) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }
        String patientId = body.get("patientId");
        if (patientId == null || patientId.isBlank()) {
            return ResponseEntity.badRequest().build();
        }
        return ResponseEntity.ok(service.createAssignment(doctorId, patientId));
    }

    // List all patients assigned to a doctor
    @GetMapping("/doctors/{doctorId}/patients")
    public ResponseEntity<List<DoctorAssignment>> getByDoctor(
            @PathVariable String doctorId) {
        return ResponseEntity.ok(service.getByDoctor(doctorId));
    }

    // Get the active doctor for a patient
    @GetMapping("/patients/{patientId}/doctor")
    public ResponseEntity<DoctorAssignment> getActiveByPatient(
            @PathVariable String patientId) {
        return ResponseEntity.ok(service.getActiveByPatient(patientId));
    }

    // Deactivate doctor assignment
    @PutMapping("/doctor-assignments/{id}/deactivate")
    public ResponseEntity<DoctorAssignment> deactivate(@PathVariable UUID id) {
        return ResponseEntity.ok(service.deactivate(id));
    }

    // Admin: List all doctor assignments with optional filtering
    @GetMapping("/doctors/assignments")
    public ResponseEntity<List<DoctorAssignment>> getAllAssignments(
            @RequestParam(required = false) String doctorId,
            @RequestParam(required = false) String patientId,
            @RequestParam(required = false) Boolean active) {
        return ResponseEntity.ok(service.findAll(doctorId, patientId, active));
    }

    /** Module 3.2 — update patient care demographics (care-team bounded context). */
    @PutMapping("/patients/{patientId}")
    public ResponseEntity<PatientCareProfile> updatePatientCareProfile(
            @PathVariable String patientId,
            @RequestBody Map<String, String> body) {
        return ResponseEntity.ok(patientCareProfileService.upsert(patientId, body));
    }

    @GetMapping("/patients/{patientId}/care-profile")
    public ResponseEntity<PatientCareProfile> getPatientCareProfile(
            @PathVariable String patientId) {
        return ResponseEntity.ok(patientCareProfileService.get(patientId));
    }
}