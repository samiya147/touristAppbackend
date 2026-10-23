package org.example.demo.Service;

import org.example.demo.Entity.Tourist;
import org.example.demo.Repository.TouristRepository;
import org.example.demo.dto.profile;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class TouristServiceImpl implements touristservice {

    @Autowired
    private TouristRepository touristRepository;

    // ===================================
    // SAVE OR UPDATE PROFILE FROM FRONTEND
    // ===================================
    @Override
    public profile saveOrUpdateProfile(profile request) {
        Tourist tourist = null;

        // 1. Try to find existing by firebaseUid
        if (request.getFirebaseUid() != null && !request.getFirebaseUid().isBlank()) {
            tourist = touristRepository.findByFirebaseUid(request.getFirebaseUid()).orElse(null);
        }

        // 2. Try to find by ID
        if (tourist == null && request.getId() != null) {
            tourist = touristRepository.findById(request.getId()).orElse(null);
        }

        // 3. Try to find by mobileNumber
        if (tourist == null && request.getMobileNumber() != null && !request.getMobileNumber().isBlank()) {
            tourist = touristRepository.findByMobileNumber(request.getMobileNumber()).orElse(null);
        }

        // 4. Try to find by email
        if (tourist == null && request.getEmail() != null && !request.getEmail().isBlank()) {
            tourist = touristRepository.findByEmail(request.getEmail()).orElse(null);
        }

        // 5. If not found, create new Tourist
        if (tourist == null) {
            tourist = new Tourist();
        }

        applyProfileToTourist(tourist, request);
        Tourist savedTourist = touristRepository.save(tourist);
        return toProfileDto(savedTourist);
    }

    // =========================
    // CREATE PROFILE
    // =========================
    @Override
    public profile createProfile(profile request) {
        return saveOrUpdateProfile(request);
    }

    // =========================
    // GET PROFILE BY FIREBASE UID
    // =========================
    @Override
    public profile getProfileByUid(String uid) {
        Tourist tourist = touristRepository.findByFirebaseUid(uid)
                .orElseThrow(() ->
                        new RuntimeException("Tourist profile not found with UID: " + uid));

        return toProfileDto(tourist);
    }

    // =========================
    // GET PROFILE BY MOBILE
    // =========================
    @Override
    public profile getProfile(String mobileNumber) {
        Tourist tourist = touristRepository.findByMobileNumber(mobileNumber)
                .orElseThrow(() ->
                        new RuntimeException("Tourist profile not found with mobile number: " + mobileNumber));

        return toProfileDto(tourist);
    }

    // =========================
    // GET PROFILE BY ID
    // =========================
    @Override
    public profile getProfileById(Long id) {
        Tourist tourist = touristRepository.findById(id)
                .orElseThrow(() ->
                        new RuntimeException("Tourist profile not found with ID: " + id));

        return toProfileDto(tourist);
    }

    // =========================
    // GET PROFILE BY EMAIL
    // =========================
    @Override
    public profile getProfileByEmail(String email) {
        Tourist tourist = touristRepository.findByEmail(email)
                .orElseThrow(() ->
                        new RuntimeException("Tourist profile not found with email: " + email));

        return toProfileDto(tourist);
    }

    // =========================
    // GET PROFILE BY IDENTIFIER (uid / phone / email / id)
    // =========================
    @Override
    public profile getProfileByIdentifier(String identifier) {
        Tourist tourist = getTouristByIdentifier(identifier);
        return toProfileDto(tourist);
    }

    // =========================
    // UPDATE PROFILE BY MOBILE
    // =========================
    @Override
    public profile updateProfile(String mobileNumber, profile request) {
        Tourist tourist = touristRepository.findByMobileNumber(mobileNumber)
                .orElseThrow(() ->
                        new RuntimeException("Tourist profile not found with mobile number: " + mobileNumber));

        applyProfileToTourist(tourist, request);
        Tourist savedTourist = touristRepository.save(tourist);

        return toProfileDto(savedTourist);
    }

    // =========================
    // UPDATE PROFILE BY ID
    // =========================
    @Override
    public profile updateProfileById(Long id, profile request) {
        Tourist tourist = touristRepository.findById(id)
                .orElseThrow(() ->
                        new RuntimeException("Tourist profile not found with ID: " + id));

        applyProfileToTourist(tourist, request);
        Tourist savedTourist = touristRepository.save(tourist);

        return toProfileDto(savedTourist);
    }

    // =========================
    // UPDATE PROFILE BY IDENTIFIER
    // =========================
    @Override
    public profile updateProfileByIdentifier(String identifier, profile request) {
        Tourist tourist = getTouristByIdentifier(identifier);
        applyProfileToTourist(tourist, request);
        Tourist savedTourist = touristRepository.save(tourist);

        return toProfileDto(savedTourist);
    }

    // =========================
    // GET ALL PROFILES
    // =========================
    @Override
    public List<profile> getAllProfiles() {
        return touristRepository.findAll()
                .stream()
                .map(this::toProfileDto)
                .toList();
    }

    // =========================
    // DELETE PROFILE
    // =========================
    @Override
    public void deleteProfile(String identifier) {
        Tourist tourist = getTouristByIdentifier(identifier);
        touristRepository.delete(tourist);
    }

    // =========================
    // GET TOURIST BY EMAIL
    // =========================
    @Override
    public Tourist getTouristByEmail(String email) {
        return touristRepository.findByEmail(email)
                .orElseThrow(() ->
                        new RuntimeException("Tourist not found with email: " + email));
    }

    // =========================
    // GET TOURIST BY IDENTIFIER (uid / phone / email / id)
    // =========================
    @Override
    public Tourist getTouristByIdentifier(String identifier) {
        if (identifier == null || identifier.isBlank()) {
            throw new IllegalArgumentException("Identifier must not be empty");
        }

        // 1. Try Firebase UID
        Optional<Tourist> byUid = touristRepository.findByFirebaseUid(identifier);
        if (byUid.isPresent()) {
            return byUid.get();
        }

        // 2. Try numeric database ID
        if (identifier.matches("^\\d+$")) {
            try {
                Long id = Long.parseLong(identifier);
                Optional<Tourist> byId = touristRepository.findById(id);
                if (byId.isPresent()) {
                    return byId.get();
                }
            } catch (NumberFormatException ignored) {
            }
        }

        // 3. Try mobile number
        Optional<Tourist> byMobile = touristRepository.findByMobileNumber(identifier);
        if (byMobile.isPresent()) {
            return byMobile.get();
        }

        // 4. Try email
        Optional<Tourist> byEmail = touristRepository.findByEmail(identifier);
        if (byEmail.isPresent()) {
            return byEmail.get();
        }

        throw new RuntimeException("Tourist profile not found with identifier: " + identifier);
    }

    // =========================
    // HELPER: Map DTO to Entity
    // =========================
    private void applyProfileToTourist(Tourist tourist, profile request) {
        if (request.getFirebaseUid() != null) tourist.setFirebaseUid(request.getFirebaseUid());
        if (request.getFullName() != null) tourist.setFullName(request.getFullName());
        if (request.getEmail() != null) tourist.setEmail(request.getEmail());
        if (request.getMobileNumber() != null) tourist.setMobileNumber(request.getMobileNumber());
        if (request.getAge() != null) tourist.setAge(request.getAge());
        if (request.getDateOfBirth() != null) tourist.setDateOfBirth(request.getDateOfBirth());
        if (request.getGender() != null) tourist.setGender(request.getGender());
        if (request.getAddress() != null) tourist.setAddress(request.getAddress());
        if (request.getEmergencyContact() != null) tourist.setEmergencyContact(request.getEmergencyContact());
        if (request.getCountry() != null) tourist.setCountry(request.getCountry());
        if (request.getNationality() != null) tourist.setNationality(request.getNationality());
        if (request.getTouristType() != null) tourist.setTouristType(request.getTouristType());
        if (request.getAadhaarNumber() != null) tourist.setAadhaarNumber(request.getAadhaarNumber());
        if (request.getPassportNumber() != null) tourist.setPassportNumber(request.getPassportNumber());
        if (request.getArrivalDate() != null) tourist.setArrivalDate(request.getArrivalDate());
        if (request.getDepartureDate() != null) tourist.setDepartureDate(request.getDepartureDate());
        if (request.getBloodGroup() != null) tourist.setBloodGroup(request.getBloodGroup());
        if (request.getMedicalConditions() != null) tourist.setMedicalConditions(request.getMedicalConditions());
        if (request.getAllergies() != null) tourist.setAllergies(request.getAllergies());
        if (request.getHasDisability() != null) tourist.setHasDisability(request.getHasDisability());
        if (request.getDisabilityDetails() != null) tourist.setDisabilityDetails(request.getDisabilityDetails());
        if (request.getRole() != null) tourist.setRole(request.getRole());
        if (request.getProfileCompleted() != null) tourist.setProfileCompleted(request.getProfileCompleted());
    }

    // =========================
    // HELPER: Map Entity to DTO
    // =========================
    private profile toProfileDto(Tourist tourist) {
        if (tourist == null) return null;

        return profile.builder()
                .id(tourist.getId())
                .firebaseUid(tourist.getFirebaseUid())
                .fullName(tourist.getFullName())
                .email(tourist.getEmail())
                .mobileNumber(tourist.getMobileNumber())
                .age(tourist.getAge())
                .dateOfBirth(tourist.getDateOfBirth())
                .gender(tourist.getGender())
                .address(tourist.getAddress())
                .emergencyContact(tourist.getEmergencyContact())
                .country(tourist.getCountry())
                .nationality(tourist.getNationality())
                .touristType(tourist.getTouristType())
                .aadhaarNumber(tourist.getAadhaarNumber())
                .passportNumber(tourist.getPassportNumber())
                .arrivalDate(tourist.getArrivalDate())
                .departureDate(tourist.getDepartureDate())
                .bloodGroup(tourist.getBloodGroup())
                .medicalConditions(tourist.getMedicalConditions())
                .allergies(tourist.getAllergies())
                .hasDisability(tourist.getHasDisability())
                .disabilityDetails(tourist.getDisabilityDetails())
                .role(tourist.getRole())
                .profileCompleted(tourist.getProfileCompleted())
                .build();
    }
}