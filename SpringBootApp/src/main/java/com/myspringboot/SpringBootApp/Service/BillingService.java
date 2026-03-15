// src/main/java/com/myspringboot/SpringBootApp/Service/BillingService.java
package com.myspringboot.SpringBootApp.Service;

import com.myspringboot.SpringBootApp.dto.BillingForm;
import com.myspringboot.SpringBootApp.dto.CreditPaymentForm;
import com.myspringboot.SpringBootApp.dto.CreditSummaryDto;
import com.myspringboot.SpringBootApp.model.*;
import com.myspringboot.SpringBootApp.repo.BillingRepository;
import com.myspringboot.SpringBootApp.repo.CreditPaymentRepository;
import com.myspringboot.SpringBootApp.repo.MedicineRepository;
import com.myspringboot.SpringBootApp.repo.PharmacyBillCounterRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Optional;

@Service
public class BillingService {

    @Autowired private BillingRepository            billingRepository;
    @Autowired private CreditPaymentRepository      creditPaymentRepository;
    @Autowired private MedicineRepository           medicineRepository;
    @Autowired private TenantPharmacyService        tenantPharmacyService;
    @Autowired private AuditLogService              auditLogService;
    @Autowired private PharmacyBillCounterRepository billCounterRepository;

    // ── Create bill ────────────────────────────────────────────────────

    @Transactional
    public Billing createBill(BillingForm form, User createdBy) {
        if (form == null) throw new IllegalArgumentException("Billing form is required.");

        Long     pharmacyId = tenantPharmacyService.getCurrentPharmacyId();
        Pharmacy pharmacy   = tenantPharmacyService.getCurrentPharmacy();

        Billing billing = new Billing();
        billing.setBillNumber(generateBillNumber(pharmacyId));
        billing.setPatientName(form.getPatientName());
        billing.setPatientPhone(form.getPatientPhone());
        billing.setNotes(form.getNotes());
        billing.setCreatedAt(LocalDateTime.now());
        billing.setCreatedBy(createdBy);
        billing.setPharmacy(pharmacy);

        // Validate credit requires a customer name
        if (form.getPaymentType() == PaymentType.CREDIT
                && (form.getPatientName() == null || form.getPatientName().isBlank())) {
            throw new IllegalArgumentException(
                    "Customer name is required for credit billing.");
        }

        // Build items
        if (form.getItems() != null) {
            for (BillingItemForm itemForm : form.getItems()) {
                if (itemForm == null) continue;

                boolean hasId    = itemForm.getMedicineId() != null;
                boolean hasName  = itemForm.getMedicineName() != null && !itemForm.getMedicineName().isBlank();
                boolean hasQty   = itemForm.getQuantity()  != null && itemForm.getQuantity()  > 0;
                boolean hasPrice = itemForm.getUnitPrice() != null
                        && itemForm.getUnitPrice().compareTo(BigDecimal.ZERO) > 0;

                if ((!hasId && !hasName) || !hasQty || !hasPrice) continue;

                BillingItem item = new BillingItem();

                if (hasId) {
                    // ── TASK 2: Pessimistic lock prevents concurrent oversell ─────
                    Optional<Medicine> medOpt =
                            medicineRepository.findByIdAndPharmacyIdForUpdate(
                                    itemForm.getMedicineId(), pharmacyId);
                    if (medOpt.isPresent()) {
                        Medicine med = medOpt.get();
                        item.setMedicine(med);
                        item.setMedicineName(med.getName());
                        item.setBatchNo(
                                itemForm.getBatchNo() != null && !itemForm.getBatchNo().isBlank()
                                        ? itemForm.getBatchNo() : med.getBatchNo());

                        int currentStock = med.getStockQuantity() != null ? med.getStockQuantity() : 0;
                        int requestedQty = itemForm.getQuantity();

                        if (currentStock < requestedQty) {
                            throw new RuntimeException("Insufficient stock available for: " + med.getName()
                                    + ". Available: " + currentStock + ", requested: " + requestedQty);
                        }

                        med.setStockQuantity(currentStock - requestedQty);
                        medicineRepository.save(med);
                    }
                }

                if (item.getMedicineName() == null) item.setMedicineName(itemForm.getMedicineName());
                if (item.getBatchNo()       == null && itemForm.getBatchNo() != null)
                    item.setBatchNo(itemForm.getBatchNo());

                item.setQuantity(itemForm.getQuantity());
                item.setUnitPrice(itemForm.getUnitPrice());
                item.setGstPercentage(
                        itemForm.getGstPercentage() != null ? itemForm.getGstPercentage() : BigDecimal.ZERO);
                item.setPharmacy(pharmacy);
                item.calculateTotals();
                billing.addItem(item);
            }
        }

        billing.recalculateTotals();

        // Apply payment type — sets status, paidAmount, balanceDue
        PaymentType paymentType = form.getPaymentType() != null ? form.getPaymentType() : PaymentType.CASH;
        billing.applyPaymentType(paymentType, form.getInitialPayment());

        Billing saved = billingRepository.save(billing);
        auditLogService.log(createdBy, "BILL_CREATED", "Billing",
                saved.getId(), "Bill " + saved.getBillNumber() + " for " +
                (saved.getPatientName() != null ? saved.getPatientName() : "Walk-in"));
        return saved;
    }

    // ── Record credit payment ──────────────────────────────────────────

