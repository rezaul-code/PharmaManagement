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
    private MedicineRepository medicineRepository;

    @Autowired
    private MedicineService medicineService;

    // ════════════════════════════════════════════════════════════════
    //  ADD MEDICINE
    //  GET  /medicine/add   → show form       (sidebar link)
    //  POST /medicine/add   → save & redirect (form action in add_medicine.html)
    //  GET  /add_medicine   → legacy alias
    //  POST /add_medicine   → legacy alias (your original controller)
    // ════════════════════════════════════════════════════════════════

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

    // ════════════════════════════════════════════════════════════════
    //  SHOW / SEARCH MEDICINES
    //  GET /medicine/show   → sidebar link, show_medicine.html hrefs
    //  GET /show_medicine   → legacy alias (your original controller)
    // ════════════════════════════════════════════════════════════════

    @GetMapping({"/medicine/show", "/show_medicine"})
    public String showMedicines(
            @RequestParam(required = false) Long id,
            @RequestParam(required = false) String name,
            @RequestParam(required = false) String description,
            @RequestParam(required = false) MedicineType type,
            Model model) {

        model.addAttribute("medicines",
            medicineRepository.searchMedicines(id, name, description, type));
        model.addAttribute("types",             MedicineType.values());
        model.addAttribute("searchId",          id);
        model.addAttribute("searchName",        name);
        model.addAttribute("searchDescription", description);
        model.addAttribute("searchType",        type);
        return "pages/show_medicine";
    }

    // ════════════════════════════════════════════════════════════════
    //  EDIT MEDICINE
    //  GET  /medicine/edit/{id}   → show edit form  (show_medicine.html href)
    //  POST /medicine/edit/{id}   → update          (med_edit.html form action)
    //  GET  /med_edit?id=X        → legacy alias
    //  POST /med_edit             → legacy alias
    // ════════════════════════════════════════════════════════════════

    @GetMapping("/medicine/edit/{id}")
    public String showEditForm(@PathVariable Long id, Model model) {
        model.addAttribute("medicine", medicineService.getMedicineById(id));
        model.addAttribute("types", MedicineType.values());
        return "pages/med_edit";
    }

    // Legacy: GET /med_edit?id=X
    @GetMapping("/med_edit")
    public String showEditFormLegacy(@RequestParam("id") Long id, Model model) {
        return showEditForm(id, model);
    }

    @PostMapping("/medicine/edit/{id}")
    public String updateMedicine(@PathVariable Long id,
                                 @ModelAttribute("medicine") Medicine medicine) {
        medicine.setId(id);
        medicineService.saveMedicine(medicine);
        return "redirect:/medicine/show";
    }

    // Legacy: POST /med_edit
    @PostMapping("/med_edit")
    public String updateMedicineLegacy(@ModelAttribute("medicine") Medicine medicine) {
        medicineService.saveMedicine(medicine);
        return "redirect:/medicine/show";
    }

    // ════════════════════════════════════════════════════════════════
    //  DELETE MEDICINE
    //  GET /medicine/delete/{id}  → show_medicine.html href
    //  GET /med_delete?id=X       → legacy alias
    // ════════════════════════════════════════════════════════════════

    @GetMapping("/medicine/delete/{id}")
    public String deleteMedicine(@PathVariable Long id) {
        medicineService.deleteMedicine(id);
        return "redirect:/medicine/show";
    }

    // Legacy: GET /med_delete?id=X
    @GetMapping("/med_delete")
    public String deleteMedicineLegacy(@RequestParam("id") Long id) {
        return deleteMedicine(id);
    }
}