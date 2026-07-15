package com.myspringboot.SpringBootApp.controller.api;

import com.myspringboot.SpringBootApp.Service.ExportService;
import com.myspringboot.SpringBootApp.Service.TenantPharmacyService;
import com.myspringboot.SpringBootApp.model.Billing;
import com.myspringboot.SpringBootApp.model.Medicine;
import com.myspringboot.SpringBootApp.model.User;
import com.myspringboot.SpringBootApp.repo.BillingRepository;
import com.myspringboot.SpringBootApp.repo.MedicineRepository;
import com.myspringboot.SpringBootApp.security.CustomUserDetails;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.*;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/reports")
public class ReportsRestController {

    @Autowired private ExportService exportService;
    @Autowired private TenantPharmacyService tenantPharmacyService;
    @Autowired private MedicineRepository medicineRepository;
    @Autowired private BillingRepository billingRepository;

    private static final MediaType XLSX =
            MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");

    private boolean isAuthorized() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.getPrincipal() instanceof CustomUserDetails userDetails) {
            User user = userDetails.getUser();
            return user.isOwner() || user.isPharmacist();
        }
        return false;
    }

    // ── GET /api/reports/inventory ────────────────────────────────────────
    @GetMapping("/inventory")
    public ResponseEntity<?> getInventoryReport(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "50") int size) {
        if (!isAuthorized()) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(Map.of("error", "Access denied. Only owners or pharmacists can access reports."));
        }
        Long pharmacyId = tenantPharmacyService.getCurrentPharmacyId();
        Pageable pageable = PageRequest.of(page, size, Sort.by("name"));
        Page<Medicine> slice = medicineRepository.findByPharmacyId(pharmacyId, pageable);
        return ResponseEntity.ok(slice);
    }

    // ── GET /api/reports/sales ────────────────────────────────────────────
    @GetMapping("/sales")
    public ResponseEntity<?> getSalesReport(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "50") int size) {
        if (!isAuthorized()) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(Map.of("error", "Access denied. Only owners or pharmacists can access reports."));
        }
        Long pharmacyId = tenantPharmacyService.getCurrentPharmacyId();
        Pageable pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());
        Page<Billing> slice = billingRepository.findByPharmacyIdOrderByCreatedAtDesc(pharmacyId, pageable);
        return ResponseEntity.ok(slice);
    }

    // ── GET /api/reports/export/inventory ─────────────────────────────────
    @GetMapping("/export/inventory")
    public ResponseEntity<byte[]> exportInventory(@RequestParam(defaultValue = "xlsx") String format) {
        if (!isAuthorized()) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }
        try {
            byte[] data = exportService.exportInventory(format);
            String filename = "inventory." + ("csv".equalsIgnoreCase(format) ? "csv" : "xlsx");
            return buildFileResponse(data, format, filename);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    // ── GET /api/reports/export/sales ─────────────────────────────────────
    @GetMapping("/export/sales")
    public ResponseEntity<byte[]> exportSales(@RequestParam(defaultValue = "xlsx") String format) {
        if (!isAuthorized()) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }
        try {
            byte[] data = exportService.exportSales(format);
            String filename = "sales." + ("csv".equalsIgnoreCase(format) ? "csv" : "xlsx");
            return buildFileResponse(data, format, filename);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    private ResponseEntity<byte[]> buildFileResponse(byte[] data, String format, String filename) {
        MediaType mediaType = "csv".equalsIgnoreCase(format)
                ? MediaType.parseMediaType("text/csv;charset=UTF-8")
                : XLSX;

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(mediaType);
        headers.setContentDisposition(ContentDisposition.attachment().filename(filename).build());
        return new ResponseEntity<>(data, headers, HttpStatus.OK);
    }
}
