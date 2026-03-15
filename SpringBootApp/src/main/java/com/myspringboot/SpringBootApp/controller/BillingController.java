// src/main/java/com/myspringboot/SpringBootApp/controller/BillingController.java
package com.myspringboot.SpringBootApp.controller;

import com.myspringboot.SpringBootApp.Service.BillingService;
import com.myspringboot.SpringBootApp.Service.MedicineService;
import com.myspringboot.SpringBootApp.Service.TenantPharmacyService;
import com.myspringboot.SpringBootApp.dto.BillingForm;
import com.myspringboot.SpringBootApp.dto.CreditPaymentForm;
import com.myspringboot.SpringBootApp.model.Billing;
import com.myspringboot.SpringBootApp.model.Medicine;
import com.myspringboot.SpringBootApp.model.PaymentType;
import com.myspringboot.SpringBootApp.model.User;
import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import com.myspringboot.SpringBootApp.repo.BillingRepository;
import com.myspringboot.SpringBootApp.Service.PdfInvoiceService;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Controller
@RequestMapping("/billing")
public class BillingController {

    @Autowired private BillingService        billingService;
    @Autowired private MedicineService       medicineService;
    @Autowired private TenantPharmacyService tenantPharmacyService;
    @Autowired private BillingRepository     billingRepository;
    @Autowired private PdfInvoiceService     pdfInvoiceService;

    // ── Guard ──────────────────────────────────────────────────────────

    private User requireLoggedIn(HttpSession session, RedirectAttributes ra) {
        User user = (User) session.getAttribute("loggedInUser");
        if (user == null && ra != null)
            ra.addFlashAttribute("error", "Please log in to access billing.");
        return user;
    }

    // ── New bill ───────────────────────────────────────────────────────

    @GetMapping("/new")
    public String newBillForm(HttpSession session, Model model, RedirectAttributes ra) {
        User user = requireLoggedIn(session, ra);
        if (user == null) return "redirect:/login";

        model.addAttribute("billingForm",  new BillingForm());
        model.addAttribute("paymentTypes", PaymentType.values());
        model.addAttribute("currentUser",  user);
        return "pages/billing_new";
    }

    // ── Create bill ────────────────────────────────────────────────────

    @PostMapping("/create")
    public String createBill(
            @Valid @ModelAttribute("billingForm") BillingForm form,
            BindingResult bindingResult,
            HttpSession session,
            Model model,
            RedirectAttributes ra) {

        User user = requireLoggedIn(session, ra);
        if (user == null) return "redirect:/login";

        if (bindingResult.hasErrors()) {
            model.addAttribute("error", "Please correct the highlighted fields.");
            model.addAttribute("billingForm",  form);
            model.addAttribute("paymentTypes", PaymentType.values());
            model.addAttribute("currentUser",  user);
            return "pages/billing_new";
        }

        try {
            Billing billing = billingService.createBill(form, user);
            ra.addFlashAttribute("success", "Bill " + billing.getBillNumber() + " created successfully.");
            return "redirect:/billing/view/" + billing.getId();
        } catch (Exception e) {
            model.addAttribute("error", "Failed to create bill: " + e.getMessage());
            model.addAttribute("billingForm",  form);
            model.addAttribute("paymentTypes", PaymentType.values());
            model.addAttribute("currentUser",  user);
            return "pages/billing_new";
        }
    }

    // ── View single bill ───────────────────────────────────────────────

    @GetMapping("/view/{id}")
    public String viewBill(@PathVariable Long id,
                           HttpSession session,
                           Model model,
                           RedirectAttributes ra) {
        User user = requireLoggedIn(session, ra);
        if (user == null) return "redirect:/login";

        Optional<Billing> billing = billingService.getBillById(id);
        if (billing.isEmpty()) return "redirect:/billing/list";

        model.addAttribute("billing",            billing.get());
        model.addAttribute("currentUser",        user);
        model.addAttribute("pharmacy",           tenantPharmacyService.getCurrentPharmacy());
        model.addAttribute("creditPaymentForm",  new CreditPaymentForm());
        model.addAttribute("paymentTypes",       PaymentType.values());
        return "pages/billing_view";
    }

    // ── List all bills ─────────────────────────────────────────────────

