package com.alzcare.careteam.controller;

import com.alzcare.careteam.service.TomTomService;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/care-team/routing")
public class TomTomController {

    private final TomTomService tomTomService;

    public TomTomController(TomTomService tomTomService) {
        this.tomTomService = tomTomService;
    }

    @GetMapping("/travel-time")
    public Object getTravelTime(
            @RequestParam double originLat,
            @RequestParam double originLon,
            @RequestParam double destLat,
            @RequestParam double destLon) {
        return tomTomService.getRoute(originLat, originLon, destLat, destLon);
    }
}