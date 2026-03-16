package com.myspringboot.SpringBootApp.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
@RequestMapping("/admin")
public class SuperAdminController {

    @GetMapping("/pharmacies")
    public String getPharmacies() {
        return "superadmin/dashboard"; 
    }

    @GetMapping("/subscriptions")
    public String getSubscriptions() {
        // Will implement the template in the future
        return "superadmin/dashboard"; 
    }
}
