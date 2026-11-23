package com.alzcare.careteam.controller;

import com.alzcare.careteam.entity.CaregiverHandover;
import com.alzcare.careteam.service.CaregiverHandoverService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/care-team/caregivers")
public class CaregiverHandoverController {

    private final CaregiverHandoverService service;

    public CaregiverHandoverController(CaregiverHandoverService service) {
        this.service = service;
    }

    @PostMapping("/handover")
    public ResponseEntity<CaregiverHandover> create(
            @RequestBody CaregiverHandover handover) {
        return ResponseEntity.ok(service.create(handover));
    }

    @PutMapping("/handover/{id}/acknowledge")
    public ResponseEntity<CaregiverHandover> acknowledge(
            @PathVariable UUID id) {
        return ResponseEntity.ok(service.acknowledge(id));
    }

    @GetMapping("/patients/{patientId}/handovers")
    public ResponseEntity<List<CaregiverHandover>> getByPatient(
            @PathVariable String patientId) {
        return ResponseEntity.ok(service.getByPatient(patientId));
    }

    @GetMapping("/handover")
    public ResponseEntity<List<CaregiverHandover>> listHandovers(
            @RequestParam(required = false) String patientId,
            @RequestParam(required = false) String fromCaregiverId,
            @RequestParam(required = false) String toCaregiverId,
            @RequestParam(required = false) Boolean acknowledged) {
        return ResponseEntity.ok(service.findFiltered(patientId, fromCaregiverId, toCaregiverId, acknowledged));
    }
}