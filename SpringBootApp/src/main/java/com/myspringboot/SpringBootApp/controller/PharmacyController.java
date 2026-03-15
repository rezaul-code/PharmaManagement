package com.myspringboot.SpringBootApp.controller;

import com.myspringboot.SpringBootApp.Service.AuditLogService;
import com.myspringboot.SpringBootApp.Service.TenantPharmacyService;
import com.myspringboot.SpringBootApp.model.Pharmacy;
import com.myspringboot.SpringBootApp.model.User;
import com.myspringboot.SpringBootApp.repo.PharmacyRepository;
import jakarta.servlet.http.HttpSession;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;

@Controller
@RequestMapping("/pharmacy")
public class PharmacyController {

    private static final Logger log = LoggerFactory.getLogger(PharmacyController.class);

    @Autowired private PharmacyRepository pharmacyRepository;
    @Autowired private TenantPharmacyService tenantPharmacyService;
    @Autowired private AuditLogService auditLogService;

    @Value("${app.upload.logo-dir:uploads/logos/}")
    private String logoDir;

    // ── GET /pharmacy/settings ────────────────────────────────────────
    @GetMapping("/settings")
    public String settingsPage(HttpSession session, Model model) {

        User loggedInUser = (User) session.getAttribute("loggedInUser");
        if (loggedInUser == null || !loggedInUser.isOwner()) {
            log.warn("[SETTINGS] Unauthorized access attempt.");
            return "redirect:/dashboard";
        }

        Long pharmacyId = tenantPharmacyService.getCurrentPharmacyId();
        log.info("[SETTINGS] Loading settings for pharmacyId={}", pharmacyId);

        Pharmacy pharmacy = pharmacyRepository.findById(pharmacyId)
                .orElseThrow(() -> new IllegalStateException(
                        "Pharmacy not found for id: " + pharmacyId));

        model.addAttribute("pharmacy", pharmacy);
        return "pages/pharmacy_settings";
    }

    // ── POST /pharmacy/settings ───────────────────────────────────────
    @PostMapping("/settings")
    public String saveSettings(@ModelAttribute("pharmacy") Pharmacy formData,
                               HttpSession session,
                               RedirectAttributes redirectAttributes) {

        User loggedInUser = (User) session.getAttribute("loggedInUser");
        if (loggedInUser == null || !loggedInUser.isOwner()) {
            return "redirect:/dashboard";
        }

        Long pharmacyId = tenantPharmacyService.getCurrentPharmacyId();
        Pharmacy pharmacy = pharmacyRepository.findById(pharmacyId)
                .orElseThrow(() -> new IllegalStateException(
                        "Pharmacy not found for id: " + pharmacyId));

        pharmacy.setName(formData.getName());
        pharmacy.setGstNumber(formData.getGstNumber());
        pharmacy.setPhone(formData.getPhone());
        pharmacy.setAddress(formData.getAddress());
        pharmacy.setInvoiceFooter(formData.getInvoiceFooter());

        pharmacyRepository.save(pharmacy);
        log.info("[SETTINGS] Saved settings for pharmacyId={}", pharmacyId);
        redirectAttributes.addFlashAttribute("successMessage", "Pharmacy settings saved successfully!");
        return "redirect:/pharmacy/settings";
    }

