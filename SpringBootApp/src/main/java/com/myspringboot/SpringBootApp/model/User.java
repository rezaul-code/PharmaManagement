package com.myspringboot.SpringBootApp.model;

import jakarta.persistence.*;

@Entity
@Table(
    name = "users",
    uniqueConstraints = {
        @UniqueConstraint(name = "uk_users_pharmacy_email", columnNames = {"pharmacy_id", "email"}),
        @UniqueConstraint(name = "uk_users_pharmacy_phone", columnNames = {"pharmacy_id", "phone"})
    }
)
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String username;

    private String email;
    private String phone;

    @Column(nullable = false)
    private String password;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Role role = Role.STAFF; // default safety net
    
    @Column(nullable = false)
    private String status = "ACTIVE";

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "pharmacy_id")
    private Pharmacy pharmacy;

    public User() {}

    public User(String username, String email, String password) {
        this.username = username;
        this.email = email;
        this.password = password;
    }

    public String getAvatarInitial() {
        if (username == null || username.isBlank()) return "?";
        return String.valueOf(username.charAt(0)).toUpperCase();
    }

    // ── Getters & Setters ─────────────────────────────────────────────

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public String getPhone() { return phone; }
    public void setPhone(String phone) { this.phone = phone; }

    public String getPassword() { return password; }
    public void setPassword(String password) { this.password = password; }

    public Role getRole() { return role; }
    public void setRole(Role role) { this.role = role; }

    public Pharmacy getPharmacy() { return pharmacy; }
    public void setPharmacy(Pharmacy pharmacy) { this.pharmacy = pharmacy; }

    // ── Convenience helpers used in controllers / Thymeleaf ───────────

    public boolean isOwner() { return Role.OWNER.equals(this.role); }
    public boolean isPharmacist() { return Role.PHARMACIST.equals(this.role); }
    public boolean isStaff() { return Role.STAFF.equals(this.role); }

    /** True if user can manage medicines (OWNER or PHARMACIST). */
    public boolean canManageMedicines() {
        return isOwner() || isPharmacist();
    }

    /** True if user can create bills (all roles). */
    public boolean canCreateBills() {
        return true;
    }

    /** True if user can manage staff (OWNER only). */
    public boolean canManageStaff() {
        return isOwner();
    }
    
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
}