package com.myspringboot.SpringBootApp.repo;

import com.myspringboot.SpringBootApp.model.Role;
import com.myspringboot.SpringBootApp.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {

    // ── Pharmacy-scoped lookups ───────────────────────────────────────
    User findByEmailAndPharmacyId(String email, Long pharmacyId);
    User findByPhoneAndPharmacyId(String phone, Long pharmacyId);
    Optional<User> findByIdAndPharmacyId(Long id, Long pharmacyId);
    boolean existsByEmailAndPharmacyId(String email, Long pharmacyId);
    boolean existsByPhoneAndPharmacyId(String phone, Long pharmacyId);

    @Query("SELECT u FROM User u WHERE u.pharmacy.id = :pharmacyId")
    List<User> findByPharmacyId(@Param("pharmacyId") Long pharmacyId);

    // ── Role count per pharmacy — used for limit enforcement ──────────
    long countByPharmacyIdAndRole(Long pharmacyId, Role role);

    // ── Global lookups (login) ────────────────────────────────────────
    Optional<User> findByEmail(String email);
    User findByPhone(String phone);

    // ── Migration helper ──────────────────────────────────────────────
    List<User> findByPharmacyIsNull();
    
    @Query("""
    		SELECT u
    		FROM User u
    		LEFT JOIN FETCH u.pharmacy
    		WHERE u.email = :email
    		""")
    		Optional<User> findByEmailWithPharmacy(@Param("email") String email);

    		@Query("""
    		SELECT u
    		FROM User u
    		LEFT JOIN FETCH u.pharmacy
    		WHERE u.phone = :phone
    		""")
    		User findByPhoneWithPharmacy(@Param("phone") String phone);
    
}