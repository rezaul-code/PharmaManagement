package com.myspringboot.SpringBootApp.controller.api;

import com.myspringboot.SpringBootApp.Service.AuditLogService;
import com.myspringboot.SpringBootApp.Service.TenantPharmacyService;
import com.myspringboot.SpringBootApp.model.Pharmacy;
import com.myspringboot.SpringBootApp.model.User;
import com.myspringboot.SpringBootApp.repo.PharmacyRepository;
import com.myspringboot.SpringBootApp.repo.UserRepository;
import com.myspringboot.SpringBootApp.security.CustomUserDetails;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/pharmacy")
public class PharmacyRestController {

    private static final Logger log = LoggerFactory.getLogger(PharmacyRestController.class);

    @Autowired private PharmacyRepository pharmacyRepository;
    @Autowired private UserRepository userRepository;
    @Autowired private TenantPharmacyService tenantPharmacyService;
    @Autowired private AuditLogService auditLogService;

    @Value("${app.upload.logo-dir:uploads/logos/}")
    private String logoDir;

    private User getAuthenticatedOwner() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.getPrincipal() instanceof CustomUserDetails userDetails) {
            User user = userDetails.getUser();
            if (user.isOwner()) {
                return userRepository.findById(user.getId()).orElse(null);
            }
        }
        return null;
    }

    // ── GET /api/pharmacy ────────────────────────────────────────────────
    @GetMapping
    public ResponseEntity<?> getPharmacy() {
        User owner = getAuthenticatedOwner();
        if (owner == null) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(Map.of("error", "Access denied. Only owners can access settings."));
        }
        Long pharmacyId = tenantPharmacyService.getCurrentPharmacyId();
        Pharmacy pharmacy = pharmacyRepository.findById(pharmacyId)
                .orElseThrow(() -> new IllegalStateException("Pharmacy not found: " + pharmacyId));
        return ResponseEntity.ok(pharmacy);
    }

    // ── PUT /api/pharmacy ────────────────────────────────────────────────
    @PutMapping
    public ResponseEntity<?> updatePharmacy(@RequestBody Pharmacy formData) {
        User owner = getAuthenticatedOwner();
        if (owner == null) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of("error", "Access denied."));
        }

        Long pharmacyId = tenantPharmacyService.getCurrentPharmacyId();
        Pharmacy pharmacy = pharmacyRepository.findById(pharmacyId)
                .orElseThrow(() -> new IllegalStateException("Pharmacy not found: " + pharmacyId));

        pharmacy.setName(formData.getName());
        pharmacy.setGstNumber(formData.getGstNumber());
        pharmacy.setPhone(formData.getPhone());
        pharmacy.setAddress(formData.getAddress());
        pharmacy.setInvoiceFooter(formData.getInvoiceFooter());

        Pharmacy saved = pharmacyRepository.save(pharmacy);
        log.info("[SETTINGS] Saved settings via REST for pharmacyId={}", pharmacyId);
        
        return ResponseEntity.ok(saved);
    }

    // ── POST /api/pharmacy/logo ──────────────────────────────────────────
    @PostMapping("/logo")
    public ResponseEntity<?> uploadLogo(@RequestParam("logoFile") MultipartFile file) {
        User owner = getAuthenticatedOwner();
        if (owner == null) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of("error", "Access denied."));
        }

        if (file.isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of("error", "Please select a file to upload."));
        }

        long maxBytes = 5_242_880L; // 5 MB
        if (file.getSize() > maxBytes) {
            return ResponseEntity.badRequest().body(Map.of("error", "File is too large. Maximum allowed size is 5 MB."));
        }

        String originalFilename = file.getOriginalFilename();
        if (originalFilename == null || originalFilename.isBlank()) {
            return ResponseEntity.badRequest().body(Map.of("error", "Invalid file name."));
        }

        String lowerName = originalFilename.toLowerCase();
        if (!lowerName.endsWith(".png") && !lowerName.endsWith(".jpg") && !lowerName.endsWith(".jpeg")) {
            return ResponseEntity.badRequest().body(Map.of("error", "Only PNG and JPG files are allowed."));
        }

        try {
            Long pharmacyId = tenantPharmacyService.getCurrentPharmacyId();
            Pharmacy pharmacy = pharmacyRepository.findById(pharmacyId)
                    .orElseThrow(() -> new IllegalStateException("Pharmacy not found: " + pharmacyId));

            Path dir = Paths.get(logoDir).toAbsolutePath();
            Files.createDirectories(dir);

            String ext = lowerName.substring(lowerName.lastIndexOf('.'));
            String filename = "pharmacy-" + pharmacyId + ext;
            Path target = dir.resolve(filename);

            Path temp = Files.createTempFile("logo-upload-", ext);
            try {
                Files.copy(file.getInputStream(), temp, StandardCopyOption.REPLACE_EXISTING);

                String mimeType = Files.probeContentType(temp);
                if (mimeType != null && !mimeType.equals("image/png") && !mimeType.equals("image/jpeg")) {
                    return ResponseEntity.badRequest().body(Map.of("error", "Invalid file content. Only PNG/JPG are accepted."));
                }

                if (pharmacy.getLogoPath() != null) {
                    try {
                        Path oldPath = Paths.get(pharmacy.getLogoPath()).toAbsolutePath();
                        Files.deleteIfExists(oldPath);
                    } catch (Exception ex) {
                        log.warn("Could not delete old logo: {}", ex.getMessage());
                    }
                }

                Files.move(temp, target, StandardCopyOption.REPLACE_EXISTING);

            } finally {
                Files.deleteIfExists(temp);
            }

            String relativePath = logoDir + filename;
            pharmacy.setLogoPath(relativePath);
            pharmacyRepository.save(pharmacy);

            auditLogService.log(owner, pharmacyId, "LOGO_UPLOAD", "Pharmacy", pharmacyId, "Logo uploaded via REST: " + filename);

            return ResponseEntity.ok(Map.of("success", true, "logoPath", relativePath));

        } catch (IOException e) {
            log.error("IOException during REST logo upload", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "File upload failed: " + e.getMessage()));
        }
    }

    // ── DELETE /api/pharmacy/logo ────────────────────────────────────────
    @DeleteMapping("/logo")
    public ResponseEntity<?> removeLogo() {
        User owner = getAuthenticatedOwner();
        if (owner == null) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of("error", "Access denied."));
        }

        Long pharmacyId = tenantPharmacyService.getCurrentPharmacyId();
        Pharmacy pharmacy = pharmacyRepository.findById(pharmacyId)
                .orElseThrow(() -> new IllegalStateException("Pharmacy not found."));

        if (pharmacy.getLogoPath() != null) {
            try {
                Path oldPath = Paths.get(pharmacy.getLogoPath()).toAbsolutePath();
                Files.deleteIfExists(oldPath);
            } catch (Exception ex) {
                log.warn("Could not delete logo file: {}", ex.getMessage());
            }

            pharmacy.setLogoPath(null);
            pharmacyRepository.save(pharmacy);

            auditLogService.log(owner, pharmacyId, "LOGO_REMOVE", "Pharmacy", pharmacyId, "Logo removed via REST");

            return ResponseEntity.ok(Map.of("success", true, "message", "Logo removed successfully."));
        } else {
            return ResponseEntity.badRequest().body(Map.of("error", "No logo found to remove."));
        }
    }

    // ── GET /api/pharmacy/subscription ───────────────────────────────────
    @GetMapping("/subscription")
    public ResponseEntity<?> getSubscription() {
        User owner = getAuthenticatedOwner();
        if (owner == null) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of("error", "Access denied."));
        }
        Long pharmacyId = tenantPharmacyService.getCurrentPharmacyId();
        Pharmacy pharmacy = pharmacyRepository.findById(pharmacyId)
                .orElseThrow(() -> new IllegalStateException("Pharmacy not found: " + pharmacyId));

        Map<String, Object> subDetails = new HashMap<>();
        subDetails.put("subscriptionEndDate", pharmacy.getSubscriptionEndDate());
        subDetails.put("status", pharmacy.getStatus());
        subDetails.put("planType", pharmacy.getPlanType());
        
        return ResponseEntity.ok(subDetails);
    }
}
