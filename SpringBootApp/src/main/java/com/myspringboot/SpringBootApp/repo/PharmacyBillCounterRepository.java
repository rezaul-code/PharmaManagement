package com.myspringboot.SpringBootApp.repo;

import com.myspringboot.SpringBootApp.model.PharmacyBillCounter;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface PharmacyBillCounterRepository extends JpaRepository<PharmacyBillCounter, Long> {

    /**
     * Fetches the counter row for a pharmacy with a PESSIMISTIC_WRITE lock.
     * Call this inside a @Transactional method to guarantee that concurrent
     * bill-creation transactions queue up and never read the same sequence value.
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT c FROM PharmacyBillCounter c WHERE c.pharmacyId = :pharmacyId")
    Optional<PharmacyBillCounter> findByPharmacyIdForUpdate(@Param("pharmacyId") Long pharmacyId);
}
