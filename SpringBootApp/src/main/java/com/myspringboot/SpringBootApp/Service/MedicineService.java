package com.myspringboot.SpringBootApp.Service;

import com.myspringboot.SpringBootApp.model.Medicine;
import com.myspringboot.SpringBootApp.model.MedicineType;
import com.myspringboot.SpringBootApp.repo.MedicineRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class MedicineService {

    @Autowired
    private MedicineRepository medicineRepository;

    // ─── CRUD ─────────────────────────────────────────────────────────

    /**
     * saveMedicine() — resolves medicineService.saveMedicine(medicine)
     * in MedicineController (add + update).
     */
    public Medicine saveMedicine(Medicine medicine) {
        return medicineRepository.save(medicine);
    }

    /** Alias kept for BillingService which calls save() internally. */
    public Medicine save(Medicine medicine) {
        return saveMedicine(medicine);
    }

    public List<Medicine> getAll() {
        return medicineRepository.findAll();
    }

    /**
     * getMedicineById() — resolves medicineService.getMedicineById(id)
     * in MedicineController (/med_edit GET).
     *
     * Returns the Medicine or throws a clean IllegalArgumentException
     * so the controller never has to handle an empty Optional.
     */
    public Medicine getMedicineById(Long id) {
        return medicineRepository.findById(id)
                .orElseThrow(() ->
                    new IllegalArgumentException("Medicine not found with id: " + id));
    }

    /** Optional-returning variant used by BillingController. */
    public Optional<Medicine> getById(Long id) {
        return medicineRepository.findById(id);
    }

    /**
     * deleteMedicine() — resolves medicineService.deleteMedicine(id)
     * in MedicineController (/med_delete).
     */
    public void deleteMedicine(Long id) {
        medicineRepository.deleteById(id);
    }

    /** Alias kept for any existing call sites using deleteById. */
    public void deleteById(Long id) {
        deleteMedicine(id);
    }

    // ─── Multi-field search ───────────────────────────────────────────

    /**
     * Delegates to MedicineRepository.searchMedicines() —
     * resolves medicineRepository.searchMedicines(id, name, description, type)
     * in MedicineController (/show_medicine GET).
     */
    public List<Medicine> searchMedicines(Long id, String name,
                                          String description, MedicineType type) {
        return medicineRepository.searchMedicines(id, name, description, type);
    }

    /** Simple name-only search used by billing autocomplete. */
    public List<Medicine> searchByName(String keyword) {
        return medicineRepository.findByNameContainingIgnoreCase(keyword);
    }

    // ─── Dashboard stats ──────────────────────────────────────────────

    public long getTotalCount() {
        return medicineRepository.count();
    }

    public long getLowStockCount() {
        return medicineRepository.countByStockQuantityLessThanEqual(10);
    }

    public List<Medicine> getLowStockMedicines() {
        return medicineRepository.findByStockQuantityLessThanEqual(10);
    }
}