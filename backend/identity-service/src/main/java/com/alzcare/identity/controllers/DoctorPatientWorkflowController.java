package com.alzcare.identity.controllers;

import com.alzcare.identity.dtos.requests.DoctorCreatePatientRequest;
import com.alzcare.identity.dtos.responses.DoctorCreatesPatientResponse;
import com.alzcare.identity.services.DoctorPatientProvisioningService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * PDF module 3.2 — doctor creates patient; system auto-assigns doctor in care-team.
 */
@RestController
@RequestMapping("/api/v1/doctors")
@RequiredArgsConstructor
@Tag(name = "Doctor patient workflow", description = "Doctor-driven patient onboarding (PDF)")
public class DoctorPatientWorkflowController {

    private final DoctorPatientProvisioningService provisioningService;

    @PostMapping("/{doctorId}/patients/create")
    @Operation(summary = "Doctor creates patient account", description = "Creates Keycloak user + patient profile + active doctor assignment")
    public ResponseEntity<DoctorCreatesPatientResponse> createPatientAsDoctor(
            @PathVariable String doctorId,
            @Valid @RequestBody DoctorCreatePatientRequest request,
            @RequestHeader(value = HttpHeaders.AUTHORIZATION, required = false) String authorization) {
        // The caller's token is forwarded to care-team, which authorises the assignment
        DoctorCreatesPatientResponse response = provisioningService.provision(doctorId, request, authorization);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }
}
