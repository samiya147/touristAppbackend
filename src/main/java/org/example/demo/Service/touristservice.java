package org.example.demo.Service;

import org.example.demo.Entity.Tourist;
import org.example.demo.dto.profile;

import java.util.List;

public interface touristservice {

    profile saveOrUpdateProfile(profile request);
    profile createProfile(profile request);
    profile getProfile(String mobileNumber);
    profile getProfileById(Long id);
    profile getProfileByUid(String uid);
    profile getProfileByEmail(String email);
    profile getProfileByIdentifier(String identifier);
    profile updateProfile(String mobileNumber, profile request);
    profile updateProfileById(Long id, profile request);
    profile updateProfileByIdentifier(String identifier, profile request);
    List<profile> getAllProfiles();
    void deleteProfile(String identifier);

    Tourist getTouristByEmail(String email);
    Tourist getTouristByIdentifier(String identifier);
}