    // ── POST /pharmacy/upload-logo ────────────────────────────────────
    @PostMapping("/upload-logo")
    public String uploadLogo(@RequestParam("logoFile") MultipartFile file,
                             HttpSession session,
                             RedirectAttributes redirectAttributes) {

        log.info("[UPLOAD-LOGO] Request received. FileName={}, Size={} bytes, ContentType={}",
                file.getOriginalFilename(), file.getSize(), file.getContentType());

        // ── Auth check ────────────────────────────────────────────────
        User loggedInUser = (User) session.getAttribute("loggedInUser");
        if (loggedInUser == null || !loggedInUser.isOwner()) {
            log.warn("[UPLOAD-LOGO] Unauthorized — no session or not owner.");
            return "redirect:/dashboard";
        }

        // ── Empty file check ──────────────────────────────────────────
        if (file.isEmpty()) {
            log.warn("[UPLOAD-LOGO] Empty file submitted.");
            redirectAttributes.addFlashAttribute("errorMessage", "Please select a file to upload.");
            return "redirect:/pharmacy/settings";
        }

        // ── File size check (5 MB) ────────────────────────────────────
        long maxBytes = 5_242_880L; // 5 MB
        if (file.getSize() > maxBytes) {
            log.warn("[UPLOAD-LOGO] File too large: {} bytes (max {})", file.getSize(), maxBytes);
            redirectAttributes.addFlashAttribute("errorMessage",
                    "File is too large (" + (file.getSize() / 1024 / 1024) + " MB). Maximum allowed size is 5 MB.");
            return "redirect:/pharmacy/settings";
        }

        // ── Extension check ───────────────────────────────────────────
        String originalFilename = file.getOriginalFilename();
        if (originalFilename == null || originalFilename.isBlank()) {
            log.warn("[UPLOAD-LOGO] Null or blank filename.");
            redirectAttributes.addFlashAttribute("errorMessage", "Invalid file name.");
            return "redirect:/pharmacy/settings";
        }

        String lowerName = originalFilename.toLowerCase();
        if (!lowerName.endsWith(".png") && !lowerName.endsWith(".jpg") && !lowerName.endsWith(".jpeg")) {
            log.warn("[UPLOAD-LOGO] Invalid extension: {}", originalFilename);
            redirectAttributes.addFlashAttribute("errorMessage",
                    "Only PNG and JPG files are allowed. Received: " + originalFilename);
            return "redirect:/pharmacy/settings";
        }

        // ── Main processing ───────────────────────────────────────────
        try {
            Long pharmacyId = tenantPharmacyService.getCurrentPharmacyId();
            log.info("[UPLOAD-LOGO] Processing for pharmacyId={}", pharmacyId);

            Pharmacy pharmacy = pharmacyRepository.findById(pharmacyId)
                    .orElseThrow(() -> new IllegalStateException("Pharmacy not found: " + pharmacyId));

            // Create directory if it doesn't exist
            Path dir = Paths.get(logoDir).toAbsolutePath();
            log.info("[UPLOAD-LOGO] Resolved upload directory: {}", dir);
            Files.createDirectories(dir);
            log.info("[UPLOAD-LOGO] Directory ready: {}", dir);

            String ext      = lowerName.substring(lowerName.lastIndexOf('.'));
            String filename = "pharmacy-" + pharmacyId + ext;
            Path   target   = dir.resolve(filename);
            log.info("[UPLOAD-LOGO] Target file path: {}", target);

            // Write to temp file for MIME validation
            Path temp = Files.createTempFile("logo-upload-", ext);
            log.info("[UPLOAD-LOGO] Temp file created: {}", temp);

            try {
                Files.copy(file.getInputStream(), temp, StandardCopyOption.REPLACE_EXISTING);
                log.info("[UPLOAD-LOGO] Bytes written to temp file: {}", Files.size(temp));

                // MIME type validation
                String mimeType = Files.probeContentType(temp);
                log.info("[UPLOAD-LOGO] Probed MIME type: {}", mimeType);

                if (mimeType == null) {
                    log.warn("[UPLOAD-LOGO] Could not determine MIME type — proceeding by extension only.");
                    // On some OSes probeContentType returns null; don't block the upload, trust the extension check above
                } else if (!mimeType.equals("image/png") && !mimeType.equals("image/jpeg")) {
                    log.warn("[UPLOAD-LOGO] MIME mismatch: expected image/png or image/jpeg, got {}", mimeType);
                    redirectAttributes.addFlashAttribute("errorMessage",
                            "Invalid file content (detected: " + mimeType + "). Only real PNG/JPG images are accepted.");
                    return "redirect:/pharmacy/settings";
                }

                // Delete old logo if different extension
                if (pharmacy.getLogoPath() != null) {
                    try {
                        Path oldPath = Paths.get(pharmacy.getLogoPath()).toAbsolutePath();
                        boolean deleted = Files.deleteIfExists(oldPath);
                        log.info("[UPLOAD-LOGO] Old logo deleted={} path={}", deleted, oldPath);
                    } catch (Exception ex) {
                        log.warn("[UPLOAD-LOGO] Could not delete old logo: {}", ex.getMessage());
                    }
                }

                // Move validated temp file to final destination
                Files.move(temp, target, StandardCopyOption.REPLACE_EXISTING);
                log.info("[UPLOAD-LOGO] File moved to final path: {}", target);

            } finally {
                // Always clean up temp file
                boolean tempDeleted = Files.deleteIfExists(temp);
                log.debug("[UPLOAD-LOGO] Temp file cleanup: deleted={}", tempDeleted);
            }

            // Store relative path (for serving via static resources)
            String relativePath = logoDir + filename;
            pharmacy.setLogoPath(relativePath);
            pharmacyRepository.save(pharmacy);
            log.info("[UPLOAD-LOGO] DB updated with logoPath={}", relativePath);

            // Update session pharmacy reference — only if already initialized (not a lazy proxy)
            // The session-stored Pharmacy may be a detached Hibernate proxy; calling setLogoPath()
            // on it without an active DB session throws LazyInitializationException.
            // Safe approach: check initialization before touching it.
            if (loggedInUser.getPharmacy() != null
                    && org.hibernate.Hibernate.isInitialized(loggedInUser.getPharmacy())) {
                loggedInUser.getPharmacy().setLogoPath(relativePath);
                log.info("[UPLOAD-LOGO] Session pharmacy reference updated.");
            } else {
                log.info("[UPLOAD-LOGO] Session pharmacy is a lazy proxy — skipping in-memory update (redirect will reload fresh data).");
            }

            // Audit log
            auditLogService.log(loggedInUser, pharmacyId,
                    "LOGO_UPLOAD", "Pharmacy", pharmacyId,
                    "Logo uploaded: " + filename);

            redirectAttributes.addFlashAttribute("successMessage", "Logo uploaded successfully! ✅");
            log.info("[UPLOAD-LOGO] Upload complete for pharmacyId={}", pharmacyId);

        } catch (MaxUploadSizeExceededException ex) {
            // Catches Spring-level size exceeded (from multipart config) — belt-and-suspenders
            log.error("[UPLOAD-LOGO] MaxUploadSizeExceededException: {}", ex.getMessage());
            redirectAttributes.addFlashAttribute("errorMessage",
                    "File is too large. Please upload a file smaller than 5 MB.");

        } catch (IllegalStateException ex) {
            log.error("[UPLOAD-LOGO] IllegalStateException: {}", ex.getMessage());
            redirectAttributes.addFlashAttribute("errorMessage",
                    "Session or pharmacy error: " + ex.getMessage());

        } catch (IOException ex) {
            log.error("[UPLOAD-LOGO] IOException during file upload", ex);
            redirectAttributes.addFlashAttribute("errorMessage",
                    "File upload failed (IO error): " + ex.getMessage()
                    + " — Check server logs for details.");

        } catch (Exception ex) {
            log.error("[UPLOAD-LOGO] Unexpected error during logo upload", ex);
            redirectAttributes.addFlashAttribute("errorMessage",
                    "Unexpected error: " + ex.getClass().getSimpleName() + " — " + ex.getMessage());
        }

        return "redirect:/pharmacy/settings";
    }

