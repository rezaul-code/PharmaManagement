package com.myspringboot.SpringBootApp.dto;

public class LoginDto {
    private String identifier; // email or username
    private String password;

    public String getIdentifier() { return identifier; }
    public void setIdentifier(String identifier) { this.identifier = identifier; }
    public String getPassword() { return password; }
    public void setPassword(String password) { this.password = password; }
}
