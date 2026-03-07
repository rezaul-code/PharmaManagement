package com.myspringboot.SpringBootApp.Service;

import com.myspringboot.SpringBootApp.model.*;
import com.myspringboot.SpringBootApp.repo.StaffInviteRepository;
import com.myspringboot.SpringBootApp.repo.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Service
public class StaffService {

    @Autowired private StaffInviteRepository inviteRepository;
    @Autowired private UserRepository        userRepository;
    @Autowired private JavaMailSender        mailSender;

    @Value("${app.base-url}")
    private String baseUrl;

    // ── Invite ────────────────────────────────────────────────────────

    @Transactional
    public void inviteStaff(String email, Role role, User owner) {
        assertOwner(owner);

        if (inviteRepository.existsByEmailAndPharmacyIdAndUsedFalse(
                email, owner.getPharmacy().getId())) {
            throw new IllegalArgumentException(
                    "A pending invite already exists for " + email);
        }

        if (userRepository.existsByEmailAndPharmacyId(
                email, owner.getPharmacy().getId())) {
            throw new IllegalArgumentException(
                    email + " is already a member of your pharmacy.");
        }

        StaffInvite invite = new StaffInvite();
        invite.setToken(UUID.randomUUID().toString());
        invite.setEmail(email);
        invite.setRole(role);
        invite.setPharmacy(owner.getPharmacy());
        invite.setExpiresAt(LocalDateTime.now().plusHours(24));
        inviteRepository.save(invite);

        sendInviteEmail(email, invite.getToken(), owner.getPharmacy().getName());
    }

    private void sendInviteEmail(String to, String token, String pharmacyName) {
        String link = baseUrl + "/staff/accept?token=" + token;

        SimpleMailMessage msg = new SimpleMailMessage();
        msg.setTo(to);
        msg.setSubject("You have been invited to join " + pharmacyName);
        msg.setText(
            "Hello,\n\n" +
            "You have been invited to join " + pharmacyName + " as a staff member.\n\n" +
            "Click the link below to set up your account (valid for 24 hours):\n" +
            link + "\n\n" +
            "If you did not expect this invitation, you can ignore this email."
        );
        mailSender.send(msg);
    }

    // ── Validate token ────────────────────────────────────────────────

    public StaffInvite validateToken(String token) {
        StaffInvite invite = inviteRepository.findByToken(token)
                .orElseThrow(() -> new IllegalArgumentException(
                        "Invalid invitation link."));
        if (!invite.isValid()) {
            throw new IllegalArgumentException(
                    "This invitation has already been used or has expired.");
        }
        return invite;
    }

    // ── Accept invite ─────────────────────────────────────────────────

    @Transactional
    public User acceptInvite(String token, String username, String password) {
        StaffInvite invite = validateToken(token);

        User user = new User();
        user.setUsername(username);
        user.setEmail(invite.getEmail());
        user.setPassword(password);
        user.setRole(invite.getRole());
        user.setPharmacy(invite.getPharmacy());
        user.setStatus("ACTIVE");            // ← activated immediately on accept

        User saved = userRepository.save(user);

        invite.setUsed(true);
        inviteRepository.save(invite);

        return saved;
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

    @Transactional
    public void createStaff(String email, String password, String username, Role role, User owner) {
        assertOwner(owner);

        if (userRepository.existsByEmailAndPharmacyId(email, owner.getPharmacy().getId())) {
            throw new IllegalArgumentException(email + " is already a member of your pharmacy.");
        }

        User user = new User();
        user.setUsername(username);
        user.setEmail(email);
        user.setPassword(password); // hash this if you're using BCrypt
        user.setRole(role);
        user.setPharmacy(owner.getPharmacy());
        user.setStatus("ACTIVE");

        userRepository.save(user);
    }
}