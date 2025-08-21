package com.myspringboot.SpringBootApp.controller;

import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

import com.myspringboot.SpringBootApp.Service.MedicineService;
import com.myspringboot.SpringBootApp.model.Medicine;
import com.myspringboot.SpringBootApp.repo.MedicineRepository;

@Controller
public class MedicineController {

    @Autowired
    private MedicineRepository medicineRepository;

    @Autowired
    private MedicineService medicineService;

    @GetMapping("/add_medicine")
    public String addMedicineForm(Model model) {
        model.addAttribute("medicine", new Medicine());
        return "pages/add_medicine";
    }

    // Save Medicine
    @PostMapping("/add_medicine")
    public String saveMedicine(Medicine medicine) {
        medicineRepository.save(medicine);
        return "redirect:/show_medicine";
    }

    @GetMapping("/show_medicine")
    public String showMedicines(
            @RequestParam(required = false) Long id,
            @RequestParam(required = false) String name,
            @RequestParam(required = false) String description,
            @RequestParam(required = false) com.myspringboot.SpringBootApp.model.MedicineType type,
            Model model) {

        model.addAttribute("medicines", medicineRepository.searchMedicines(id, name, description, type));
        model.addAttribute("types", com.myspringboot.SpringBootApp.model.MedicineType.values());

        // Keep the search values in form after searching
        model.addAttribute("searchId", id);
        model.addAttribute("searchName", name);
        model.addAttribute("searchDescription", description);
        model.addAttribute("searchType", type);

        return "pages/show_medicine";
    }

    @GetMapping("/med_edit")
    public String editMedicine(@RequestParam("id") Long id, Model model) {
        Medicine medicine = medicineService.getMedicineById(id);
        model.addAttribute("medicine", medicine);
        return "pages/med_edit"; // thymeleaf page inside /pages/
    }

    @PostMapping("/med_edit")
    public String updateMedicine(@ModelAttribute("medicine") Medicine medicine) {
        medicineService.saveMedicine(medicine); // update existing medicine
        return "redirect:/show_medicine";
    }
    @GetMapping("/med_delete")
    public String deleteMedicine(@RequestParam("id") Long id) {
        medicineService.deleteMedicine(id);
        return "redirect:/show_medicine";
    }

}