    @Transactional
    public Billing recordCreditPayment(Long billingId, CreditPaymentForm form, User recordedBy) {
        Long pharmacyId = tenantPharmacyService.getCurrentPharmacyId();

        Billing billing = billingRepository.findByIdAndPharmacyId(billingId, pharmacyId)
                .orElseThrow(() -> new IllegalArgumentException("Bill not found."));

        if (billing.getPaymentType() != PaymentType.CREDIT) {
            throw new IllegalArgumentException("This is not a credit bill.");
        }
        if (billing.getBalanceDue().compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("This credit bill is already fully cleared.");
        }

        // Cap payment at remaining balance
        BigDecimal toCollect = form.getAmountPaid().min(billing.getBalanceDue());

        CreditPayment cp = new CreditPayment();
        cp.setAmountPaid(toCollect);
        cp.setPaymentMode(form.getPaymentMode() != null ? form.getPaymentMode() : PaymentType.CASH);
        cp.setNotes(form.getNotes());
        cp.setPaidAt(LocalDateTime.now());
        cp.setPharmacy(tenantPharmacyService.getCurrentPharmacy());
        cp.setRecordedBy(recordedBy);

        billing.recordCreditPayment(cp);
        Billing saved = billingRepository.save(billing);
        auditLogService.log(recordedBy, "CREDIT_PAYMENT_COLLECTED", "Billing",
                saved.getId(), "₹" + toCollect + " collected. Balance due: ₹" + saved.getBalanceDue());
        return saved;
    }

    // ── Read methods ───────────────────────────────────────────────────

    public org.springframework.data.domain.Page<Billing> getAllBills(int page, int size) {
        org.springframework.data.domain.Pageable pageable = org.springframework.data.domain.PageRequest.of(page, size, org.springframework.data.domain.Sort.by("createdAt").descending());
        return billingRepository.findByPharmacyIdOrderByCreatedAtDesc(
                tenantPharmacyService.getCurrentPharmacyId(), pageable);
    }

    @Transactional(readOnly = true)
    public Optional<Billing> getBillById(Long id) {
        Long pharmacyId = tenantPharmacyService.getCurrentPharmacyId();
        Optional<Billing> billingOpt = billingRepository.findByIdWithDetails(id, pharmacyId);
        
        if (billingOpt.isPresent()) {
            Billing billing = billingOpt.get();
            // Manually trigger loading of lazy collections while session is open
            billing.getItems().size(); 
            billing.getCreditPayments().size();
        }
        
        return billingOpt;
    }

    public List<Billing> getCreditBills() {
        Long pharmacyId = tenantPharmacyService.getCurrentPharmacyId();
        return billingRepository.findByPharmacyIdAndStatusInOrderByCreatedAtDesc(
                pharmacyId,
                List.of(Billing.BillingStatus.CREDIT_PENDING,
                        Billing.BillingStatus.CREDIT_PARTIAL,
                        Billing.BillingStatus.CREDIT_CLEARED));
    }

    public List<Billing> getPendingCreditBills() {
        Long pharmacyId = tenantPharmacyService.getCurrentPharmacyId();
        return billingRepository.findByPharmacyIdAndStatusInOrderByCreatedAtDesc(
                pharmacyId,
                List.of(Billing.BillingStatus.CREDIT_PENDING,
                        Billing.BillingStatus.CREDIT_PARTIAL));
    }

    public CreditSummaryDto getCreditSummary() {
        Long pharmacyId = tenantPharmacyService.getCurrentPharmacyId();

        BigDecimal issued   = orZero(billingRepository.sumCreditAmountByPharmacyId(pharmacyId));
        BigDecimal recovered = orZero(billingRepository.sumPaidAmountByPharmacyId(pharmacyId));
        BigDecimal pending  = orZero(billingRepository.sumBalanceDueByPharmacyId(pharmacyId));
        long total          = billingRepository.countCreditBillsByPharmacyId(pharmacyId);
        long pendingCount   = billingRepository.countPendingCreditBillsByPharmacyId(pharmacyId);

        return new CreditSummaryDto(issued, recovered, pending, total, pendingCount);
    }

    public long getTotalBillCount() {
        return billingRepository.countByPharmacyId(tenantPharmacyService.getCurrentPharmacyId());
    }

    public BigDecimal getTodaySales() {
        Long pharmacyId = tenantPharmacyService.getCurrentPharmacyId();
        LocalDateTime start = LocalDate.now().atStartOfDay();
        BigDecimal total = billingRepository.sumGrandTotalBetween(pharmacyId, start, start.plusDays(1));
        return total != null ? total : BigDecimal.ZERO;
    }

    // ── Helpers ────────────────────────────────────────────────────────

    /**
     * TASK 3: Generates a collision-free bill number using a per-pharmacy
     * atomic counter row with a PESSIMISTIC_WRITE lock.
     *
     * The counter row is created lazily on first use. Under concurrent
     * transactions, each caller blocks until the previous one commits,
     * guaranteeing a unique sequence number.
     */
    private String generateBillNumber(Long pharmacyId) {
        String date = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"));

        PharmacyBillCounter counter = billCounterRepository
                .findByPharmacyIdForUpdate(pharmacyId)
                .orElseGet(() -> {
                    PharmacyBillCounter c = new PharmacyBillCounter(pharmacyId);
                    return billCounterRepository.save(c);
                });

        long seq = counter.nextSeq();
        billCounterRepository.save(counter);

        return String.format("BILL-%s-%04d", date, seq);
    }

    private BigDecimal orZero(BigDecimal v) { return v != null ? v : BigDecimal.ZERO; }
}