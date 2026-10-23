package org.example.demo.Repository;

import org.example.demo.Entity.Tourist;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface TouristRepository extends JpaRepository<Tourist, Long> {

    Optional<Tourist> findByFirebaseUid(String firebaseUid);

    boolean existsByFirebaseUid(String firebaseUid);

    boolean existsByMobileNumber(String mobileNumber);

    boolean existsByAadhaarNumber(String aadhaarNumber);

    boolean existsByPassportNumber(String passportNumber);

    Optional<Tourist> findByMobileNumber(String mobileNumber);

    Optional<Tourist> findByEmail(String email);
}