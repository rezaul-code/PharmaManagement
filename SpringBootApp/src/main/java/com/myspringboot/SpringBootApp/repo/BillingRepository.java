package com.myspringboot.SpringBootApp.repo;

import com.myspringboot.SpringBootApp.model.Billing;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface BillingRepository extends JpaRepository<Billing, Long> {

    List<Billing> findAllByOrderByCreatedAtDesc();

    @Query("SELECT COALESCE(SUM(b.grandTotal), 0) FROM Billing b " +
           "WHERE b.createdAt >= :start AND b.createdAt < :end")
    BigDecimal sumGrandTotalBetween(
        @Param("start") LocalDateTime start,
        @Param("end")   LocalDateTime end
    );
}