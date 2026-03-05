package com.myspringboot.SpringBootApp.repo;

import com.myspringboot.SpringBootApp.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {

    // ── Pharmacy-scoped lookups (used post-login) ─────────────────────
    User findByEmailAndPharmacyId(String email, Long pharmacyId);
    User findByPhoneAndPharmacyId(String phone, Long pharmacyId);
    Optional<User> findByIdAndPharmacyId(Long id, Long pharmacyId);
    boolean existsByEmailAndPharmacyId(String email, Long pharmacyId);
    boolean existsByPhoneAndPharmacyId(String phone, Long pharmacyId);
    List<User> findByPharmacyId(Long pharmacyId);

    // ── Global lookups (used during login — no pharmacy context yet) ──
    User findByEmail(String email);
    User findByPhone(String phone);

    // ── Migration helper ──────────────────────────────────────────────
    List<User> findByPharmacyIsNull();
}