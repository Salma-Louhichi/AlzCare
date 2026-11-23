package com.alzcare.careteam.controller;

import com.alzcare.careteam.entity.DoctorAssignment;
import com.alzcare.careteam.entity.PatientCareProfile;
import com.alzcare.careteam.service.DoctorAssignmentService;
import com.alzcare.careteam.service.PatientCareProfileService;
import org.springframework.http.ResponseEntity;
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
            @RequestBody Map<String, String> body) {
        String patientId = body.get("patientId");
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