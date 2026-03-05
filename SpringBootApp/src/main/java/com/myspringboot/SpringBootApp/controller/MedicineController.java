package com.myspringboot.SpringBootApp.controller;

import com.myspringboot.SpringBootApp.Service.MedicineService;
import com.myspringboot.SpringBootApp.model.Medicine;
import com.myspringboot.SpringBootApp.model.MedicineType;
import com.myspringboot.SpringBootApp.repo.MedicineRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
public class MedicineController {

    @Autowired
    private MedicineRepository medicineRepository;

    @Autowired
    private MedicineService medicineService;

    @GetMapping({"/medicine/add", "/add_medicine"})
    public String showAddForm(Model model) {
        model.addAttribute("medicine", new Medicine());
        model.addAttribute("types", MedicineType.values());
        return "pages/add_medicine";
    }

    @PostMapping({"/medicine/add", "/add_medicine"})
    public String saveNewMedicine(@ModelAttribute("medicine") Medicine medicine) {
        medicineService.saveMedicine(medicine);
        return "redirect:/medicine/show";
    }

    @GetMapping({"/medicine/show", "/show_medicine"})
    public String showMedicines(
            @RequestParam(required = false) Long id,
            @RequestParam(required = false) String name,
            @RequestParam(required = false) String description,
            @RequestParam(required = false) MedicineType type,
            Model model) {

        model.addAttribute("medicines", medicineRepository.searchMedicines(id, name, description, type));
        model.addAttribute("types", MedicineType.values());
        model.addAttribute("searchId", id);
        model.addAttribute("searchName", name);
        model.addAttribute("searchDescription", description);
        model.addAttribute("searchType", type);
        return "pages/show_medicine";
    }

    @GetMapping("/medicine/edit/{id}")
    public String showEditForm(@PathVariable Long id, Model model, RedirectAttributes redirectAttributes) {
        try {
            model.addAttribute("medicine", medicineService.getMedicineById(id));
            model.addAttribute("types", MedicineType.values());
            return "pages/med_edit";
        } catch (IllegalArgumentException ex) {
            redirectAttributes.addFlashAttribute("error", ex.getMessage());
            return "redirect:/medicine/show";
        }
    }

    @GetMapping("/med_edit")
    public String showEditFormLegacy(@RequestParam("id") Long id, Model model, RedirectAttributes redirectAttributes) {
        return showEditForm(id, model, redirectAttributes);
    }

    @PostMapping("/medicine/edit/{id}")
    public String updateMedicine(@PathVariable Long id,
                                 @ModelAttribute("medicine") Medicine medicine,
                                 RedirectAttributes redirectAttributes) {
        try {
            medicineService.getMedicineById(id);
        } catch (IllegalArgumentException ex) {
            redirectAttributes.addFlashAttribute("error", ex.getMessage());
            return "redirect:/medicine/show";
        }

        medicine.setId(id);
        medicineService.saveMedicine(medicine);
        return "redirect:/medicine/show";
    }

    @PostMapping("/med_edit")
    public String updateMedicineLegacy(@ModelAttribute("medicine") Medicine medicine,
                                       RedirectAttributes redirectAttributes) {
        if (medicine.getId() == null) {
            redirectAttributes.addFlashAttribute("error", "Medicine id is required for update.");
            return "redirect:/medicine/show";
        }
        return updateMedicine(medicine.getId(), medicine, redirectAttributes);
    }

    @GetMapping("/medicine/delete/{id}")
    public String deleteMedicine(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        try {
            medicineService.deleteMedicine(id);
        } catch (EmptyResultDataAccessException ex) {
            redirectAttributes.addFlashAttribute("error", "Medicine not found with id: " + id);
        }
        return "redirect:/medicine/show";
    }

    @GetMapping("/med_delete")
    public String deleteMedicineLegacy(@RequestParam("id") Long id, RedirectAttributes redirectAttributes) {
        return deleteMedicine(id, redirectAttributes);
    }
}
