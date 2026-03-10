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

/**
 * Repository for Billing.
 *
 * KEY MODEL FACTS (confirmed from source):
 *  - Billing      date field  → createdAt          (NOT billingDate)
 *  - BillingItem  price field → unitPrice           (selling price at time of sale)
 *  - BillingItem  has NO purchasePrice field        (join Medicine for that)
 *  - Medicine     selling price column → price
 *  - Medicine     purchase price column → purchasePrice
 *
 * PROFIT formula:
 *   bi.quantity × (bi.medicine.price − bi.medicine.purchasePrice)
 *   bi.medicine.purchasePrice may be NULL on legacy rows → COALESCE → 0
 */
@Repository
public interface BillingRepository extends JpaRepository<Billing, Long> {

    // ─────────────────────────────────────────────────────────────────────────
    // EXISTING utility queries (unchanged)
    // ─────────────────────────────────────────────────────────────────────────

    List<Billing> findByPharmacyIdOrderByCreatedAtDesc(Long pharmacyId);

    Optional<Billing> findByIdAndPharmacyId(Long id, Long pharmacyId);

    List<Billing> findByPharmacyIsNull();

    long countByPharmacyId(Long pharmacyId);

    @Query("""
        SELECT SUM(b.grandTotal)
        FROM   Billing b
        WHERE  b.pharmacy.id = :pharmacyId
          AND  b.createdAt  >= :start
          AND  b.createdAt  <  :end
        """)
    BigDecimal sumGrandTotalBetween(
            @Param("pharmacyId") Long          pharmacyId,
            @Param("start")      LocalDateTime start,
            @Param("end")        LocalDateTime end);

    // ─────────────────────────────────────────────────────────────────────────
    // SALES ANALYTICS — Top medicines
    //
    // Revenue = SUM(bi.quantity × bi.unitPrice)
    // Profit  = SUM(bi.quantity × (m.price − COALESCE(m.purchasePrice, 0)))
    //
    // bi.medicine may be NULL for manually-typed items → LEFT JOIN + COALESCE
    // ─────────────────────────────────────────────────────────────────────────

    @Query("""
        SELECT new com.myspringboot.SpringBootApp.dto.SalesAnalyticsResult(
            bi.medicineName,
            SUM(CAST(bi.quantity AS long)),
            SUM(bi.unitPrice * bi.quantity),
            SUM(bi.quantity * (COALESCE(m.price, bi.unitPrice)
                               - COALESCE(m.purchasePrice, 0)))
        )
        FROM   Billing b
        JOIN   b.items bi
        LEFT JOIN bi.medicine m
        WHERE  b.pharmacy.id = :pharmacyId
          AND  b.createdAt  >= :start
          AND  b.createdAt  <  :end
        GROUP  BY bi.medicineName
        ORDER  BY SUM(CAST(bi.quantity AS long)) DESC
        """)
    List<SalesAnalyticsResult> findTopByQuantity(
            @Param("pharmacyId") Long          pharmacyId,
            @Param("start")      LocalDateTime start,
            @Param("end")        LocalDateTime end);

    @Query("""
        SELECT new com.myspringboot.SpringBootApp.dto.SalesAnalyticsResult(
            bi.medicineName,
            SUM(CAST(bi.quantity AS long)),
            SUM(bi.unitPrice * bi.quantity),
            SUM(bi.quantity * (COALESCE(m.price, bi.unitPrice)
                               - COALESCE(m.purchasePrice, 0)))
        )
        FROM   Billing b
        JOIN   b.items bi
        LEFT JOIN bi.medicine m
        WHERE  b.pharmacy.id = :pharmacyId
          AND  b.createdAt  >= :start
          AND  b.createdAt  <  :end
        GROUP  BY bi.medicineName
        ORDER  BY SUM(bi.unitPrice * bi.quantity) DESC
        """)
    List<SalesAnalyticsResult> findTopByRevenue(
            @Param("pharmacyId") Long          pharmacyId,
            @Param("start")      LocalDateTime start,
            @Param("end")        LocalDateTime end);

