package com.alzcare.careteam.controller;

import com.alzcare.careteam.dto.ChecklistGroupResponse;
import com.alzcare.careteam.dto.GenerateChecklistRequest;
import com.alzcare.careteam.entity.ChecklistItem;
import com.alzcare.careteam.service.ChecklistItemService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/care-team/checklists")
public class ChecklistItemController {

    private final ChecklistItemService service;

    public ChecklistItemController(ChecklistItemService service) {
        this.service = service;
    }

    /** FR6 — generate daily checklist (stub items or explicit seeds). */
    @PostMapping("/generate")
    public ResponseEntity<List<ChecklistItem>> generate(
            @RequestBody GenerateChecklistRequest request) {
        return ResponseEntity.ok(service.generate(request));
    }

    @PostMapping("/items")
    public ResponseEntity<ChecklistItem> create(
            @RequestBody ChecklistItem item) {
        return ResponseEntity.ok(service.create(item));
    }

    /** Grouped view: logical checklists by (patientId, date). */
    @GetMapping("/doctor/{doctorId}/grouped")
    public ResponseEntity<List<ChecklistGroupResponse>> getGroupedByDoctor(
            @PathVariable String doctorId) {
        return ResponseEntity.ok(service.getGroupedByDoctor(doctorId));
    }

    @GetMapping("/items/{id}")
    public ResponseEntity<ChecklistItem> getById(@PathVariable UUID id) {
        return ResponseEntity.ok(service.getById(id));
    }

    @GetMapping("/doctor/{doctorId}")
    public ResponseEntity<List<ChecklistItem>> getByDoctor(
            @PathVariable String doctorId) {
        return ResponseEntity.ok(service.getByDoctor(doctorId));
    }

    // Date format in URL must be: 2026-02-24
    @GetMapping("/patient/{patientId}/date/{date}")
    public ResponseEntity<List<ChecklistItem>> getByPatientAndDate(
            @PathVariable String patientId,
            @PathVariable String date,
            @RequestParam(required = false) String doctorId) {
        return ResponseEntity.ok(
                service.getByDoctorPatientDate(
                        doctorId, patientId, LocalDate.parse(date)
                )
        );
    }

    @PutMapping("/items/{id}/complete")
    public ResponseEntity<ChecklistItem> complete(
            @PathVariable UUID id,
            @RequestBody Map<String, String> body) {
        return ResponseEntity.ok(
                service.complete(id, body.get("completedBy"))
        );
    }

    @PutMapping("/items/{id}/assign")
    public ResponseEntity<ChecklistItem> assign(
            @PathVariable UUID id,
            @RequestBody Map<String, String> body) {
        return ResponseEntity.ok(
                service.assignToCaregiver(
                        id, body.get("caregiverId")
                )
        );
    }

    @DeleteMapping("/items/{id}")
    public ResponseEntity<Void> delete(@PathVariable UUID id) {
        service.delete(id);
        return ResponseEntity.noContent().build();
    }

    // Admin: List all checklist items with optional filtering
    @GetMapping("/items")
    public ResponseEntity<List<ChecklistItem>> getAllItems(
            @RequestParam(required = false) String patientId,
            @RequestParam(required = false) String doctorId,
            @RequestParam(required = false) String assignedTo,
            @RequestParam(required = false) String date,
            @RequestParam(required = false) Boolean completed) {
        return ResponseEntity.ok(service.findAll(patientId, doctorId, assignedTo, date, completed));
    }
}