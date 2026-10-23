package org.example.demo.Service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.Map;

@Service
public class OrsRouteService {

    @Value("${ors.api.key}")
    private String apiKey;

    private final RestTemplate restTemplate = new RestTemplate();

    public String getRoutes(double startLon, double startLat,
                            double endLon, double endLat) {

        String url =
                "https://api.openrouteservice.org/v2/directions/driving-car/geojson";

        HttpHeaders headers = new HttpHeaders();
        headers.set("Authorization", apiKey);
        headers.setContentType(MediaType.APPLICATION_JSON);

        Map<String, Object> body = Map.of(
                "coordinates", new double[][] {
                        {startLon, startLat},
                        {endLon, endLat}
                },
                "alternative_routes", Map.of(
                        "target_count", 3,
                        "share_factor", 0.6,
                        "weight_factor", 1.4
                )
        );

        HttpEntity<Map<String, Object>> request =
                new HttpEntity<>(body, headers);

        ResponseEntity<String> response = restTemplate.exchange(
                url, HttpMethod.POST, request, String.class
        );

        return response.getBody();
    }
}