package com.myspringboot.SpringBootApp.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public class RegistrationDto {
    @NotBlank
    private String pharmacyName;
    
    @NotBlank
    @Email
    private String email;
    
    private String phone;
    
    @NotBlank
    private String ownerName;
    
    @NotBlank
    @Size(min = 6)
    private String password;

    public String getPharmacyName() { return pharmacyName; }
    public void setPharmacyName(String pharmacyName) { this.pharmacyName = pharmacyName; }
    
    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }
    
    public String getPhone() { return phone; }
    public void setPhone(String phone) { this.phone = phone; }
    
    public String getOwnerName() { return ownerName; }
    public void setOwnerName(String ownerName) { this.ownerName = ownerName; }
    
    public String getPassword() { return password; }
    public void setPassword(String password) { this.password = password; }
}
