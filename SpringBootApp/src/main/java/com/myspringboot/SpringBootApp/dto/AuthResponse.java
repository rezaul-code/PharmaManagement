package com.myspringboot.SpringBootApp.dto;

public class AuthResponse {
    private String token;
    private String role;
    private String tenantId;
    private Long pharmacyId;
    private String pharmacyName;
    private String username;

    public AuthResponse(String token, String role, String tenantId, Long pharmacyId, String pharmacyName, String username) {
        this.token = token;
        this.role = role;
        this.tenantId = tenantId;
        this.pharmacyId = pharmacyId;
        this.pharmacyName = pharmacyName;
        this.username = username;
    }

    public String getToken() { return token; }
    public void setToken(String token) { this.token = token; }
    
    public String getRole() { return role; }
    public void setRole(String role) { this.role = role; }
    
    public String getTenantId() { return tenantId; }
    public void setTenantId(String tenantId) { this.tenantId = tenantId; }
    
    public Long getPharmacyId() { return pharmacyId; }
    public void setPharmacyId(Long pharmacyId) { this.pharmacyId = pharmacyId; }
    
    public String getPharmacyName() { return pharmacyName; }
    public void setPharmacyName(String pharmacyName) { this.pharmacyName = pharmacyName; }
    
    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }
}
