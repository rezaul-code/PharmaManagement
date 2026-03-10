package com.myspringboot.SpringBootApp.controller;

import com.myspringboot.SpringBootApp.Service.MedicineService;
import com.myspringboot.SpringBootApp.model.Medicine;
import com.myspringboot.SpringBootApp.model.MedicineType;
import com.myspringboot.SpringBootApp.model.User;
import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
public class MedicineController {

    @Autowired
    private MedicineService medicineService;

    // ── Guard helper ──────────────────────────────────────────────────────

    private boolean canManageMedicines(HttpSession session, RedirectAttributes ra) {
        User user = (User) session.getAttribute("loggedInUser");
        if (user == null) return false;
        if (!user.canManageMedicines()) {
            ra.addFlashAttribute("error",
                    "Access denied: only OWNER or PHARMACIST can manage medicines.");
            return false;
        }
        return true;
    }

    // ── READ (all roles) ──────────────────────────────────────────────────

    @GetMapping({"/medicine/show", "/show_medicine"})
    public String showMedicines(
            @RequestParam(required = false) Long         id,
            @RequestParam(required = false) String       name,
            @RequestParam(required = false) String       description,
            @RequestParam(required = false) MedicineType type,
            @RequestParam(required = false) String       medicineCode,
            HttpSession session,
            Model model) {

        User user = (User) session.getAttribute("loggedInUser");
        if (user == null) return "redirect:/login";

        model.addAttribute("medicines",
                medicineService.searchMedicines(id, name, description, type, medicineCode));
        model.addAttribute("types",              MedicineType.values());
        model.addAttribute("searchId",           id);
        model.addAttribute("searchName",         name);
        model.addAttribute("searchDescription",  description);
        model.addAttribute("searchType",         type);
        model.addAttribute("searchMedicineCode", medicineCode);
        model.addAttribute("currentUser",        user);
        model.addAttribute("activePage",         "medicines");

        // ── Analytics cards removed from here —
        //    They now live on /sales-analytics (SalesAnalyticsController)
        return "pages/show_medicine";
    }

    // ── CREATE (OWNER + PHARMACIST only) ──────────────────────────────────

    @GetMapping({"/medicine/add", "/add_medicine"})
    public String showAddForm(HttpSession session, Model model, RedirectAttributes ra) {
        User user = (User) session.getAttribute("loggedInUser");
        if (user == null) return "redirect:/login";
        if (!user.canManageMedicines()) {
            ra.addFlashAttribute("error",
                    "Access denied: only OWNER or PHARMACIST can add medicines.");
            return "redirect:/medicine/show";
        }
        model.addAttribute("medicine", new Medicine());
        model.addAttribute("types", MedicineType.values());
        model.addAttribute("activePage", "add-medicine");
        return "pages/add_medicine";
    }

    @PostMapping({"/medicine/add", "/add_medicine"})
    public String saveNewMedicine(@ModelAttribute("medicine") Medicine medicine,
                                  HttpSession session,
                                  RedirectAttributes ra) {
        if (!canManageMedicines(session, ra)) return "redirect:/medicine/show";
        medicineService.saveMedicine(medicine);
        ra.addFlashAttribute("success", "Medicine added successfully.");
        return "redirect:/medicine/show";
    }

    // ── EDIT (OWNER + PHARMACIST only) ────────────────────────────────────

    @GetMapping("/medicine/edit/{id}")
    public String showEditForm(@PathVariable Long id,
                               HttpSession session,
                               Model model,
                               RedirectAttributes ra) {
        User user = (User) session.getAttribute("loggedInUser");
        if (user == null) return "redirect:/login";
        if (!user.canManageMedicines()) {
            ra.addFlashAttribute("error",
                    "Access denied: only OWNER or PHARMACIST can edit medicines.");
            return "redirect:/medicine/show";
        }
        try {
            model.addAttribute("medicine", medicineService.getMedicineById(id));
            model.addAttribute("types", MedicineType.values());
            model.addAttribute("activePage", "add-medicine");
            return "pages/med_edit";
        } catch (IllegalArgumentException ex) {
            ra.addFlashAttribute("error", ex.getMessage());
            return "redirect:/medicine/show";
        }
    }

    @GetMapping("/med_edit")
    public String showEditFormLegacy(@RequestParam("id") Long id,
                                     HttpSession session,
                                     Model model,
                                     RedirectAttributes ra) {
        return showEditForm(id, session, model, ra);
    }

    @PostMapping("/medicine/edit/{id}")
    public String updateMedicine(@PathVariable Long id,
                                 @ModelAttribute("medicine") Medicine medicine,
                                 HttpSession session,
                                 RedirectAttributes ra) {
        if (!canManageMedicines(session, ra)) return "redirect:/medicine/show";
        try {
            medicineService.getMedicineById(id);
        } catch (IllegalArgumentException ex) {
            ra.addFlashAttribute("error", ex.getMessage());
            return "redirect:/medicine/show";
        }
        medicine.setId(id);
        medicineService.saveMedicine(medicine);
        ra.addFlashAttribute("success", "Medicine updated successfully.");
        return "redirect:/medicine/show";
    }

    @PostMapping("/med_edit")
    public String updateMedicineLegacy(@ModelAttribute("medicine") Medicine medicine,
                                       HttpSession session,
                                       RedirectAttributes ra) {
        if (medicine.getId() == null) {
            ra.addFlashAttribute("error", "Medicine id is required for update.");
            return "redirect:/medicine/show";
        }
        return updateMedicine(medicine.getId(), medicine, session, ra);
    }

    // ── DELETE (OWNER + PHARMACIST only) ──────────────────────────────────

    @GetMapping("/medicine/delete/{id}")
    public String deleteMedicine(@PathVariable Long id,
                                 HttpSession session,
                                 RedirectAttributes ra) {
        if (!canManageMedicines(session, ra)) return "redirect:/medicine/show";
        try {
            medicineService.deleteMedicine(id);
            ra.addFlashAttribute("success", "Medicine deleted successfully.");
        } catch (EmptyResultDataAccessException | IllegalArgumentException ex) {
            ra.addFlashAttribute("error", "Medicine not found with id: " + id);
        }
        return "redirect:/medicine/show";
    }

    @GetMapping("/med_delete")
    public String deleteMedicineLegacy(@RequestParam("id") Long id,
                                       HttpSession session,
                                       RedirectAttributes ra) {
        return deleteMedicine(id, session, ra);
    }
}