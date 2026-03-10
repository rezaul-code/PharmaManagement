package com.myspringboot.SpringBootApp.repo;

import com.myspringboot.SpringBootApp.model.BillingItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Repository for BillingItem.
 *
 * NOTE: All analytics queries (monthly trend, daily trend, top medicines)
 * live in BillingRepository — NOT here — because they aggregate across
 * Billing (parent) joined to BillingItem (child).
 */
@Repository
public interface BillingItemRepository extends JpaRepository<BillingItem, Long> {

    List<BillingItem> findByPharmacyId(Long pharmacyId);

    Optional<BillingItem> findByIdAndPharmacyId(Long id, Long pharmacyId);

    List<BillingItem> findByPharmacyIsNull();
}