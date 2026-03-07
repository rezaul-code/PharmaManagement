package com.myspringboot.SpringBootApp.repo;

import com.myspringboot.SpringBootApp.model.StaffInvite;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface StaffInviteRepository extends JpaRepository<StaffInvite, Long> {
    Optional<StaffInvite> findByToken(String token);
    boolean existsByEmailAndPharmacyIdAndUsedFalse(String email, Long pharmacyId);
}