package com.myspringboot.SpringBootApp.Service;

import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.myspringboot.SpringBootApp.model.Medicine;
import com.myspringboot.SpringBootApp.repo.MedicineRepository;

@Service
public class MedicineService {

    @Autowired
    private MedicineRepository medicineRepository;

    // Save or Update Medicine
    public Medicine saveMedicine(Medicine medicine) {
        return medicineRepository.save(medicine);
    }

    // Get Medicine by ID
    public Medicine getMedicineById(Long id) {
        Optional<Medicine> optionalMedicine = medicineRepository.findById(id);
        return optionalMedicine.orElse(null); // return null if not found
    }

    // Delete Medicine
    public void deleteMedicine(Long id) {
        medicineRepository.deleteById(id);
    }
}
