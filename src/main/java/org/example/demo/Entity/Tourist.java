package org.example.demo.Entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

import org.example.demo.enums.Touristtype;

@Entity
@Table(name = "tourists")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Tourist {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true)
    private String firebaseUid;

    @Enumerated(EnumType.STRING)
    private Touristtype touristType;

    @Column(unique = true)
    private String aadhaarNumber;

    @Column(unique = true)
    private String passportNumber;

    private String country;
    private String nationality;

    @Column(unique = true)
    private String mobileNumber;

    private String fullName;
    private String email;
    private String age;
    private String dateOfBirth;
    private String gender;
    private String address;

    private String arrivalDate;
    private String departureDate;
    private String bloodGroup;
    private String medicalConditions;
    private String allergies;
    private Boolean hasDisability;
    private String disabilityDetails;
    private String emergencyContact;

    private String role;
    private Boolean profileCompleted;

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    @PrePersist
    public void prePersist() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    public void preUpdate() {
        updatedAt = LocalDateTime.now();
    }
}