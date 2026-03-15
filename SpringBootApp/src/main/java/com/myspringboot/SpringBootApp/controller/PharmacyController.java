package com.myspringboot.SpringBootApp.controller;

import com.myspringboot.SpringBootApp.Service.TenantContext;
import com.myspringboot.SpringBootApp.model.Pharmacy;
import com.myspringboot.SpringBootApp.model.User;
import com.myspringboot.SpringBootApp.repo.PharmacyRepository;
import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
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

    @Autowired
    private PharmacyRepository pharmacyRepository;

    private static final String LOGO_DIR = "uploads/logos/";

    // ── GET /pharmacy/settings ────────────────────────────────────────
    @GetMapping("/settings")
    public String settingsPage(HttpSession session, Model model) {

        User loggedInUser = (User) session.getAttribute("loggedInUser");
        if (loggedInUser == null || !loggedInUser.isOwner()) {
            return "redirect:/dashboard";
        }

        Long pharmacyId = TenantContext.getCurrentPharmacyId();
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

        Long pharmacyId = TenantContext.getCurrentPharmacyId();
        Pharmacy pharmacy = pharmacyRepository.findById(pharmacyId)
                .orElseThrow(() -> new IllegalStateException(
                        "Pharmacy not found for id: " + pharmacyId));

        pharmacy.setName(formData.getName());
        pharmacy.setGstNumber(formData.getGstNumber());
        pharmacy.setPhone(formData.getPhone());
        pharmacy.setAddress(formData.getAddress());
        pharmacy.setInvoiceFooter(formData.getInvoiceFooter());

        pharmacyRepository.save(pharmacy);
        redirectAttributes.addFlashAttribute("successMessage", "Pharmacy settings saved successfully!");
        return "redirect:/pharmacy/settings";
    }

    // ── POST /pharmacy/upload-logo ────────────────────────────────────

    @PostMapping("/upload-logo")
    public String uploadLogo(@RequestParam("logoFile") MultipartFile file,
                             HttpSession session,
                             RedirectAttributes redirectAttributes) {

        User loggedInUser = (User) session.getAttribute("loggedInUser");
        if (loggedInUser == null || !loggedInUser.isOwner()) {
            return "redirect:/dashboard";
        }

        if (file.isEmpty()) {
            redirectAttributes.addFlashAttribute("errorMessage", "Please select a file to upload.");
            return "redirect:/pharmacy/settings";
        }

        // 1. File size limit: 5 MB
        if (file.getSize() > 5_242_880L) { // 5 * 1024 * 1024
            redirectAttributes.addFlashAttribute("errorMessage",
                    "File is too large. Maximum allowed size is 5 MB.");
            return "redirect:/pharmacy/settings";
        }

        String originalFilename = file.getOriginalFilename();
        if (originalFilename == null) {
            redirectAttributes.addFlashAttribute("errorMessage", "Invalid file name.");
            return "redirect:/pharmacy/settings";
        }

        // 2. Case-insensitive extension check
        String lowerName = originalFilename.toLowerCase();
        if (!lowerName.endsWith(".png") && !lowerName.endsWith(".jpg") && !lowerName.endsWith(".jpeg")) {
            redirectAttributes.addFlashAttribute("errorMessage", "Only PNG and JPG files are allowed.");
            return "redirect:/pharmacy/settings";
        }

        try {
            // 3. MIME type validation — write to temp file, probe, then copy to final location
            Long pharmacyId = TenantContext.getCurrentPharmacyId();
            Path dir = Paths.get(LOGO_DIR);
            Files.createDirectories(dir);

            String ext      = lowerName.substring(lowerName.lastIndexOf('.'));
            String filename = "pharmacy-" + pharmacyId + ext;
            Path   target   = dir.resolve(filename);

            // Write bytes to a temp file first so probeContentType works on all OSes
            Path temp = Files.createTempFile("logo-upload-", ext);
            try {
                Files.copy(file.getInputStream(), temp, StandardCopyOption.REPLACE_EXISTING);
                String mimeType = Files.probeContentType(temp);
                if (mimeType == null || (!mimeType.equals("image/png") && !mimeType.equals("image/jpeg"))) {
                    redirectAttributes.addFlashAttribute("errorMessage",
                            "Invalid file type. Only real PNG and JPG images are accepted.");
                    return "redirect:/pharmacy/settings";
                }
                // MIME validated — move to final destination
                Files.move(temp, target, StandardCopyOption.REPLACE_EXISTING);
            } finally {
                Files.deleteIfExists(temp);
            }

            Pharmacy pharmacy = pharmacyRepository.findById(pharmacyId)
                    .orElseThrow(() -> new IllegalStateException("Pharmacy not found."));
            pharmacy.setLogoPath(LOGO_DIR + filename);
            pharmacyRepository.save(pharmacy);

            if (loggedInUser.getPharmacy() != null) {
                loggedInUser.getPharmacy().setLogoPath(LOGO_DIR + filename);
            }

            redirectAttributes.addFlashAttribute("successMessage", "Logo uploaded successfully!");

        } catch (IOException e) {
            redirectAttributes.addFlashAttribute("errorMessage", "Failed to upload logo: " + e.getMessage());
        }

        return "redirect:/pharmacy/settings";
    }
}