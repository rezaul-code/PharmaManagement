package com.myspringboot.SpringBootApp.model;

import jakarta.persistence.*;

@Entity
@Table(name = "users")
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;                   // Long (capital L) — consistent with other models

    @Column(nullable = false)
    private String username;

    @Column(unique = true)
    private String email;

    private String phone;

    @Column(nullable = false)
    private String password;

    // ─── Constructors ────────────────────────────────────────────────

    public User() {}

    public User(String username, String email, String password) {
        this.username = username;
        this.email    = email;
        this.password = password;
    }

    // ─── Helpers ─────────────────────────────────────────────────────

    /**
     * Returns the first letter of the username for the avatar widget.
     * e.g. "Admin" → "A"
     */
    public String getAvatarInitial() {
        if (username == null || username.isBlank()) return "?";
        return String.valueOf(username.charAt(0)).toUpperCase();
    }

    // ─── Getters & Setters ───────────────────────────────────────────

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
}