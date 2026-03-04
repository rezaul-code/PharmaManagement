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

    // ─── Create Bill ─────────────────────────────────────────────────

    @Transactional
    public Billing createBill(BillingForm form, User createdBy) {
        Billing billing = new Billing();
        billing.setBillNumber(generateBillNumber());
        billing.setPatientName(form.getPatientName());
        billing.setPatientPhone(form.getPatientPhone());
        billing.setCreatedAt(LocalDateTime.now());
        billing.setCreatedBy(createdBy);
        billing.setStatus(Billing.BillingStatus.PAID);

        for (BillingItemForm itemForm : form.getItems()) {
            if (itemForm.getMedicineId() == null || itemForm.getQuantity() == null) continue;

            BillingItem item = new BillingItem();

            // Fetch medicine
            Optional<Medicine> medOpt = medicineRepository.findById(itemForm.getMedicineId());
            medOpt.ifPresent(med -> {
                item.setMedicine(med);
                item.setMedicineName(med.getName());
                item.setBatchNo(med.getBatchNo());

                // Deduct stock
                int newStock = med.getStockQuantity() - itemForm.getQuantity();
                med.setStockQuantity(Math.max(newStock, 0));
                medicineRepository.save(med);
            });

            item.setQuantity(itemForm.getQuantity());
            item.setUnitPrice(itemForm.getUnitPrice());
            item.setGstPercentage(
                itemForm.getGstPercentage() != null
                    ? itemForm.getGstPercentage()
                    : BigDecimal.ZERO
            );

            // Calculate line-item totals
            item.calculateTotals();

            billing.addItem(item);
        }

        // Recalculate bill-level totals from items (authoritative server-side calc)
        billing.recalculateTotals();

        return billingRepository.save(billing);
    }

    // ─── Read ────────────────────────────────────────────────────────

    public List<Billing> getAllBills() {
        return billingRepository.findAllByOrderByCreatedAtDesc();
    }

    public Optional<Billing> getBillById(Long id) {
        return billingRepository.findById(id);
    }

    // ─── Dashboard Stats ─────────────────────────────────────────────

    public long getTotalBillCount() {
        return billingRepository.count();
    }

    public BigDecimal getTodaySales() {
        LocalDateTime startOfDay = LocalDate.now().atStartOfDay();
        LocalDateTime endOfDay   = startOfDay.plusDays(1);
        BigDecimal total = billingRepository.sumGrandTotalBetween(startOfDay, endOfDay);
        return total != null ? total : BigDecimal.ZERO;
    }

    // ─── Helpers ─────────────────────────────────────────────────────

    private String generateBillNumber() {
        String date   = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"));
        long   count  = billingRepository.count() + 1;
        return String.format("BILL-%s-%04d", date, count);
    }
}