package com.alzcare.careteam.controller;

import com.alzcare.careteam.entity.CaregiverHandover;
import com.alzcare.careteam.service.CaregiverHandoverService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/** PDF alias: GET /api/.../patients/{id}/handovers */
@RestController
@RequestMapping("/api/v1/care-team")
public class PatientCoordinationController {

    private final CaregiverHandoverService handoverService;

    public PatientCoordinationController(CaregiverHandoverService handoverService) {
        this.handoverService = handoverService;
    }

    @GetMapping("/patients/{patientId}/handovers")
    public ResponseEntity<List<CaregiverHandover>> handoversByPatient(
            @PathVariable String patientId) {
        return ResponseEntity.ok(handoverService.getByPatient(patientId));
    }
}
