package com.alzcare.eventingestion.web;

import com.alzcare.eventingestion.security.DeviceAuthentication;
import com.alzcare.eventingestion.service.EventIngestionService;
import com.alzcare.eventingestion.service.PatientAccess;
import com.alzcare.eventingestion.web.Dtos.EventResponse;
import com.alzcare.eventingestion.web.Dtos.IngestResponse;
import com.alzcare.eventingestion.web.Dtos.MotionEventRequest;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

import java.time.Duration;
import java.util.List;

@RestController
@RequestMapping("/api/events")
public class EventController {

    private final EventIngestionService ingestion;
    private final PatientAccess access;

    public EventController(EventIngestionService ingestion, PatientAccess access) {
        this.ingestion = ingestion;
        this.access = access;
    }

    /** Called by a camera (X-Device-Id / X-Device-Key). */
    @PostMapping
    public ResponseEntity<IngestResponse> receive(@Valid @RequestBody MotionEventRequest request,
                                                  DeviceAuthentication device) {
        EventIngestionService.Ingested result = ingestion.ingest(device.cameraId(), request.timestamp(),
                request.motionIntensity(), request.durationSeconds());
        return ResponseEntity.status(HttpStatus.ACCEPTED).body(new IngestResponse(result.eventId(), result.newPatterns()));
    }

    /** Recent motion for the doctor's timeline (max 200 events). */
    @GetMapping("/patient/{patientId}")
    public List<EventResponse> recent(@PathVariable String patientId,
                                      @RequestParam(defaultValue = "24") int hours,
                                      @AuthenticationPrincipal Jwt jwt) {
        access.check(jwt, patientId);
        int bounded = Math.max(1, Math.min(hours, 24 * 7));
        return ingestion.recentForPatient(patientId, Duration.ofHours(bounded)).stream().map(EventResponse::of).toList();
    }
}