    // ── POST /pharmacy/remove-logo ────────────────────────────────────
    @PostMapping("/remove-logo")
    public String removeLogo(HttpSession session, RedirectAttributes redirectAttributes) {

        User loggedInUser = (User) session.getAttribute("loggedInUser");
        if (loggedInUser == null || !loggedInUser.isOwner()) {
            return "redirect:/dashboard";
        }

        Long pharmacyId = tenantPharmacyService.getCurrentPharmacyId();
        Pharmacy pharmacy = pharmacyRepository.findById(pharmacyId)
                .orElseThrow(() -> new IllegalStateException("Pharmacy not found."));

        if (pharmacy.getLogoPath() != null) {
            try {
                Path oldPath = Paths.get(pharmacy.getLogoPath()).toAbsolutePath();
                boolean deleted = Files.deleteIfExists(oldPath);
                log.info("[REMOVE-LOGO] Logo file deleted={} path={}", deleted, oldPath);
            } catch (Exception ex) {
                log.warn("[REMOVE-LOGO] Could not delete logo file: {}", ex.getMessage());
            }

            pharmacy.setLogoPath(null);
            pharmacyRepository.save(pharmacy);
            log.info("[REMOVE-LOGO] Logo removed from DB for pharmacyId={}", pharmacyId);

            // Same lazy proxy guard as uploadLogo
            if (loggedInUser.getPharmacy() != null
                    && org.hibernate.Hibernate.isInitialized(loggedInUser.getPharmacy())) {
                loggedInUser.getPharmacy().setLogoPath(null);
                log.info("[REMOVE-LOGO] Session pharmacy reference cleared.");
            } else {
                log.info("[REMOVE-LOGO] Session pharmacy is a lazy proxy — skipping in-memory update.");
            }

            auditLogService.log(loggedInUser, pharmacyId,
                    "LOGO_REMOVE", "Pharmacy", pharmacyId,
                    "Logo removed");

            redirectAttributes.addFlashAttribute("successMessage", "Logo removed successfully!");
        } else {
            log.info("[REMOVE-LOGO] No logo to remove for pharmacyId={}", pharmacyId);
            redirectAttributes.addFlashAttribute("errorMessage", "No logo found to remove.");
        }

        return "redirect:/pharmacy/settings";
    }
}