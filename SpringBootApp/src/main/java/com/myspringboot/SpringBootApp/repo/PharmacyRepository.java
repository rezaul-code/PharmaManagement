package com.myspringboot.SpringBootApp.repo;

import com.myspringboot.SpringBootApp.model.Pharmacy;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface PharmacyRepository extends JpaRepository<Pharmacy, Long> {

    Optional<Pharmacy> findByName(String name);

    Optional<Pharmacy> findByTenantId(String tenantId);

    boolean existsByName(String name);
}