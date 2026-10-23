
package org.example.demo.Service;

import org.example.demo.dto.MLPredictionRequest;
import org.example.demo.dto.MLPredictionResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

@Service
public class MLPredictionService {

    private final RestTemplate restTemplate = new RestTemplate();

    @Value("${ml.service.url}")
    private String mlServiceUrl;

    public double predictSafety(MLPredictionRequest request) {

        ResponseEntity<MLPredictionResponse> response =
                restTemplate.postForEntity(
                        mlServiceUrl,
                        request,
                        MLPredictionResponse.class
                );

        if (response.getBody() == null) {
            throw new RuntimeException(
                    "Python ML service returned an empty response"
            );
        }

        return response.getBody().getSafety_score();
    }
}
