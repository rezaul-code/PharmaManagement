package com.myspringboot.SpringBootApp.Service;

import com.myspringboot.SpringBootApp.model.Role;
import com.myspringboot.SpringBootApp.model.User;
import com.myspringboot.SpringBootApp.repo.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class StaffService {

    @Autowired private UserRepository userRepository;

    // ── Create staff directly ─────────────────────────────────────────

    @Transactional
    public void createStaff(String email, String password,
                             String username, Role role, User owner) {
        assertOwner(owner);

        if (userRepository.existsByEmailAndPharmacyId(
                email, owner.getPharmacy().getId())) {
            throw new IllegalArgumentException(
                    email + " is already a member of your pharmacy.");
        }

        User user = new User();
        user.setUsername(username);
        user.setEmail(email);
        user.setPassword(password); // hash this if using BCrypt
        user.setRole(role);
        user.setPharmacy(owner.getPharmacy());
        user.setStatus("ACTIVE");

        userRepository.save(user);
    }

    // ── Approve a PENDING user ────────────────────────────────────────

    @Transactional
    public void approveStaff(Long userId, User owner) {
        assertOwner(owner);
        User target = findInOwnerPharmacy(userId, owner);
        target.setStatus("ACTIVE");
        userRepository.save(target);
    }

    // ── Update role ───────────────────────────────────────────────────

    @Transactional
    public void updateRole(Long userId, Role newRole, User owner) {
        assertOwner(owner);
        User target = findInOwnerPharmacy(userId, owner);
        if (target.isOwner()) {
            throw new IllegalArgumentException(
                    "Cannot change another OWNER's role.");
        }
        target.setRole(newRole);
        userRepository.save(target);
    }

    // ── Delete staff ──────────────────────────────────────────────────

    @Transactional
    public void deleteStaff(Long userId, User owner) {
        assertOwner(owner);
        if (owner.getId().equals(userId)) {
            throw new IllegalArgumentException("You cannot remove yourself.");
        }
        User target = findInOwnerPharmacy(userId, owner);
        userRepository.delete(target);
    }

    // ── Queries ───────────────────────────────────────────────────────

    public List<User> getStaffForPharmacy(Long pharmacyId) {
        return userRepository.findByPharmacyId(pharmacyId);
    }

    // ── Internal helpers ──────────────────────────────────────────────

    private void assertOwner(User user) {
        if (user == null || !user.isOwner()) {
            throw new SecurityException("Only an OWNER can manage staff.");
        }
    }

    private User findInOwnerPharmacy(Long userId, User owner) {
        return userRepository
                .findByIdAndPharmacyId(userId, owner.getPharmacy().getId())
                .orElseThrow(() -> new IllegalArgumentException(
                        "User not found in your pharmacy."));
    }
}