    @GetMapping("/list")
    public String listBills(
            @RequestParam(required = false) Integer year,
            @RequestParam(required = false) Integer month,
            @RequestParam(defaultValue = "0")  int page,
            @RequestParam(defaultValue = "10") int size,
            HttpSession session, Model model, RedirectAttributes ra) {

        User user = requireLoggedIn(session, ra);
        if (user == null) return "redirect:/login";

        // Default to current month if no filter supplied, or if inputs are invalid
        java.time.YearMonth ym;
        if (year != null && month != null) {
            // Validate month range before calling YearMonth.of() — prevents DateTimeException HTTP 500
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

        org.springframework.data.domain.Pageable pageable =
                org.springframework.data.domain.PageRequest.of(
                        page, size,
                        org.springframework.data.domain.Sort.by("createdAt").descending());

        Long pharmacyId = tenantPharmacyService.getCurrentPharmacyId();
        org.springframework.data.domain.Page<Billing> billPage =
                billingRepository.findByPharmacyIdAndCreatedAtBetweenOrderByCreatedAtDesc(
                        pharmacyId, start, end, pageable);

        // Build year list: from 2020 up to current year
        int currentYear  = java.time.LocalDate.now().getYear();
        java.util.List<Integer> years  = java.util.stream.IntStream
                .rangeClosed(2020, currentYear)
                .boxed()
                .sorted(java.util.Comparator.reverseOrder())
                .collect(java.util.stream.Collectors.toList());

        model.addAttribute("billPage",    billPage);
        model.addAttribute("bills",       billPage.getContent());
        model.addAttribute("currentPage", page);
        model.addAttribute("pageSize",    size);
        model.addAttribute("selectedYear",  ym.getYear());
        model.addAttribute("selectedMonth", ym.getMonthValue());
        model.addAttribute("years",       years);
        model.addAttribute("currentUser", user);
        return "pages/billing_list";
    }

    // ── Credit bills list ──────────────────────────────────────────────

    @GetMapping("/credit")
    public String creditBills(
            @RequestParam(defaultValue = "0")  int page,
            @RequestParam(defaultValue = "20") int size,
            HttpSession session, Model model, RedirectAttributes ra) {
        User user = requireLoggedIn(session, ra);
        if (user == null) return "redirect:/login";

        org.springframework.data.domain.Pageable pageable =
                org.springframework.data.domain.PageRequest.of(page, size,
                        org.springframework.data.domain.Sort.by("createdAt").descending());

        org.springframework.data.domain.Page<Billing> billPage =
                billingService.getPendingCreditBills(pageable);

        model.addAttribute("billPage",      billPage);
        model.addAttribute("bills",         billPage.getContent());
        model.addAttribute("currentPage",   page);
        model.addAttribute("pageSize",      size);
        model.addAttribute("creditSummary", billingService.getCreditSummary());
        model.addAttribute("currentUser",   user);
        return "pages/billing_credit";
    }

    // ── Record credit payment (POST) ───────────────────────────────────

    @PostMapping("/credit/{id}/pay")
    public String recordCreditPayment(
            @PathVariable Long id,
            @Valid @ModelAttribute("creditPaymentForm") CreditPaymentForm form,
            BindingResult bindingResult,
            HttpSession session,
            RedirectAttributes ra) {

        User user = requireLoggedIn(session, ra);
        if (user == null) return "redirect:/login";

        if (bindingResult.hasErrors()) {
            ra.addFlashAttribute("error", "Invalid payment amount.");
            return "redirect:/billing/view/" + id;
        }

        try {
            billingService.recordCreditPayment(id, form, user);
            ra.addFlashAttribute("success", "Payment recorded successfully.");
        } catch (Exception e) {
            ra.addFlashAttribute("error", "Payment failed: " + e.getMessage());
        }
        return "redirect:/billing/view/" + id;
    }

    // ── AJAX: medicine search ──────────────────────────────────────────

    @GetMapping("/medicine/search")
    @ResponseBody
    public List<Medicine> searchMedicine(@RequestParam("q") String query) {
        return medicineService.searchByName(query);
    }

    @GetMapping("/medicine/{id}")
    @ResponseBody
    public ResponseEntity<Medicine> getMedicineById(@PathVariable Long id) {
        return medicineService.getById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }
    
 // ── Today's Sales Page ───────────────────────────────────────────────
    @GetMapping("/today")
    public String todaySalesPage(HttpSession session, Model model, RedirectAttributes ra) {
        User user = requireLoggedIn(session, ra);
        if (user == null) return "redirect:/login";
     
        Long pharmacyId = tenantPharmacyService.getCurrentPharmacyId();
        LocalDate today      = LocalDate.now();
        LocalDateTime start  = today.atStartOfDay();
        LocalDateTime end    = today.atTime(23, 59, 59);
     
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
     
        model.addAttribute("bills",          bills);
        model.addAttribute("today",          today);
        model.addAttribute("todayTotal",     todayTotal);
        model.addAttribute("todayGst",       todayGst);
        model.addAttribute("pendingBalance", pendingBalance);
        model.addAttribute("creditCount",    creditCount);
        model.addAttribute("currentUser",    user);
        return "pages/today_sales";
    }

    // ── PDF Invoice Download ───────────────────────────────────────────

    @GetMapping("/{id}/pdf")
    public ResponseEntity<byte[]> downloadInvoicePdf(
            @PathVariable Long id,
            HttpSession session) {

        User user = (User) session.getAttribute("loggedInUser");
        if (user == null) return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();

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



