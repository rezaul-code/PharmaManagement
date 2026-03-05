package com.myspringboot.SpringBootApp.controller;

import com.myspringboot.SpringBootApp.Service.BillingService;
import com.myspringboot.SpringBootApp.Service.MedicineService;
import com.myspringboot.SpringBootApp.dto.BillingForm;
import com.myspringboot.SpringBootApp.model.Billing;
import com.myspringboot.SpringBootApp.model.Medicine;
import com.myspringboot.SpringBootApp.model.User;
import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import java.util.List;
import java.util.Optional;

@Controller
@RequestMapping("/billing")
public class BillingController {

    @Autowired
    private BillingService billingService;

    @Autowired
    private MedicineService medicineService;

    @GetMapping("/new")
    public String newBillForm(Model model) {
        model.addAttribute("billingForm", new BillingForm());
        return "pages/billing_new";
    }

    @PostMapping("/create")
    public String createBill(
            @Valid @ModelAttribute("billingForm") BillingForm form,
            BindingResult bindingResult,
            HttpSession session,
            Model model) {

        if (bindingResult.hasErrors()) {
            model.addAttribute("error", "Please correct the highlighted billing form fields.");
            model.addAttribute("billingForm", form);
            return "pages/billing_new";
        }

        User user = (User) session.getAttribute("loggedInUser");

        try {
            Billing billing = billingService.createBill(form, user);
            return "redirect:/billing/view/" + billing.getId();
        } catch (Exception e) {
            model.addAttribute("error", "Failed to create bill: " + e.getMessage());
            model.addAttribute("billingForm", form);
            return "pages/billing_new";
        }
    }

    @GetMapping("/view/{id}")
    public String viewBill(@PathVariable Long id, Model model) {
        Optional<Billing> billing = billingService.getBillById(id);
        if (billing.isEmpty()) {
            return "redirect:/billing/list";
        }
        model.addAttribute("billing", billing.get());
        return "pages/billing_view";
    }

    @GetMapping("/list")
    public String listBills(Model model) {
        model.addAttribute("bills", billingService.getAllBills());
        return "pages/billing_list";
    }

    @GetMapping("/medicine/search")
    @ResponseBody
    public List<Medicine> searchMedicine(@RequestParam("q") String query) {
        return medicineService.searchByName(query);
    }

    @GetMapping("/medicine/{id}")
    @ResponseBody
    public ResponseEntity<Medicine> getMedicineById(@PathVariable Long id) {
        return medicineService.getById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }
}
