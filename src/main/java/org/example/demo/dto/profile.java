package org.example.demo.dto;

import com.fasterxml.jackson.annotation.JsonAlias;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.example.demo.enums.Touristtype;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class profile {

    private Long id;

    @JsonAlias({"uid", "firebaseUid"})
    private String firebaseUid;

    @JsonAlias({"name", "fullName"})
    private String fullName;

    private String email;

    @JsonAlias({"phone", "mobileNumber"})
    private String mobileNumber;

    private String age;
    private String dateOfBirth;
    private String gender;
    private String address;

    @JsonAlias({"emergencyContacts", "emergencyContact"})
    private String emergencyContact;

    private String country;
    private String nationality;
    private Touristtype touristType;
    private String aadhaarNumber;
    private String passportNumber;

    private String arrivalDate;
    private String departureDate;
    private String bloodGroup;
    private String medicalConditions;
    private String allergies;
    private Boolean hasDisability;
    private String disabilityDetails;
    private String role;
    private Boolean profileCompleted;
}
