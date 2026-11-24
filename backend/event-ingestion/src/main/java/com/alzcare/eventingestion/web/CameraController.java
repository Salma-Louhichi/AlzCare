package com.alzcare.eventingestion.web;

import com.alzcare.eventingestion.domain.CameraDevice;
import com.alzcare.eventingestion.domain.CameraStatus;
import com.alzcare.eventingestion.service.CameraService;
import com.alzcare.eventingestion.service.PatientAccess;
import com.alzcare.eventingestion.web.Dtos.CameraResponse;
import com.alzcare.eventingestion.web.Dtos.PairCameraRequest;
import com.alzcare.eventingestion.web.Dtos.PairingResponse;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

/** Camera management for doctors (their own patients) and admins. */
@RestController
@RequestMapping("/api/cameras")
public class CameraController {

    private final CameraService cameras;
    private final PatientAccess access;

    public CameraController(CameraService cameras, PatientAccess access) {
        this.cameras = cameras;
        this.access = access;
    }

    @PostMapping("/pair")
    public ResponseEntity<PairingResponse> pair(@Valid @RequestBody PairCameraRequest request,
                                                @AuthenticationPrincipal Jwt jwt) {
        access.check(jwt, request.patientId());
        CameraService.Pairing pairing = cameras.pair(request.patientId(), request.macAddress(), request.zone(),
                jwt.getSubject());   // pairedBy is the caller, never taken from the body
        CameraDevice c = pairing.camera();
        return ResponseEntity.status(HttpStatus.CREATED).body(new PairingResponse(c.getId(), c.getPatientId(),
                c.getMacAddress(), c.getZone(), c.getStatus(), c.getLastSeen(), c.getPairedAt(), c.getPairedBy(),
                pairing.deviceKey()));
    }

    @GetMapping("/patient/{patientId}")
    public List<CameraResponse> forPatient(@PathVariable String patientId, @AuthenticationPrincipal Jwt jwt) {
        access.check(jwt, patientId);
        return cameras.forPatient(patientId).stream().map(CameraResponse::of).toList();
    }

    @GetMapping("/{id}")
    public CameraResponse get(@PathVariable UUID id, @AuthenticationPrincipal Jwt jwt) {
        CameraDevice camera = cameras.get(id);
        access.check(jwt, camera.getPatientId());
        return CameraResponse.of(camera);
    }

    @PutMapping("/{id}/status")
    public CameraResponse updateStatus(@PathVariable UUID id, @RequestParam CameraStatus status,
                                       @AuthenticationPrincipal Jwt jwt) {
        access.check(jwt, cameras.get(id).getPatientId());
        return CameraResponse.of(cameras.updateStatus(id, status));
    }

    @DeleteMapping("/{id}/unpair")
    public ResponseEntity<Void> unpair(@PathVariable UUID id, @AuthenticationPrincipal Jwt jwt) {
        access.check(jwt, cameras.get(id).getPatientId());
        cameras.unpair(id);
        return ResponseEntity.noContent().build();
    }
}
