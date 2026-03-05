package com.myspringboot.SpringBootApp.Service;

import com.myspringboot.SpringBootApp.model.Pharmacy;
import com.myspringboot.SpringBootApp.model.User;
import com.myspringboot.SpringBootApp.repo.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class UserService {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private TenantPharmacyService tenantPharmacyService;

    // ── Login: global search (no pharmacy context yet) ────────────────

    /**
     * Used by LoginController — searches across ALL pharmacies.
     * After finding the user, LoginController reads user.getPharmacy().getId()
     * and stores it in the session.
     */
    public User findByIdentifierGlobal(String identifier) {
        User byEmail = userRepository.findByEmail(identifier);
        if (byEmail != null) return byEmail;
        return userRepository.findByPhone(identifier);
    }

    // ── Legacy: pharmacy-scoped search (kept for compatibility) ───────
    public User findByIdentifier(String identifier) {
        return findByIdentifierGlobal(identifier);
    }

    // ── Registration checks ───────────────────────────────────────────

    public boolean existsByEmailInPharmacy(String email, Long pharmacyId) {
        return userRepository.existsByEmailAndPharmacyId(email, pharmacyId);
    }

    public boolean existsByPhoneInPharmacy(String phone, Long pharmacyId) {
        return userRepository.existsByPhoneAndPharmacyId(phone, pharmacyId);
    }

    public boolean existsByEmail(String email) {
        return userRepository.existsByEmailAndPharmacyId(
                email, tenantPharmacyService.getCurrentPharmacyId());
    }

    public boolean existsByPhone(String phone) {
        return userRepository.existsByPhoneAndPharmacyId(
                phone, tenantPharmacyService.getCurrentPharmacyId());
    }

    // ── Save ──────────────────────────────────────────────────────────

    /**
     * Signup flow: pharmacy is explicitly provided (no session yet).
     */
    public User saveUserWithPharmacy(User user, Pharmacy pharmacy) {
        user.setPharmacy(pharmacy);
        return userRepository.save(user);
    }

    /**
     * General save: links user to current session pharmacy.
     */
    public User saveUser(User user) {
        Pharmacy pharmacy;
        try {
            pharmacy = tenantPharmacyService.getCurrentPharmacy();
        } catch (Exception e) {
            pharmacy = tenantPharmacyService.getDefaultPharmacy();
        }
        user.setPharmacy(pharmacy);
        return userRepository.save(user);
    }

    // ── Queries ───────────────────────────────────────────────────────

    public Optional<User> findById(Long id) {
        return userRepository.findByIdAndPharmacyId(
                id, tenantPharmacyService.getCurrentPharmacyId());
    }

    public List<User> getAllUsersInCurrentPharmacy() {
        return userRepository.findByPharmacyId(
                tenantPharmacyService.getCurrentPharmacyId());
    }
}