package org.example.demo.controller;

import org.example.demo.Service.touristservice;
import org.example.demo.dto.profile;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/tourist")
@CrossOrigin(origins = "*")
public class touristcontroller {

    @Autowired
    private touristservice touristService;

    // ==========================================
    // SAVE / UPDATE PROFILE (from Frontend / Firebase)
    // ==========================================
    @PostMapping("/profile")
    public ResponseEntity<profile> saveProfile(@RequestBody profile request) {
        profile saved = touristService.saveOrUpdateProfile(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(saved);
    }

    // ==========================================
    // GET PROFILE (by uid, mobileNumber, email, or id)
    // ==========================================
    @GetMapping("/profile")
    public ResponseEntity<profile> getProfile(
            @RequestParam(required = false) String uid,
            @RequestParam(required = false) String mobileNumber,
            @RequestParam(required = false) String email,
            @RequestParam(required = false) Long id) {

        if (uid != null && !uid.isBlank()) {
            return ResponseEntity.ok(touristService.getProfileByUid(uid));
        }
        if (id != null) {
            return ResponseEntity.ok(touristService.getProfileById(id));
        }
        if (mobileNumber != null && !mobileNumber.isBlank()) {
            return ResponseEntity.ok(touristService.getProfile(mobileNumber));
        }
        if (email != null && !email.isBlank()) {
            return ResponseEntity.ok(touristService.getProfileByEmail(email));
        }

        List<profile> all = touristService.getAllProfiles();
        if (!all.isEmpty()) {
            return ResponseEntity.ok(all.get(0));
        }

        throw new RuntimeException("Please provide uid, mobileNumber, email, or id query parameter to fetch profile.");
    }

    // ==========================================
    // GET PROFILE BY PATH VARIABLE (uid, phone, email, id)
    // ==========================================
    @GetMapping("/profile/{identifier}")
    public ResponseEntity<profile> getProfileByIdentifier(@PathVariable String identifier) {
        return ResponseEntity.ok(touristService.getProfileByIdentifier(identifier));
    }

    // ==========================================
    // TOURIST ID / DIGITAL ID ENDPOINT
    // ==========================================
    @GetMapping("/id")
    public ResponseEntity<profile> getTouristId(
            @RequestParam(required = false) String uid,
            @RequestParam(required = false) String mobileNumber,
            @RequestParam(required = false) String email) {

        String identifier = uid != null && !uid.isBlank() ? uid : (mobileNumber != null ? mobileNumber : email);
        if (identifier == null || identifier.isBlank()) {
            List<profile> all = touristService.getAllProfiles();
            if (!all.isEmpty()) {
                return ResponseEntity.ok(all.get(0));
            }
            throw new RuntimeException("No profile found. Please provide uid, mobileNumber, or email.");
        }
        return ResponseEntity.ok(touristService.getProfileByIdentifier(identifier));
    }

    // ==========================================
    // UPDATE PROFILE
    // ==========================================
    @PutMapping("/profile")
    public ResponseEntity<profile> updateProfile(
            @RequestParam(required = false) String uid,
            @RequestParam(required = false) String mobileNumber,
            @RequestBody profile request) {

        String identifier = uid;
        if (identifier == null || identifier.isBlank()) {
            identifier = mobileNumber;
        }
        if (identifier == null || identifier.isBlank()) {
            identifier = request.getFirebaseUid();
        }
        if (identifier == null || identifier.isBlank()) {
            identifier = request.getMobileNumber();
        }
        if (identifier == null || identifier.isBlank()) {
            if (request.getId() != null) {
                identifier = String.valueOf(request.getId());
            } else if (request.getEmail() != null) {
                identifier = request.getEmail();
            }
        }

        if (identifier == null || identifier.isBlank()) {
            // If no identifier is passed, just saveOrUpdate using whatever request fields are available
            return ResponseEntity.ok(touristService.saveOrUpdateProfile(request));
        }

        return ResponseEntity.ok(touristService.updateProfileByIdentifier(identifier, request));
    }

    // ==========================================
    // UPDATE PROFILE BY PATH VARIABLE
    // ==========================================
    @PutMapping("/profile/{identifier}")
    public ResponseEntity<profile> updateProfileByIdentifier(
            @PathVariable String identifier,
            @RequestBody profile request) {

        return ResponseEntity.ok(touristService.updateProfileByIdentifier(identifier, request));
    }

    // ==========================================
    // GET ALL PROFILES
    // ==========================================
    @GetMapping("/profiles")
    public ResponseEntity<List<profile>> getAllProfiles() {
        return ResponseEntity.ok(touristService.getAllProfiles());
    }

    // ==========================================
    // DELETE PROFILE
    // ==========================================
    @DeleteMapping("/profile/{identifier}")
    public ResponseEntity<String> deleteProfile(@PathVariable String identifier) {
        touristService.deleteProfile(identifier);
        return ResponseEntity.ok("Profile deleted successfully");
    }
}