package com.myspringboot.SpringBootApp.controller;

import com.myspringboot.SpringBootApp.Service.MedicineService;
import com.myspringboot.SpringBootApp.model.Medicine;
import com.myspringboot.SpringBootApp.model.MedicineType;
import com.myspringboot.SpringBootApp.repo.MedicineRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

@Controller
public class MedicineController {

    @Autowired
    private MedicineRepository medicineRepository;  // kept as in your original

    @Autowired
    private MedicineService medicineService;        // added for service-layer calls

    // ─── Add Medicine Form (GET) ──────────────────────────────────────

    @GetMapping("/medicine/add")
    public String showAddMedicineForm(Model model) {
        model.addAttribute("medicine", new Medicine());
        model.addAttribute("types", MedicineType.values());
        return "pages/add_medicine";
    }

    // ─── Save New Medicine (POST) ─────────────────────────────────────

    @PostMapping("/add_medicine")
    public String saveMedicine(Medicine medicine) {
        medicineRepository.save(medicine);
        return "redirect:/show_medicine";
    }

    // ─── Show / Search Medicines (GET) ────────────────────────────────
    // Matches exactly what is on lines 38–56 of your screenshot.

    @GetMapping("/show_medicine")
    public String showMedicines(
            @RequestParam(required = false) Long id,
            @RequestParam(required = false) String name,
            @RequestParam(required = false) String description,
            @RequestParam(required = false) MedicineType type,
            Model model) {

        // searchMedicines() — now resolved in MedicineRepository via @Query
        model.addAttribute("medicines",
            medicineRepository.searchMedicines(id, name, description, type));

        model.addAttribute("types", MedicineType.values());

        // Keep the search values in form after searching
        model.addAttribute("searchId",          id);
        model.addAttribute("searchName",        name);
        model.addAttribute("searchDescription", description);
        model.addAttribute("searchType",        type);

        return "pages/show_medicine";
    }

    // ─── Edit Medicine Form (GET) ─────────────────────────────────────
    // Matches lines 58–63 of your screenshot.

    @GetMapping("/med_edit")
    public String editMedicine(@RequestParam("id") Long id, Model model) {
        // getMedicineById() — now resolved in MedicineService
        Medicine medicine = medicineService.getMedicineById(id);
        model.addAttribute("medicine", medicine);
        return "pages/med_edit";   // thymeleaf page inside /pages/
    }

    // ─── Update Medicine (POST) ───────────────────────────────────────
    // Matches lines 65–68 of your screenshot.

    @PostMapping("/med_edit")
    public String updateMedicine(@ModelAttribute("medicine") Medicine medicine) {
        medicineService.saveMedicine(medicine); // update existing medicine
        return "redirect:/show_medicine";
    }

    // ─── Delete Medicine (GET) ────────────────────────────────────────
    // Matches lines 70–74 of your screenshot.

    @GetMapping("/med_delete")
    public String deleteMedicine(@RequestParam("id") Long id) {
        medicineService.deleteMedicine(id);     // deleteMedicine() — now resolved
        return "redirect:/show_medicine";
    }

    // ─── Also support /medicine/* URL variants (used in sidebar links) ─

    @GetMapping("/medicine/show")
    public String showMedicinesAlt(
            @RequestParam(required = false) Long id,
            @RequestParam(required = false) String name,
            @RequestParam(required = false) String description,
            @RequestParam(required = false) MedicineType type,
            Model model) {
        return showMedicines(id, name, description, type, model);
    }

    @GetMapping("/medicine/edit/{id}")
    public String editMedicineAlt(@PathVariable Long id, Model model) {
        return editMedicine(id, model);
    }

    @PostMapping("/medicine/edit/{id}")
    public String updateMedicineAlt(@PathVariable Long id,
                                    @ModelAttribute("medicine") Medicine medicine) {
        medicine.setId(id);
        return updateMedicine(medicine);
    }

    @GetMapping("/medicine/delete/{id}")
    public String deleteMedicineAlt(@PathVariable Long id) {
        return deleteMedicine(id);
    }
}