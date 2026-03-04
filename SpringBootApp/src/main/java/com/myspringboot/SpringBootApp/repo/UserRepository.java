package com.myspringboot.SpringBootApp.repo;

import com.myspringboot.SpringBootApp.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {

    // Login by email
    User findByEmail(String email);

    // Login by phone
    User findByPhone(String phone);

    // Signup duplicate checks
    User findByUsername(String username);
    boolean existsByEmail(String email);
    boolean existsByPhone(String phone);
}