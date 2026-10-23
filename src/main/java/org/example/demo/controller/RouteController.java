package org.example.demo.controller;

import org.example.demo.Service.OrsRouteService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/routes")
public class RouteController {

    private final OrsRouteService orsRouteService;

    public RouteController(OrsRouteService orsRouteService) {
        this.orsRouteService = orsRouteService;
    }

    @PostMapping
    public ResponseEntity<?> getRoutes(
            @RequestBody Map<String, Double> request) {
        try {
            double startLon = request.get("startLon");
            double startLat = request.get("startLat");
            double endLon = request.get("endLon");
            double endLat = request.get("endLat");

            String routes = orsRouteService.getRoutes(
                    startLon, startLat, endLon, endLat);

            return ResponseEntity.ok(routes);

        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_GATEWAY)
                    .body(Map.of(
                            "error", "Failed to fetch routes from ORS",
                            "details", e.getMessage() == null
                                    ? "Unknown error" : e.getMessage()
                    ));
        }
    }
}