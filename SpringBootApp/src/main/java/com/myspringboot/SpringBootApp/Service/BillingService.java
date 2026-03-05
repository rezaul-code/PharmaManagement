package com.myspringboot.SpringBootApp.Service;

import com.myspringboot.SpringBootApp.dto.BillingForm;
import com.myspringboot.SpringBootApp.model.*;
import com.myspringboot.SpringBootApp.repo.BillingRepository;
import com.myspringboot.SpringBootApp.repo.MedicineRepository;
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

    @Autowired
    private BillingRepository billingRepository;

    @Autowired
    private MedicineRepository medicineRepository;

    // ─── Create Bill ──────────────────────────────────────────────────

    @Transactional
    public Billing createBill(BillingForm form, User createdBy) {

        Billing billing = new Billing();
        billing.setBillNumber(generateBillNumber());
        billing.setPatientName(form.getPatientName());
        billing.setPatientPhone(form.getPatientPhone());
        billing.setCreatedAt(LocalDateTime.now());
        billing.setCreatedBy(createdBy);
        billing.setStatus(Billing.BillingStatus.PAID);

        if (form.getItems() != null) {
            for (BillingItemForm itemForm : form.getItems()) {

                // BUG FIX: Original code skipped rows where medicineId == null.
                // Now we skip only truly empty rows — where BOTH medicineId
                // AND medicineName are absent AND quantity is null/zero.
                boolean hasId   = itemForm.getMedicineId() != null;
                boolean hasName = itemForm.getMedicineName() != null
                                  && !itemForm.getMedicineName().isBlank();
                boolean hasQty  = itemForm.getQuantity() != null
                                  && itemForm.getQuantity() > 0;
                boolean hasPrice = itemForm.getUnitPrice() != null
                                   && itemForm.getUnitPrice().compareTo(BigDecimal.ZERO) > 0;

                // Skip the row if there's nothing meaningful in it
                if ((!hasId && !hasName) || !hasQty || !hasPrice) {
                    continue;
                }

                BillingItem item = new BillingItem();

                // ── Populate from Medicine record (if ID given) ───────
                if (hasId) {
                    Optional<Medicine> medOpt = medicineRepository.findById(itemForm.getMedicineId());
                    if (medOpt.isPresent()) {
                        Medicine med = medOpt.get();
                        item.setMedicine(med);

                        // Snapshot name and batch at time of billing
                        item.setMedicineName(med.getName());
                        item.setBatchNo(
                            itemForm.getBatchNo() != null && !itemForm.getBatchNo().isBlank()
                                ? itemForm.getBatchNo()
                                : med.getBatchNo()
                        );

                        // Deduct stock
                        int newStock = Math.max(0, med.getStockQuantity() - itemForm.getQuantity());
                        med.setStockQuantity(newStock);
                        medicineRepository.save(med);
                    }
                }

                // ── Fallback: use whatever name was typed ─────────────
                if (item.getMedicineName() == null) {
                    item.setMedicineName(itemForm.getMedicineName());
                }
                if (item.getBatchNo() == null && itemForm.getBatchNo() != null) {
                    item.setBatchNo(itemForm.getBatchNo());
                }

                // ── Set numeric fields ────────────────────────────────
                item.setQuantity(itemForm.getQuantity());
                item.setUnitPrice(itemForm.getUnitPrice());
                item.setGstPercentage(
                    itemForm.getGstPercentage() != null
                        ? itemForm.getGstPercentage()
                        : BigDecimal.ZERO
                );

                // ── Calculate line totals server-side ─────────────────
                item.calculateTotals();

                billing.addItem(item);
            }
        }

        // Recalculate bill-level totals from items (server-side, authoritative)
        billing.recalculateTotals();

        return billingRepository.save(billing);
    }

    // ─── Read ─────────────────────────────────────────────────────────

    public List<Billing> getAllBills() {
        return billingRepository.findAllByOrderByCreatedAtDesc();
    }

    public Optional<Billing> getBillById(Long id) {
        return billingRepository.findById(id);
    }

    // ─── Dashboard Stats ──────────────────────────────────────────────

    public long getTotalBillCount() {
        return billingRepository.count();
    }

    public BigDecimal getTodaySales() {
        LocalDateTime startOfDay = LocalDate.now().atStartOfDay();
        LocalDateTime endOfDay   = startOfDay.plusDays(1);
        BigDecimal total = billingRepository.sumGrandTotalBetween(startOfDay, endOfDay);
        return total != null ? total : BigDecimal.ZERO;
    }

    // ─── Helpers ──────────────────────────────────────────────────────

    private String generateBillNumber() {
        String date  = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"));
        long   count = billingRepository.count() + 1;
        return String.format("BILL-%s-%04d", date, count);
    }
}