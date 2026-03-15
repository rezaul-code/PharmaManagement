package com.myspringboot.SpringBootApp.controller;

import com.myspringboot.SpringBootApp.Service.AuditLogService;
import com.myspringboot.SpringBootApp.Service.ExportService;
import com.myspringboot.SpringBootApp.Service.TenantPharmacyService;
import com.myspringboot.SpringBootApp.model.User;
import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

@Controller
@RequestMapping("/export")
public class ExportController {

    @Autowired private ExportService exportService;
    @Autowired private AuditLogService auditLogService;
    @Autowired private TenantPharmacyService tenantPharmacyService;

    private static final MediaType XLSX =
        MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");

    // ── Inventory Export ─────────────────────────────────────────────────────

    @GetMapping("/inventory")
    public ResponseEntity<byte[]> exportInventory(
            @RequestParam(defaultValue = "xlsx") String format,
            HttpSession session) {

        User user = (User) session.getAttribute("loggedInUser");
        if (user == null)     return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        if (!isAuthorized(user)) return ResponseEntity.status(HttpStatus.FORBIDDEN).build();

        try {
            byte[] data    = exportService.exportInventory(format);
            String filename = "inventory." + normalise(format);

            // Audit log
            auditLogService.log(user, tenantPharmacyService.getCurrentPharmacyId(),
                    "EXPORT_INVENTORY", "Export", null,
                    "Inventory exported as " + normalise(format).toUpperCase());

            return buildResponse(data, format, filename);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    // ── Sales Export ─────────────────────────────────────────────────────────

    @GetMapping("/sales")
    public ResponseEntity<byte[]> exportSales(
            @RequestParam(defaultValue = "xlsx") String format,
            HttpSession session) {

        User user = (User) session.getAttribute("loggedInUser");
        if (user == null)     return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        if (!isAuthorized(user)) return ResponseEntity.status(HttpStatus.FORBIDDEN).build();

        try {
            byte[] data    = exportService.exportSales(format);
            String filename = "sales." + normalise(format);

            // Audit log
            auditLogService.log(user, tenantPharmacyService.getCurrentPharmacyId(),
                    "EXPORT_SALES", "Export", null,
                    "Sales exported as " + normalise(format).toUpperCase());

            return buildResponse(data, format, filename);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    // ── Helpers ──────────────────────────────────────────────────────────────

    /**
     * Only OWNER and PHARMACIST may export data.
     * STAFF users are restricted to operational screens only.
     */
    private boolean isAuthorized(User user) {
        return user.isOwner() || user.isPharmacist();
    }

    private ResponseEntity<byte[]> buildResponse(byte[] data, String format, String filename) {
        MediaType mediaType = "csv".equalsIgnoreCase(format)
                ? MediaType.parseMediaType("text/csv;charset=UTF-8")
                : XLSX;

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(mediaType);
        headers.setContentDisposition(ContentDisposition.attachment().filename(filename).build());
        return new ResponseEntity<>(data, headers, HttpStatus.OK);
    }

    private String normalise(String format) {
        return "csv".equalsIgnoreCase(format) ? "csv" : "xlsx";
    }
}
