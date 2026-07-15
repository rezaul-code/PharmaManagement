package com.myspringboot.SpringBootApp.controller.api;

import com.myspringboot.SpringBootApp.Service.BillingService;
import com.myspringboot.SpringBootApp.Service.MedicineService;
import com.myspringboot.SpringBootApp.Service.PdfInvoiceService;
import com.myspringboot.SpringBootApp.Service.TenantPharmacyService;
import com.myspringboot.SpringBootApp.dto.BillingForm;
import com.myspringboot.SpringBootApp.dto.CreditPaymentForm;
import com.myspringboot.SpringBootApp.dto.CreditSummaryDto;
import com.myspringboot.SpringBootApp.model.Billing;
import com.myspringboot.SpringBootApp.model.Medicine;
import com.myspringboot.SpringBootApp.model.User;
import com.myspringboot.SpringBootApp.repo.BillingRepository;
import com.myspringboot.SpringBootApp.security.CustomUserDetails;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/api/billing")
public class BillingRestController {

    @Autowired private BillingService billingService;
    @Autowired private MedicineService medicineService;
    @Autowired private TenantPharmacyService tenantPharmacyService;
    @Autowired private PdfInvoiceService pdfInvoiceService;

    @Autowired private BillingRepository billingRepository;

    // Helper to get authenticated User
    private User getAuthenticatedUser() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.getPrincipal() instanceof CustomUserDetails userDetails) {
            return userDetails.getUser();
        }
        return null;
    }

    // ── GET /api/billing ─────────────────────────────────────────────────
    @GetMapping
    public ResponseEntity<?> getBills(
            @RequestParam(required = false) Integer year,
            @RequestParam(required = false) Integer month,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {

        Long pharmacyId = tenantPharmacyService.getCurrentPharmacyId();

        java.time.YearMonth ym;
        if (year != null && month != null) {
            int safeMonth = (month >= 1 && month <= 12) ? month : java.time.YearMonth.now().getMonthValue();
            int safeYear  = (year  >= 1900 && year <= 9999) ? year : java.time.YearMonth.now().getYear();
            try {
                ym = java.time.YearMonth.of(safeYear, safeMonth);
            } catch (java.time.DateTimeException e) {
                ym = java.time.YearMonth.now();
            }
        } else {
            ym = java.time.YearMonth.now();
        }

        LocalDateTime start = ym.atDay(1).atStartOfDay();
        LocalDateTime end   = ym.atEndOfMonth().atTime(23, 59, 59);

        Pageable pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());
        Page<Billing> billPage = billingRepository
                .findByPharmacyIdAndCreatedAtBetweenOrderByCreatedAtDesc(pharmacyId, start, end, pageable);

        return ResponseEntity.ok(billPage);
    }

    // ── GET /api/billing/{id} ─────────────────────────────────────────────
    @GetMapping("/{id}")
    public ResponseEntity<?> getBillById(@PathVariable Long id) {
        Optional<Billing> billing = billingService.getBillById(id);
        if (billing.isEmpty()) {
            Map<String, Object> errorBody = new HashMap<>();
            errorBody.put("success", false);
            errorBody.put("message", "Bill not found.");
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(errorBody);
        }
        return ResponseEntity.ok(billing.get());
    }

    // ── POST /api/billing ────────────────────────────────────────────────
    @PostMapping
    public ResponseEntity<?> createBill(@RequestBody @Valid BillingForm form) {
        User user = getAuthenticatedUser();
        if (user == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("error", "Not authenticated"));
        }
        try {
            Billing billing = billingService.createBill(form, user);
            return ResponseEntity.status(HttpStatus.CREATED).body(billing);
        } catch (Exception e) {
            Map<String, Object> errorBody = new HashMap<>();
            errorBody.put("success", false);
            errorBody.put("message", e.getMessage());
            return ResponseEntity.badRequest().body(errorBody);
        }
    }

    // ── GET /api/billing/credit ──────────────────────────────────────────
    @GetMapping("/credit")
    public ResponseEntity<Page<Billing>> getCreditBills(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {

        Pageable pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());
        Page<Billing> billPage = billingService.getPendingCreditBills(pageable);
        return ResponseEntity.ok(billPage);
    }

    // ── GET /api/billing/credit/summary ──────────────────────────────────
    @GetMapping("/credit/summary")
    public ResponseEntity<CreditSummaryDto> getCreditSummary() {
        return ResponseEntity.ok(billingService.getCreditSummary());
    }

    // ── POST /api/billing/credit/{id}/pay ────────────────────────────────
    @PostMapping("/credit/{id}/pay")
    public ResponseEntity<?> recordCreditPayment(
            @PathVariable Long id,
            @RequestBody @Valid CreditPaymentForm form) {
        User user = getAuthenticatedUser();
        if (user == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("error", "Not authenticated"));
        }
        try {
            Billing updatedBill = billingService.recordCreditPayment(id, form, user);
            return ResponseEntity.ok(updatedBill);
        } catch (Exception e) {
            Map<String, Object> errorBody = new HashMap<>();
            errorBody.put("success", false);
            errorBody.put("message", e.getMessage());
            return ResponseEntity.badRequest().body(errorBody);
        }
    }

    // ── GET /api/billing/today ───────────────────────────────────────────
    @GetMapping("/today")
    public ResponseEntity<Map<String, Object>> getTodaySales() {
        Long pharmacyId = tenantPharmacyService.getCurrentPharmacyId();
        LocalDate today = LocalDate.now();
        LocalDateTime start = today.atStartOfDay();
        LocalDateTime end = today.atTime(23, 59, 59);

        List<Billing> bills = billingRepository
                .findByPharmacyIdAndCreatedAtBetweenOrderByCreatedAtDesc(pharmacyId, start, end);

        BigDecimal todayTotal = bills.stream()
                .map(Billing::getGrandTotal)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal todayGst = bills.stream()
                .map(Billing::getTotalGst)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal pendingBalance = bills.stream()
                .filter(b -> b.isCreditBill() && b.getBalanceDue().compareTo(BigDecimal.ZERO) > 0)
                .map(Billing::getBalanceDue)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        long creditCount = bills.stream().filter(Billing::isCreditBill).count();

        Map<String, Object> data = new HashMap<>();
        data.put("bills", bills);
        data.put("today", today);
        data.put("todayTotal", todayTotal);
        data.put("todayGst", todayGst);
        data.put("pendingBalance", pendingBalance);
        data.put("creditCount", creditCount);

        return ResponseEntity.ok(data);
    }

    // ── GET /api/billing/medicine/search ─────────────────────────────────
    @GetMapping("/medicine/search")
    public ResponseEntity<List<Medicine>> searchMedicine(@RequestParam("q") String query) {
        List<Medicine> result = medicineService.searchByName(query);
        return ResponseEntity.ok(result);
    }

    // ── GET /api/billing/medicine/{id} ───────────────────────────────────
    @GetMapping("/medicine/{id}")
    public ResponseEntity<Medicine> getMedicineById(@PathVariable Long id) {
        return medicineService.getById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    // ── GET /api/billing/{id}/pdf ────────────────────────────────────────
    @GetMapping("/{id}/pdf")
    public ResponseEntity<byte[]> downloadInvoicePdf(@PathVariable Long id) {
        User user = getAuthenticatedUser();
        if (user == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        try {
            byte[] pdf = pdfInvoiceService.generateInvoice(id);
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_PDF);
            headers.setContentDisposition(
                    ContentDisposition.attachment()
                            .filename("invoice-" + id + ".pdf")
                            .build());
            return new ResponseEntity<>(pdf, headers, HttpStatus.OK);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
}
