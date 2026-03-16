package com.myspringboot.SpringBootApp.Service;

import com.myspringboot.SpringBootApp.dto.RegistrationDto;
import com.myspringboot.SpringBootApp.model.Pharmacy;
import com.myspringboot.SpringBootApp.model.Role;
import com.myspringboot.SpringBootApp.model.TenantStatus;
import com.myspringboot.SpringBootApp.model.User;
import com.myspringboot.SpringBootApp.repo.PharmacyRepository;
import com.myspringboot.SpringBootApp.repo.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.UUID;

@Service
public class TenantRegistrationService {
    @Autowired private PharmacyRepository pharmacyRepo;
    @Autowired private UserRepository userRepo;
    @Autowired private PasswordEncoder passwordEncoder;

    @Transactional
    public Pharmacy registerPharmacy(RegistrationDto req) {
        if (pharmacyRepo.existsByName(req.getPharmacyName())) {
            throw new RuntimeException("Pharmacy name already taken.");
        }
        if (userRepo.findByEmail(req.getEmail()).isPresent()) {
            throw new RuntimeException("Email already taken.");
        }

        Pharmacy p = new Pharmacy();
        p.setTenantId(UUID.randomUUID().toString());
        p.setName(req.getPharmacyName());
        p.setEmail(req.getEmail());
        p.setPhone(req.getPhone());
        p.setStatus(TenantStatus.ACTIVE); 
        p.setSubscriptionEndDate(LocalDate.now().plusDays(14)); // 14 day trial
        p.setCreatedAt(java.time.LocalDateTime.now());
        p.setPlanType("TRIAL");
        pharmacyRepo.save(p);
        
        User adminUser = new User();
        adminUser.setPharmacy(p);
        adminUser.setUsername(req.getOwnerName());
        adminUser.setEmail(req.getEmail());
        adminUser.setPhone(req.getPhone());
        adminUser.setPassword(passwordEncoder.encode(req.getPassword()));
        adminUser.setRole(Role.OWNER); // Mapped to OWNER in existing system
        adminUser.setCreatedAt(java.time.LocalDateTime.now());
        adminUser.setPlanType("TRIAL");
        userRepo.save(adminUser);
        
        return p;
    }
}
