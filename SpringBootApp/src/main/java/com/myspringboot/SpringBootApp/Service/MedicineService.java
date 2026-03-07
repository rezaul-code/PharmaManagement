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

    public Medicine saveMedicine(Medicine medicine) {
        Long pharmacyId = tenantPharmacyService.getCurrentPharmacyId();
        if (medicine.getId() != null) {
            medicineRepository.findByIdAndPharmacyId(medicine.getId(), pharmacyId)
                    .orElseThrow(() -> new IllegalArgumentException("Medicine not found with id: " + medicine.getId()));
        }

        medicine.setPharmacy(tenantPharmacyService.getCurrentPharmacy());
        return medicineRepository.save(medicine);
    }

    public Medicine save(Medicine medicine) {
        return saveMedicine(medicine);
    }

    public List<Medicine> getAll() {
        return medicineRepository.findByPharmacyId(tenantPharmacyService.getCurrentPharmacyId());
    }

    public Medicine getMedicineById(Long id) {
        return medicineRepository.findByIdAndPharmacyId(id, tenantPharmacyService.getCurrentPharmacyId())
                .orElseThrow(() -> new IllegalArgumentException("Medicine not found with id: " + id));
    }

    public Optional<Medicine> getById(Long id) {
        return medicineRepository.findByIdAndPharmacyId(id, tenantPharmacyService.getCurrentPharmacyId());
    }

    public void deleteMedicine(Long id) {
        Medicine medicine = medicineRepository.findByIdAndPharmacyId(id, tenantPharmacyService.getCurrentPharmacyId())
                .orElseThrow(() -> new IllegalArgumentException("Medicine not found with id: " + id));
        medicineRepository.delete(medicine);
    }

    public void deleteById(Long id) {
        deleteMedicine(id);
    }

    public List<Medicine> searchMedicines(Long id, String name, String description, MedicineType type) {
        return medicineRepository.searchMedicines(
                tenantPharmacyService.getCurrentPharmacyId(),
                id,
                name,
                description,
                type
        );
    }

    public List<Medicine> searchByName(String keyword) {
        return medicineRepository.findByNameContainingIgnoreCaseAndPharmacyId(
                keyword,
                tenantPharmacyService.getCurrentPharmacyId()
        );
    }

    public long getTotalCount() {
        return medicineRepository.countByPharmacyId(tenantPharmacyService.getCurrentPharmacyId());
    }

    public long getLowStockCount() {
        return medicineRepository.countByStockQuantityLessThanEqualAndPharmacyId(
                10,
                tenantPharmacyService.getCurrentPharmacyId()
        );
    }

    public List<Medicine> getLowStockMedicines() {
        return medicineRepository.findByStockQuantityLessThanEqualAndPharmacyId(
                10,
                tenantPharmacyService.getCurrentPharmacyId()
        );
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