package com.myspringboot.SpringBootApp.Service;

import com.myspringboot.SpringBootApp.model.Medicine;
import com.myspringboot.SpringBootApp.model.MedicineType;
import com.myspringboot.SpringBootApp.repo.MedicineRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Service
public class MedicineService {

    @Autowired
    private MedicineRepository medicineRepository;

    @Autowired
    private TenantPharmacyService tenantPharmacyService;

    // ── Medicine Code generation ─────────────────────────────────────────

    public String generateNextMedicineCode() {
        Long pharmacyId = tenantPharmacyService.getCurrentPharmacyId();
        int next = medicineRepository
                .findMaxCodeSequence(pharmacyId)
                .map(max -> max + 1)
                .orElse(1);
        return String.format("MED-%03d", next);
    }

    // ── Save (create + update) ───────────────────────────────────────────

    public Medicine saveMedicine(Medicine medicine) {
        Long pharmacyId = tenantPharmacyService.getCurrentPharmacyId();

        if (medicine.getId() != null) {
            // UPDATE — fetch existing to preserve fields the form doesn't send
            Medicine existing = medicineRepository
                    .findByIdAndPharmacyId(medicine.getId(), pharmacyId)
                    .orElseThrow(() -> new IllegalArgumentException(
                            "Medicine not found with id: " + medicine.getId()));

            // Preserve the medicine code — never allow it to change on edit
            medicine.setMedicineCode(existing.getMedicineCode());

        } else {
            // CREATE — assign code if not already set
            if (medicine.getMedicineCode() == null
                    || medicine.getMedicineCode().isBlank()) {
                medicine.setMedicineCode(generateNextMedicineCode());
            }
        }

        medicine.setPharmacy(tenantPharmacyService.getCurrentPharmacy());
        return medicineRepository.save(medicine);
    }

    /** Alias for backward compatibility. */
    public Medicine save(Medicine medicine) {
        return saveMedicine(medicine);
    }

    // ── Read ─────────────────────────────────────────────────────────────

    public List<Medicine> getAll() {
        return medicineRepository.findByPharmacyId(
                tenantPharmacyService.getCurrentPharmacyId());
    }

    public Medicine getMedicineById(Long id) {
        return medicineRepository
                .findByIdAndPharmacyId(id, tenantPharmacyService.getCurrentPharmacyId())
                .orElseThrow(() -> new IllegalArgumentException(
                        "Medicine not found with id: " + id));
    }

    public Optional<Medicine> getById(Long id) {
        return medicineRepository.findByIdAndPharmacyId(
                id, tenantPharmacyService.getCurrentPharmacyId());
    }

    public Optional<Medicine> getByMedicineCode(String code) {
        return medicineRepository.findByMedicineCodeAndPharmacyId(
                code, tenantPharmacyService.getCurrentPharmacyId());
    }

    // ── Delete ────────────────────────────────────────────────────────────

    public void deleteMedicine(Long id) {
        Medicine medicine = medicineRepository
                .findByIdAndPharmacyId(id, tenantPharmacyService.getCurrentPharmacyId())
                .orElseThrow(() -> new IllegalArgumentException(
                        "Medicine not found with id: " + id));
        medicineRepository.delete(medicine);
    }

    public void deleteById(Long id) {
        deleteMedicine(id);
    }

    // ── Search ────────────────────────────────────────────────────────────

    /**
     * Original 4-param signature — unchanged, delegates to extended version.
     * All existing callers continue to work without modification.
     */
    public List<Medicine> searchMedicines(
            Long id, String name, String description, MedicineType type) {
        return searchMedicines(id, name, description, type, null);
    }

    /** Extended search that also accepts an optional medicine-code fragment. */
    public List<Medicine> searchMedicines(
            Long id, String name, String description,
            MedicineType type, String medicineCode) {
        return medicineRepository.searchMedicinesWithCode(
                tenantPharmacyService.getCurrentPharmacyId(),
                id, name, description, type,
                (medicineCode != null && medicineCode.isBlank()) ? null : medicineCode
        );
    }

    public List<Medicine> searchByName(String keyword) {
        return medicineRepository.findByNameContainingIgnoreCaseAndPharmacyId(
                keyword, tenantPharmacyService.getCurrentPharmacyId());
    }

    // ── Stock analytics ───────────────────────────────────────────────────

    public long getTotalCount() {
        return medicineRepository.countByPharmacyId(
                tenantPharmacyService.getCurrentPharmacyId());
    }

    public long getLowStockCount() {
        return medicineRepository.countByStockQuantityLessThanEqualAndPharmacyId(
                10, tenantPharmacyService.getCurrentPharmacyId());
    }

    public List<Medicine> getLowStockMedicines() {
        return medicineRepository.findByStockQuantityLessThanEqualAndPharmacyId(
                10, tenantPharmacyService.getCurrentPharmacyId());
    }

    // ── Expiry analytics ──────────────────────────────────────────────────

    public long getExpiringWithin30DaysCount() {
        LocalDate today    = LocalDate.now();
        LocalDate deadline = today.plusDays(30);
        return medicineRepository.countExpiringBetween(
                tenantPharmacyService.getCurrentPharmacyId(), today, deadline);
    }

    public List<Medicine> getExpiringWithin30Days() {
        LocalDate today    = LocalDate.now();
        LocalDate deadline = today.plusDays(30);
        return medicineRepository.findExpiringBetween(
                tenantPharmacyService.getCurrentPharmacyId(), today, deadline);
    }

    // ── Profit analytics ──────────────────────────────────────────────────

    /**
     * Total inventory cost (purchasePrice × stock) across all medicines.
     */
    public BigDecimal getTotalInventoryCostValue() {
        BigDecimal val = medicineRepository.sumInventoryValueAtCost(
                tenantPharmacyService.getCurrentPharmacyId());
        return val != null ? val : BigDecimal.ZERO;
    }

    /**
     * Total inventory value at selling price (price × stock).
     */
    public BigDecimal getTotalInventorySellingValue() {
        BigDecimal val = medicineRepository.sumInventoryValueAtSelling(
                tenantPharmacyService.getCurrentPharmacyId());
        return val != null ? val : BigDecimal.ZERO;
    }

    /**
     * Potential profit if all current stock were sold.
     * = sellingValue − costValue
     */
    public BigDecimal getPotentialInventoryProfit() {
        return getTotalInventorySellingValue()
                .subtract(getTotalInventoryCostValue());
    }

    /**
     * Top N medicines by absolute profit per unit (descending).
     */
    public List<Medicine> getTopProfitMedicines(int limit) {
        return medicineRepository
                .findTopProfitMedicines(tenantPharmacyService.getCurrentPharmacyId())
                .stream()
                .limit(limit)
                .toList();
    }

    /**
     * Medicines where selling price < purchase price (negative margin alert).
     */
    public List<Medicine> getNegativeMarginMedicines() {
        return medicineRepository.findNegativeMarginMedicines(
                tenantPharmacyService.getCurrentPharmacyId());
    }

    /**
     * Count of negative-margin medicines — useful for dashboard warning badge.
     */
    public long getNegativeMarginCount() {
        return getNegativeMarginMedicines().size();
    }
}