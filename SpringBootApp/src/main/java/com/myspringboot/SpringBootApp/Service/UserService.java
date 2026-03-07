package com.myspringboot.SpringBootApp.Service;

import com.myspringboot.SpringBootApp.model.Pharmacy;
import com.myspringboot.SpringBootApp.model.Role;
import com.myspringboot.SpringBootApp.model.User;
import com.myspringboot.SpringBootApp.repo.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class UserService {

    @Autowired private UserRepository userRepository;
    @Autowired private TenantPharmacyService tenantPharmacyService;

    // ── Login ─────────────────────────────────────────────────────────

    public User findByIdentifierGlobal(String identifier) {
        User byEmail = userRepository.findByEmail(identifier);
        if (byEmail != null) return byEmail;
        return userRepository.findByPhone(identifier);
    }

    public User findByIdentifier(String identifier) {
        return findByIdentifierGlobal(identifier);
    }

    // ── Existence checks ──────────────────────────────────────────────

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

    public User saveUserWithPharmacy(User user, Pharmacy pharmacy) {
        user.setPharmacy(pharmacy);
        return userRepository.save(user);
    }

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

    // ── Staff management (OWNER only) ─────────────────────────────────

    /**
     * Add a new staff/pharmacist user to the owner's pharmacy.
     * Validates uniqueness within that pharmacy.
     *
     * @param newUser   User object (username, email/phone, password already set)
     * @param role      PHARMACIST or STAFF
     * @param owner     The authenticated OWNER performing the action
     * @throws IllegalArgumentException on duplicate email/phone
     * @throws SecurityException        if caller is not OWNER
     */
    public User addStaff(User newUser, Role role, User owner) {
        assertOwner(owner);

        Pharmacy pharmacy = owner.getPharmacy();
        Long pharmacyId   = pharmacy.getId();

        if (newUser.getEmail() != null && !newUser.getEmail().isBlank()
                && existsByEmailInPharmacy(newUser.getEmail(), pharmacyId)) {
            throw new IllegalArgumentException(
                    "A user with this email already exists in your pharmacy.");
        }

        if (newUser.getPhone() != null && !newUser.getPhone().isBlank()
                && existsByPhoneInPharmacy(newUser.getPhone(), pharmacyId)) {
            throw new IllegalArgumentException(
                    "A user with this phone already exists in your pharmacy.");
        }

        newUser.setRole(role);
        newUser.setPharmacy(pharmacy);   // ← same pharmacy as owner (tenant-safe)
        return userRepository.save(newUser);
    }

    /**
     * Change the role of an existing staff member.
     * Target user must belong to the same pharmacy as the owner.
     */
    public User updateStaffRole(Long userId, Role newRole, User owner) {
        assertOwner(owner);

        User target = findStaffInOwnerPharmacy(userId, owner);
        if (target.isOwner()) {
            throw new IllegalArgumentException("Cannot change the role of another OWNER.");
        }
        target.setRole(newRole);
        return userRepository.save(target);
    }

    /**
     * Remove a staff member. Owner cannot delete themselves.
     */
    public void removeStaff(Long userId, User owner) {
        assertOwner(owner);

        if (owner.getId().equals(userId)) {
            throw new IllegalArgumentException("You cannot remove yourself.");
        }

        User target = findStaffInOwnerPharmacy(userId, owner);
        userRepository.delete(target);
    }

    // ── Internal helpers ──────────────────────────────────────────────

    private void assertOwner(User user) {
        if (user == null || !user.isOwner()) {
            throw new SecurityException("Only an OWNER can manage staff.");
        }
    }

    /** Fetches a user that belongs to the same pharmacy as the owner. */
    private User findStaffInOwnerPharmacy(Long userId, User owner) {
        return userRepository
                .findByIdAndPharmacyId(userId, owner.getPharmacy().getId())
                .orElseThrow(() -> new IllegalArgumentException(
                        "User not found in your pharmacy."));
    }
}