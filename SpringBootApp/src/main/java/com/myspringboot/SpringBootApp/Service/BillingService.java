package com.myspringboot.SpringBootApp.Service;

import com.myspringboot.SpringBootApp.dto.BillingForm;
import com.myspringboot.SpringBootApp.model.Billing;
import com.myspringboot.SpringBootApp.model.BillingItem;
import com.myspringboot.SpringBootApp.model.BillingItemForm;
import com.myspringboot.SpringBootApp.model.Medicine;
import com.myspringboot.SpringBootApp.model.Pharmacy;
import com.myspringboot.SpringBootApp.model.User;
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

    @Autowired
    private TenantPharmacyService tenantPharmacyService;

    @Transactional
    public Billing createBill(BillingForm form, User createdBy) {
        if (form == null) {
            throw new IllegalArgumentException("Billing form is required.");
        }

        Long pharmacyId = tenantPharmacyService.getCurrentPharmacyId();
        Pharmacy pharmacy = tenantPharmacyService.getCurrentPharmacy();

        Billing billing = new Billing();
        billing.setBillNumber(generateBillNumber(pharmacyId));
        billing.setPatientName(form.getPatientName());
        billing.setPatientPhone(form.getPatientPhone());
        billing.setCreatedAt(LocalDateTime.now());
        billing.setCreatedBy(createdBy);
        billing.setPharmacy(pharmacy);
        billing.setStatus(Billing.BillingStatus.PAID);

        if (form.getItems() != null) {
            for (BillingItemForm itemForm : form.getItems()) {
                if (itemForm == null) {
                    continue;
                }

                boolean hasId = itemForm.getMedicineId() != null;
                boolean hasName = itemForm.getMedicineName() != null
                        && !itemForm.getMedicineName().isBlank();
                boolean hasQty = itemForm.getQuantity() != null
                        && itemForm.getQuantity() > 0;
                boolean hasPrice = itemForm.getUnitPrice() != null
                        && itemForm.getUnitPrice().compareTo(BigDecimal.ZERO) > 0;

                if ((!hasId && !hasName) || !hasQty || !hasPrice) {
                    continue;
                }

                BillingItem item = new BillingItem();

                if (hasId) {
                    Optional<Medicine> medOpt = medicineRepository.findByIdAndPharmacyId(itemForm.getMedicineId(), pharmacyId);
                    if (medOpt.isPresent()) {
                        Medicine med = medOpt.get();
                        item.setMedicine(med);
                        item.setMedicineName(med.getName());
                        item.setBatchNo(
                                itemForm.getBatchNo() != null && !itemForm.getBatchNo().isBlank()
                                        ? itemForm.getBatchNo()
                                        : med.getBatchNo()
                        );

                        int currentStock = med.getStockQuantity() != null ? med.getStockQuantity() : 0;
                        int newStock = Math.max(0, currentStock - itemForm.getQuantity());
                        med.setStockQuantity(newStock);
                        medicineRepository.save(med);
                    }
                }

                if (item.getMedicineName() == null) {
                    item.setMedicineName(itemForm.getMedicineName());
                }
                if (item.getBatchNo() == null && itemForm.getBatchNo() != null) {
                    item.setBatchNo(itemForm.getBatchNo());
                }

                item.setQuantity(itemForm.getQuantity());
                item.setUnitPrice(itemForm.getUnitPrice());
                item.setGstPercentage(
                        itemForm.getGstPercentage() != null
                                ? itemForm.getGstPercentage()
                                : BigDecimal.ZERO
                );
                item.setPharmacy(pharmacy);

                item.calculateTotals();
                billing.addItem(item);
            }
        }

        billing.recalculateTotals();
        return billingRepository.save(billing);
    }

    public List<Billing> getAllBills() {
        return billingRepository.findByPharmacyIdOrderByCreatedAtDesc(tenantPharmacyService.getCurrentPharmacyId());
    }

    public Optional<Billing> getBillById(Long id) {
        return billingRepository.findByIdAndPharmacyId(id, tenantPharmacyService.getCurrentPharmacyId());
    }

    public long getTotalBillCount() {
        return billingRepository.countByPharmacyId(tenantPharmacyService.getCurrentPharmacyId());
    }

    public BigDecimal getTodaySales() {
        Long pharmacyId = tenantPharmacyService.getCurrentPharmacyId();
        LocalDateTime startOfDay = LocalDate.now().atStartOfDay();
        LocalDateTime endOfDay = startOfDay.plusDays(1);
        BigDecimal total = billingRepository.sumGrandTotalBetween(pharmacyId, startOfDay, endOfDay);
        return total != null ? total : BigDecimal.ZERO;
    }

    private String generateBillNumber(Long pharmacyId) {
        String date = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"));
        long count = billingRepository.countByPharmacyId(pharmacyId) + 1;
        return String.format("BILL-%s-%04d", date, count);
    }
}