    @Query("""
        SELECT new com.myspringboot.SpringBootApp.dto.SalesAnalyticsResult(
            bi.medicineName,
            SUM(CAST(bi.quantity AS long)),
            SUM(bi.unitPrice * bi.quantity),
            SUM(bi.quantity * (COALESCE(m.price, bi.unitPrice)
                               - COALESCE(m.purchasePrice, 0)))
        )
        FROM   Billing b
        JOIN   b.items bi
        LEFT JOIN bi.medicine m
        WHERE  b.pharmacy.id = :pharmacyId
          AND  b.createdAt  >= :start
          AND  b.createdAt  <  :end
        GROUP  BY bi.medicineName
        ORDER  BY SUM(bi.quantity * (COALESCE(m.price, bi.unitPrice)
                                     - COALESCE(m.purchasePrice, 0))) DESC
        """)
    List<SalesAnalyticsResult> findTopByProfit(
            @Param("pharmacyId") Long          pharmacyId,
            @Param("start")      LocalDateTime start,
            @Param("end")        LocalDateTime end);

    // ─────────────────────────────────────────────────────────────────────────
    // DAILY REVENUE TREND
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Returns one row per day: [ dateString, revenue ]
     * Uses b.createdAt (confirmed field name on Billing entity).
     */
    @Query("""
        SELECT
            CAST(b.createdAt AS date)          AS saleDay,
            SUM(bi.unitPrice * bi.quantity)    AS revenue
        FROM   Billing b
        JOIN   b.items bi
        WHERE  b.pharmacy.id = :pharmacyId
          AND  b.createdAt  >= :start
          AND  b.createdAt  <  :end
        GROUP  BY CAST(b.createdAt AS date)
        ORDER  BY saleDay ASC
        """)
    List<Object[]> findDailyRevenueBetween(
            @Param("pharmacyId") Long          pharmacyId,
            @Param("start")      LocalDateTime start,
            @Param("end")        LocalDateTime end);

    // ─────────────────────────────────────────────────────────────────────────
    // MONTHLY SALES TREND  (NEW)
    //
    // Groups by YEAR + MONTH of b.createdAt.
    // Profit joins Medicine for purchasePrice; COALESCE handles NULL rows.
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Returns one row per calendar month ordered oldest → newest.
     * <p>
     * Columns (Object[]):
     * <ol>
     *   <li>[0] yr      – 4-digit year  (Integer)</li>
     *   <li>[1] mo      – month 1–12    (Integer)</li>
     *   <li>[2] revenue – SUM(qty × unitPrice)                       (BigDecimal)</li>
     *   <li>[3] profit  – SUM(qty × (m.price − m.purchasePrice))     (BigDecimal)</li>
     *   <li>[4] units   – SUM(qty)                                   (Long)</li>
     * </ol>
     */
    @Query("""
        SELECT
            FUNCTION('YEAR',  b.createdAt)                                          AS yr,
            FUNCTION('MONTH', b.createdAt)                                          AS mo,
            SUM(bi.unitPrice * bi.quantity)                                         AS revenue,
            SUM(bi.quantity * (COALESCE(m.price, bi.unitPrice)
                               - COALESCE(m.purchasePrice, 0)))                     AS profit,
            SUM(CAST(bi.quantity AS long))                                          AS units
        FROM   Billing b
        JOIN   b.items bi
        LEFT JOIN bi.medicine m
        WHERE  b.pharmacy.id = :pharmacyId
          AND  b.createdAt  >= :start
          AND  b.createdAt  <  :end
        GROUP  BY
            FUNCTION('YEAR',  b.createdAt),
            FUNCTION('MONTH', b.createdAt)
        ORDER  BY yr ASC, mo ASC
        """)
    List<Object[]> findMonthlyRevenue(
            @Param("pharmacyId") Long          pharmacyId,
            @Param("start")      LocalDateTime start,
            @Param("end")        LocalDateTime end);
}