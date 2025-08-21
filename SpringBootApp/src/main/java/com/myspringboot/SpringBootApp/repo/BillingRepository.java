package com.myspringboot.SpringBootApp.repo;

import com.myspringboot.SpringBootApp.model.Billing;
import org.springframework.data.jpa.repository.JpaRepository;

public interface BillingRepository extends JpaRepository<Billing, Long> {}
