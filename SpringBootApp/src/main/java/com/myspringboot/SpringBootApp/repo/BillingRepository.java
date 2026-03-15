package com.myspringboot.SpringBootApp.repo;

import com.myspringboot.SpringBootApp.dto.SalesAnalyticsResult;
import com.myspringboot.SpringBootApp.model.Billing;
import com.myspringboot.SpringBootApp.model.Billing.BillingStatus;
import com.myspringboot.SpringBootApp.model.PaymentType;
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

    // ─────────────────────────────────────────────────────────────────────────
    // CORE / EXISTING QUERIES
    // ─────────────────────────────────────────────────────────────────────────

    List<Billing> findByPharmacyIdOrderByCreatedAtDesc(Long pharmacyId);

    org.springframework.data.domain.Page<Billing> findByPharmacyIdOrderByCreatedAtDesc(Long pharmacyId, org.springframework.data.domain.Pageable pageable);

    /**
     * Paginated bills within a date range — used by the month/year filter on bill history.
     * Benefits from the idx_billing_pharmacy_created composite index.
     */
    org.springframework.data.domain.Page<Billing> findByPharmacyIdAndCreatedAtBetweenOrderByCreatedAtDesc(
            Long pharmacyId,
            LocalDateTime start,
            LocalDateTime end,
            org.springframework.data.domain.Pageable pageable);

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
    // CREDIT BILLING QUERIES
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * All credit bills for a pharmacy ordered newest first.
     */
    List<Billing> findByPharmacyIdAndPaymentTypeOrderByCreatedAtDesc(
            Long        pharmacyId,
            PaymentType paymentType);

    /**
     * Bills matching any of the supplied statuses (used for pending/partial credit lists).
     */
    List<Billing> findByPharmacyIdAndStatusInOrderByCreatedAtDesc(
            Long               pharmacyId,
            List<BillingStatus> statuses);

    /**
     * Total face-value of all credit issued by a pharmacy.
     */
    @Query("""
        SELECT COALESCE(SUM(b.creditAmount), 0)
        FROM   Billing b
        WHERE  b.pharmacy.id  = :pharmacyId
          AND  b.paymentType  = 'CREDIT'
        """)
    BigDecimal sumCreditAmountByPharmacyId(@Param("pharmacyId") Long pharmacyId);

    /**
     * Total cash actually collected against credit bills.
     */
    @Query("""
        SELECT COALESCE(SUM(b.paidAmount), 0)
        FROM   Billing b
        WHERE  b.pharmacy.id  = :pharmacyId
          AND  b.paymentType  = 'CREDIT'
        """)
    BigDecimal sumPaidAmountByPharmacyId(@Param("pharmacyId") Long pharmacyId);

    /**
     * Total outstanding balance still owed across all credit bills.
     */
    @Query("""
        SELECT COALESCE(SUM(b.balanceDue), 0)
        FROM   Billing b
        WHERE  b.pharmacy.id  = :pharmacyId
          AND  b.paymentType  = 'CREDIT'
        """)
    BigDecimal sumBalanceDueByPharmacyId(@Param("pharmacyId") Long pharmacyId);

    /**
     * Count of all credit bills ever created for the pharmacy.
     */
    @Query("""
        SELECT COUNT(b)
        FROM   Billing b
        WHERE  b.pharmacy.id  = :pharmacyId
          AND  b.paymentType  = 'CREDIT'
        """)
    long countCreditBillsByPharmacyId(@Param("pharmacyId") Long pharmacyId);

    /**
     * Count of credit bills still carrying an outstanding balance
     * (CREDIT_PENDING or CREDIT_PARTIAL).
     */
    @Query("""
        SELECT COUNT(b)
        FROM   Billing b
        WHERE  b.pharmacy.id = :pharmacyId
          AND  b.status IN ('CREDIT_PENDING', 'CREDIT_PARTIAL')
        """)
    long countPendingCreditBillsByPharmacyId(@Param("pharmacyId") Long pharmacyId);

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
            FUNCTION('DATE', b.createdAt)   AS saleDay,
            SUM(bi.unitPrice * bi.quantity) AS revenue
        FROM   Billing b
        JOIN   b.items bi
        WHERE  b.pharmacy.id = :pharmacyId
          AND  b.createdAt  >= :start
          AND  b.createdAt  <  :end
        GROUP  BY FUNCTION('DATE', b.createdAt)
        ORDER  BY saleDay ASC
        """)
    List<Object[]> findDailyRevenueBetween(
            @Param("pharmacyId") Long          pharmacyId,
            @Param("start")      LocalDateTime start,
            @Param("end")        LocalDateTime end);

    // ─────────────────────────────────────────────────────────────────────────
    // MONTHLY SALES TREND
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
    


    
    
    
    @Query("""
    	    SELECT b FROM Billing b 
    	    LEFT JOIN FETCH b.createdBy 
    	    WHERE b.id = :id AND b.pharmacy.id = :pharmacyId
    	""")
    	Optional<Billing> findByIdWithDetails(@Param("id") Long id, @Param("pharmacyId") Long pharmacyId);
    
 // Today's bills query
    List<Billing> findByPharmacyIdAndCreatedAtBetweenOrderByCreatedAtDesc(
        Long pharmacyId, LocalDateTime start, LocalDateTime end);
    
    
    List<Billing> findTop5ByPharmacyIdOrderByCreatedAtDesc(Long pharmacyId);
    
}


