package com.myspringboot.SpringBootApp.security;

import com.myspringboot.SpringBootApp.model.User;
import com.myspringboot.SpringBootApp.Service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

@Service
public class CustomUserDetailsService implements UserDetailsService {

    @Autowired
    private UserService userService;

    @Override
    public UserDetails loadUserByUsername(String identifier) throws UsernameNotFoundException {
        User user = userService.findByIdentifier(identifier);
        
        if (user == null) {
            throw new UsernameNotFoundException("User not found with email or phone");
        }

        return new CustomUserDetails(user);
    }
}
