package com.myspringboot.SpringBootApp.Service;

import com.myspringboot.SpringBootApp.model.Billing;
import com.myspringboot.SpringBootApp.model.BillingItem;
import com.myspringboot.SpringBootApp.model.Medicine;
import com.myspringboot.SpringBootApp.model.Pharmacy;
import com.myspringboot.SpringBootApp.model.User;
import com.myspringboot.SpringBootApp.repo.BillingItemRepository;
import com.myspringboot.SpringBootApp.repo.BillingRepository;
import com.myspringboot.SpringBootApp.repo.MedicineRepository;
import com.myspringboot.SpringBootApp.repo.PharmacyRepository;
import com.myspringboot.SpringBootApp.repo.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Runs once on startup.
 *
 * 1. Ensures a default Pharmacy row (id=1) exists — creates it if the
 *    database is fresh (e.g. after a drop).
 * 2. Links any legacy data (users / medicines / bills / items that have
 *    no pharmacy_id yet) to that default pharmacy.
 *
 * On a brand-new database all four "findByPharmacyIsNull" lists will be
 * empty, so the migration loop is a no-op and the app starts cleanly.
 */
@Component
public class MultiTenantBootstrapService implements CommandLineRunner {

    @Autowired
    private PharmacyRepository pharmacyRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private MedicineRepository medicineRepository;

    @Autowired
    private BillingRepository billingRepository;

    @Autowired
    private BillingItemRepository billingItemRepository;

    @Override
    @Transactional
    public void run(String... args) {

        // ── Step 1: Ensure the default pharmacy exists ────────────────
        // On a fresh database this INSERT is needed before anything else.
        // On an existing database it is a safe no-op (findById returns it).
        Pharmacy defaultPharmacy = pharmacyRepository.findById(TenantContext.DEFAULT_PHARMACY_ID)
                .orElseGet(() -> {
                    Pharmacy p = new Pharmacy();
                    p.setName("My Pharmacy");
                    p.setAddress("123 Main Street");
                    p.setPhone("9000000000");
                    p.setEmail("admin@mypharmacy.com");
                    p.setLicenseNumber("LIC-001");
                    Pharmacy saved = pharmacyRepository.save(p);
                    System.out.println("[Bootstrap] Default pharmacy created with id: " + saved.getId());
                    return saved;
                });

        // ── Step 2: Migrate legacy users ─────────────────────────────
        List<User> usersWithoutPharmacy = userRepository.findByPharmacyIsNull();
        for (User user : usersWithoutPharmacy) {
            user.setPharmacy(defaultPharmacy);
        }
        if (!usersWithoutPharmacy.isEmpty()) {
            userRepository.saveAll(usersWithoutPharmacy);
            System.out.println("[Bootstrap] Linked " + usersWithoutPharmacy.size() + " user(s) to default pharmacy.");
        }

        // ── Step 3: Migrate legacy medicines ─────────────────────────
        List<Medicine> medicinesWithoutPharmacy = medicineRepository.findByPharmacyIsNull();
        for (Medicine medicine : medicinesWithoutPharmacy) {
            medicine.setPharmacy(defaultPharmacy);
        }
        if (!medicinesWithoutPharmacy.isEmpty()) {
            medicineRepository.saveAll(medicinesWithoutPharmacy);
            System.out.println("[Bootstrap] Linked " + medicinesWithoutPharmacy.size() + " medicine(s) to default pharmacy.");
        }

        // ── Step 4: Migrate legacy billings ──────────────────────────
        List<Billing> billsWithoutPharmacy = billingRepository.findByPharmacyIsNull();
        for (Billing billing : billsWithoutPharmacy) {
            billing.setPharmacy(defaultPharmacy);
        }
        if (!billsWithoutPharmacy.isEmpty()) {
            billingRepository.saveAll(billsWithoutPharmacy);
            System.out.println("[Bootstrap] Linked " + billsWithoutPharmacy.size() + " billing(s) to default pharmacy.");
        }

        // ── Step 5: Migrate legacy billing items ─────────────────────
        List<BillingItem> itemsWithoutPharmacy = billingItemRepository.findByPharmacyIsNull();
        for (BillingItem item : itemsWithoutPharmacy) {
            if (item.getBilling() != null && item.getBilling().getPharmacy() != null) {
                item.setPharmacy(item.getBilling().getPharmacy());
            } else {
                item.setPharmacy(defaultPharmacy);
            }
        }
        if (!itemsWithoutPharmacy.isEmpty()) {
            billingItemRepository.saveAll(itemsWithoutPharmacy);
            System.out.println("[Bootstrap] Linked " + itemsWithoutPharmacy.size() + " billing item(s) to default pharmacy.");
        }

        System.out.println("[Bootstrap] Multi-tenant bootstrap complete.");
    }
}