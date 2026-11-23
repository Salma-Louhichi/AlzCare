package com.alzcare.careteam.controller;

import com.alzcare.careteam.dto.CareTeamStatsResponse;
import com.alzcare.careteam.service.CareTeamStatsService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/care-team")
public class CareTeamStatsController {

    private final CareTeamStatsService statsService;

    public CareTeamStatsController(CareTeamStatsService statsService) {
        this.statsService = statsService;
    }

    @GetMapping("/stats")
    public ResponseEntity<CareTeamStatsResponse> stats() {
        return ResponseEntity.ok(statsService.aggregate());
    }
}
