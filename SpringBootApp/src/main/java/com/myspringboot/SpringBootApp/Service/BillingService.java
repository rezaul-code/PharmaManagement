package com.myspringboot.SpringBootApp.Service;

import com.myspringboot.SpringBootApp.model.Billing;
import com.myspringboot.SpringBootApp.model.BillingItem;
import com.myspringboot.SpringBootApp.model.Medicine;
import com.myspringboot.SpringBootApp.repo.BillingRepository;
import com.myspringboot.SpringBootApp.repo.MedicineRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class BillingService {

    private final BillingRepository billingRepository;
    private final MedicineRepository medicineRepository;

    public BillingService(BillingRepository billingRepository,
                          MedicineRepository medicineRepository) {
        this.billingRepository = billingRepository;
        this.medicineRepository = medicineRepository;
    }

    // ✅ Old single medicine bill
    public Billing createSimpleBill(Long medicineId, Integer quantity, String customerName) {
        Billing bill = new Billing();
        bill.setCustomerName(customerName);

        Medicine medicine = medicineRepository.findById(medicineId)
                .orElseThrow(() -> new IllegalArgumentException("Medicine not found: " + medicineId));

        BillingItem item = new BillingItem();
        item.setMedicine(medicine);
        item.setQuantity(quantity);
        item.setPricePerUnit(medicine.getPrice());
        item.setLineTotal(medicine.getPrice() * quantity);
        item.setBilling(bill);

        bill.addItem(item);
        bill.setTotalAmount(item.getLineTotal());

        // update stock
        medicine.setQuantity(medicine.getQuantity() - quantity);
        medicineRepository.save(medicine);

        return billingRepository.save(bill);
    }

    // ✅ New multiple medicine bill
    @Transactional
    public Billing createBillWithItems(Billing bill) {
        double total = 0.0;

        for (BillingItem item : bill.getItems()) {
            Long medId = item.getMedicineId(); // ✅ comes from form field items[i].medicineId
            if (medId == null) {
                throw new IllegalArgumentException("Medicine ID missing in request");
            }

            Medicine medicine = medicineRepository.findById(medId)
                    .orElseThrow(() -> new IllegalArgumentException("Medicine not found: " + medId));

            item.setMedicine(medicine);
            item.setPricePerUnit(medicine.getPrice());
            item.setLineTotal(medicine.getPrice() * item.getQuantity());
            item.setBilling(bill);

            total += item.getLineTotal();

            // update stock
            medicine.setQuantity(medicine.getQuantity() - item.getQuantity());
            medicineRepository.save(medicine);
        }

        bill.setTotalAmount(total);
        return billingRepository.save(bill);
    }
}
