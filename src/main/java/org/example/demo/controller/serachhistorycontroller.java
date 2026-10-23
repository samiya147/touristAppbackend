package org.example.demo.controller;

import org.example.demo.Entity.SearchHistory;
import org.example.demo.Entity.Tourist;
import org.example.demo.Service.searchhistoryservice;
import org.example.demo.Service.touristservice;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/history")
@CrossOrigin(origins = "*")
class SearchHistoryController {

    private final searchhistoryservice searchHistoryService;
    private final touristservice touristService;

    public SearchHistoryController(
            searchhistoryservice searchHistoryService,
            touristservice touristService
    ) {
        this.searchHistoryService = searchHistoryService;
        this.touristService = touristService;
    }

    // Helper to resolve tourist
    private Tourist resolveTourist(String email) {
        if (email == null || email.isBlank()) {
            throw new IllegalArgumentException("Tourist email or identifier is required as query param ?email=...");
        }
        return touristService.getTouristByIdentifier(email);
    }

    // Save a search
    @PostMapping
    public ResponseEntity<SearchHistory> saveSearch(
            @RequestBody SearchHistoryRequest request,
            @RequestParam(required = false) String email
    ) {
        String touristEmail = request.getEmail() != null && !request.getEmail().isBlank()
                ? request.getEmail()
                : email;
        Tourist tourist = resolveTourist(touristEmail);

        SearchHistory savedSearch =
                searchHistoryService.saveSearch(
                        tourist,
                        request.getLatitude(),
                        request.getLongitude(),
                        request.getSafetyScore(),
                        request.getCategory(),
                        request.getTopRiskFactor()
                );

        return ResponseEntity.ok(savedSearch);
    }

    // Get recent 5 searches
    @GetMapping("/recent")
    public ResponseEntity<List<SearchHistory>> getRecentSearches(
            @RequestParam(required = false) String email
    ) {
        Tourist tourist = resolveTourist(email);

        return ResponseEntity.ok(
                searchHistoryService.getRecentSearches(tourist)
        );
    }

    // Get all searches
    @GetMapping
    public ResponseEntity<List<SearchHistory>> getAllSearches(
            @RequestParam(required = false) String email
    ) {
        Tourist tourist = resolveTourist(email);

        return ResponseEntity.ok(
                searchHistoryService.getAllSearches(tourist)
        );
    }

    // Get total number of searches
    @GetMapping("/count")
    public ResponseEntity<Long> getTotalSearches(
            @RequestParam(required = false) String email
    ) {
        Tourist tourist = resolveTourist(email);

        return ResponseEntity.ok(
                searchHistoryService.getTotalSearches(tourist)
        );
    }

    // Delete search history
    @DeleteMapping
    public ResponseEntity<String> deleteSearchHistory(
            @RequestParam(required = false) String email
    ) {
        Tourist tourist = resolveTourist(email);

        searchHistoryService.deleteSearchHistory(tourist);

        return ResponseEntity.ok("Search history deleted successfully");
    }


    // Request DTO
    public static class SearchHistoryRequest {

        private String email;

        private Double latitude;
        private Double longitude;
        private Double safetyScore;
        private String category;
        private String topRiskFactor;

        public Double getLatitude() {
            return latitude;
        }

        public void setLatitude(Double latitude) {
            this.latitude = latitude;
        }

        public Double getLongitude() {
            return longitude;
        }

        public void setLongitude(Double longitude) {
            this.longitude = longitude;
        }

        public Double getSafetyScore() {
            return safetyScore;
        }

        public void setSafetyScore(Double safetyScore) {
            this.safetyScore = safetyScore;
        }

        public String getCategory() {
            return category;
        }

        public void setCategory(String category) {
            this.category = category;
        }

        public String getEmail() {
            return email;
        }

        public void setEmail(String email) {
            this.email = email;
        }

        public String getTopRiskFactor() {
            return topRiskFactor;
        }

        public void setTopRiskFactor(String topRiskFactor) {
            this.topRiskFactor = topRiskFactor;
        }
    }
}