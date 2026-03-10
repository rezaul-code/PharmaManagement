package com.myspringboot.SpringBootApp.Service;

import com.myspringboot.SpringBootApp.model.Medicine;
import com.myspringboot.SpringBootApp.model.MedicineType;
import com.myspringboot.SpringBootApp.repo.MedicineRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Service
public class MedicineService {

    @Autowired
    private MedicineRepository medicineRepository;

    @Autowired
    private TenantPharmacyService tenantPharmacyService;

    // ── Code generation ───────────────────────────────────────────────

    /**
     * Generates the next sequential medicine code for the current pharmacy.
     * Format: MED-001, MED-002, … MED-999, MED-1000, …
     * Thread-safe: relies on DB unique constraint as the final guard;
     * the optimistic counter handles the common case without locking.
     */
    public String generateNextMedicineCode() {
        Long pharmacyId = tenantPharmacyService.getCurrentPharmacyId();
        int next = medicineRepository
                .findMaxCodeSequence(pharmacyId)
                .map(max -> max + 1)
                .orElse(1);                          // first medicine ever
        return String.format("MED-%03d", next);      // MED-001 … MED-999, then MED-1000
    }

    // ── Save (create + update) ────────────────────────────────────────

    public Medicine saveMedicine(Medicine medicine) {
        Long pharmacyId = tenantPharmacyService.getCurrentPharmacyId();

        if (medicine.getId() != null) {
            // UPDATE – keep the existing code; never regenerate on edit
            medicineRepository.findByIdAndPharmacyId(medicine.getId(), pharmacyId)
                    .orElseThrow(() -> new IllegalArgumentException(
                            "Medicine not found with id: " + medicine.getId()));
        } else {
            // CREATE – assign a new code only if none was set
            if (medicine.getMedicineCode() == null
                    || medicine.getMedicineCode().isBlank()) {
                medicine.setMedicineCode(generateNextMedicineCode());
            }
        }

        medicine.setPharmacy(tenantPharmacyService.getCurrentPharmacy());
        return medicineRepository.save(medicine);
    }

    /** Alias kept for backward compatibility. */
    public Medicine save(Medicine medicine) {
        return saveMedicine(medicine);
    }

    // ── Read ─────────────────────────────────────────────────────────

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

    /** Look up by medicine code (useful for barcode scanners). */
    public Optional<Medicine> getByMedicineCode(String code) {
        return medicineRepository.findByMedicineCodeAndPharmacyId(
                code, tenantPharmacyService.getCurrentPharmacyId());
    }

    // ── Delete ────────────────────────────────────────────────────────

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

    // ── Search ────────────────────────────────────────────────────────

    /**
     * Original signature – delegates to extended query with null code filter
     * so existing callers continue to work unchanged.
     */
    public List<Medicine> searchMedicines(
            Long id, String name, String description, MedicineType type) {
        return searchMedicines(id, name, description, type, null);
    }

    /**
     * Extended search that also accepts an optional medicine-code fragment.
     */
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

    // ── Analytics ─────────────────────────────────────────────────────

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
}