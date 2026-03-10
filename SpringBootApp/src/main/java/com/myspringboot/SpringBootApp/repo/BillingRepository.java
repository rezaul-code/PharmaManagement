package com.myspringboot.SpringBootApp.repo;

import com.myspringboot.SpringBootApp.dto.SalesAnalyticsResult;
import com.myspringboot.SpringBootApp.model.Billing;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface BillingRepository extends JpaRepository<Billing, Long> {

    // ── Existing queries — untouched ─────────────────────────────────────

    List<Billing>     findByPharmacyIdOrderByCreatedAtDesc(Long pharmacyId);
    Optional<Billing> findByIdAndPharmacyId(Long id, Long pharmacyId);
    long              countByPharmacyId(Long pharmacyId);
    List<Billing>     findByPharmacyIsNull();

    @Query("""
        SELECT COALESCE(SUM(b.grandTotal), 0) FROM Billing b
        WHERE b.pharmacy.id = :pharmacyId
          AND b.createdAt  >= :start
          AND b.createdAt   < :end
        """)
    BigDecimal sumGrandTotalBetween(
            @Param("pharmacyId") Long          pharmacyId,
            @Param("start")      LocalDateTime start,
            @Param("end")        LocalDateTime end
    );

    // ── Sales Analytics queries ───────────────────────────────────────────
    // NOTE: BillingItem has no createdAt field — we join to bi.billing.createdAt
    //       for all date filtering. pharmacy is also filtered via bi.billing.pharmacy.id.

    /**
     * Top medicines by total quantity sold (descending).
     */
    @Query("""
        SELECT new com.myspringboot.SpringBootApp.dto.SalesAnalyticsResult(
            bi.medicine.id,
            bi.medicineName,
            SUM(bi.quantity),
            SUM(bi.unitPrice * bi.quantity),
            SUM(bi.unitPrice * bi.quantity)
              - SUM(COALESCE(bi.medicine.purchasePrice, 0) * bi.quantity)
        )
        FROM BillingItem bi
        WHERE bi.billing.pharmacy.id  = :pharmacyId
          AND bi.medicine             IS NOT NULL
          AND bi.billing.createdAt   >= :start
          AND bi.billing.createdAt    < :end
        GROUP BY bi.medicine.id, bi.medicineName
        ORDER BY SUM(bi.quantity) DESC
        """)
    List<SalesAnalyticsResult> findTopByQuantity(
            @Param("pharmacyId") Long          pharmacyId,
            @Param("start")      LocalDateTime start,
            @Param("end")        LocalDateTime end
    );

    /**
     * Top medicines by total revenue (unitPrice × quantity) descending.
     */
    @Query("""
        SELECT new com.myspringboot.SpringBootApp.dto.SalesAnalyticsResult(
            bi.medicine.id,
            bi.medicineName,
            SUM(bi.quantity),
            SUM(bi.unitPrice * bi.quantity),
            SUM(bi.unitPrice * bi.quantity)
              - SUM(COALESCE(bi.medicine.purchasePrice, 0) * bi.quantity)
        )
        FROM BillingItem bi
        WHERE bi.billing.pharmacy.id  = :pharmacyId
          AND bi.medicine             IS NOT NULL
          AND bi.billing.createdAt   >= :start
          AND bi.billing.createdAt    < :end
        GROUP BY bi.medicine.id, bi.medicineName
        ORDER BY SUM(bi.unitPrice * bi.quantity) DESC
        """)
    List<SalesAnalyticsResult> findTopByRevenue(
            @Param("pharmacyId") Long          pharmacyId,
            @Param("start")      LocalDateTime start,
            @Param("end")        LocalDateTime end
    );

    /**
     * Top medicines by total profit
     * ((unitPrice − purchasePrice) × quantity) descending.
     */
    @Query("""
        SELECT new com.myspringboot.SpringBootApp.dto.SalesAnalyticsResult(
            bi.medicine.id,
            bi.medicineName,
            SUM(bi.quantity),
            SUM(bi.unitPrice * bi.quantity),
            SUM(bi.unitPrice * bi.quantity)
              - SUM(COALESCE(bi.medicine.purchasePrice, 0) * bi.quantity)
        )
        FROM BillingItem bi
        WHERE bi.billing.pharmacy.id  = :pharmacyId
          AND bi.medicine             IS NOT NULL
          AND bi.billing.createdAt   >= :start
          AND bi.billing.createdAt    < :end
        GROUP BY bi.medicine.id, bi.medicineName
        ORDER BY (
            SUM(bi.unitPrice * bi.quantity)
              - SUM(COALESCE(bi.medicine.purchasePrice, 0) * bi.quantity)
        ) DESC
        """)
    List<SalesAnalyticsResult> findTopByProfit(
            @Param("pharmacyId") Long          pharmacyId,
            @Param("start")      LocalDateTime start,
            @Param("end")        LocalDateTime end
    );

    /**
     * Daily revenue totals for the trend line chart.
     * Groups by date using the parent Billing.createdAt.
     */
    @Query("""
        SELECT FUNCTION('DATE', b.createdAt),
               COALESCE(SUM(b.grandTotal), 0)
        FROM Billing b
        WHERE b.pharmacy.id = :pharmacyId
          AND b.createdAt  >= :start
          AND b.createdAt   < :end
        GROUP BY FUNCTION('DATE', b.createdAt)
        ORDER BY FUNCTION('DATE', b.createdAt) ASC
        """)
    List<Object[]> findDailyRevenueBetween(
            @Param("pharmacyId") Long          pharmacyId,
            @Param("start")      LocalDateTime start,
            @Param("end")        LocalDateTime end
    );
}