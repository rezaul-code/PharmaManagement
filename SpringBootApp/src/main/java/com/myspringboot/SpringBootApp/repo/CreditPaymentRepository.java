// src/main/java/com/myspringboot/SpringBootApp/repo/CreditPaymentRepository.java
package com.myspringboot.SpringBootApp.repo;

import com.myspringboot.SpringBootApp.model.CreditPayment;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface CreditPaymentRepository extends JpaRepository<CreditPayment, Long> {

    List<CreditPayment> findByBillingIdAndPharmacyIdOrderByPaidAtDesc(
            Long billingId, Long pharmacyId);

    List<CreditPayment> findByPharmacyIdOrderByPaidAtDesc(Long pharmacyId);
}