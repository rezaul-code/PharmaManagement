package com.myspringboot.SpringBootApp.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;

import com.myspringboot.SpringBootApp.model.User;
import com.myspringboot.SpringBootApp.repo.UserRepository;



@Controller
public class SignUpController {
	
	@Autowired
	private UserRepository userRepository;

    @GetMapping("/signup")
    public String sign_up() {
        return "user_auth/user_signup";
    }
    
    @PostMapping("/signup")
    public String save_user(@ModelAttribute User user) {
    	
    	userRepository.save(user);
		return "redirect:/signup?success";
    	
    }
    
    
}
