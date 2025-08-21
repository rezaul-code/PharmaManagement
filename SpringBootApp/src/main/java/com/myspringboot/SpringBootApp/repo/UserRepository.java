package com.myspringboot.SpringBootApp.repo;

import org.springframework.data.jpa.repository.JpaRepository;

import com.myspringboot.SpringBootApp.model.User;

public interface UserRepository extends JpaRepository <User,Long> {
	 User findByEmail(String email);
}
