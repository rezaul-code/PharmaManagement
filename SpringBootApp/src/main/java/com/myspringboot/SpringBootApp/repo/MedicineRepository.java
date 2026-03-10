package com.myspringboot.SpringBootApp.repo;

import com.myspringboot.SpringBootApp.model.Medicine;
import com.myspringboot.SpringBootApp.model.MedicineType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface MedicineRepository extends JpaRepository<Medicine, Long> {

    // ── Tenant lookups ───────────────────────────────────────────────────
    List<Medicine>     findByPharmacyId(Long pharmacyId);
    Optional<Medicine> findByIdAndPharmacyId(Long id, Long pharmacyId);
    List<Medicine>     findByNameContainingIgnoreCaseAndPharmacyId(String name, Long pharmacyId);

    // ── Medicine code ────────────────────────────────────────────────────
    boolean            existsByMedicineCodeAndPharmacyId(String code, Long pharmacyId);
    Optional<Medicine> findByMedicineCodeAndPharmacyId(String code, Long pharmacyId);

    @Query("""
        SELECT MAX(CAST(SUBSTRING(m.medicineCode, 5) AS int))
        FROM Medicine m
        WHERE m.pharmacy.id = :pharmacyId
          AND m.medicineCode IS NOT NULL
          AND m.medicineCode LIKE 'MED-%'
        """)
    Optional<Integer> findMaxCodeSequence(@Param("pharmacyId") Long pharmacyId);

    // ── Search (with optional medicine-code filter) ──────────────────────
    @Query("""
        SELECT m FROM Medicine m
        WHERE m.pharmacy.id = :pharmacyId
          AND (:id           IS NULL OR m.id           = :id)
          AND (:name         IS NULL OR LOWER(m.name)
                                        LIKE LOWER(CONCAT('%', :name, '%')))
          AND (:description  IS NULL OR LOWER(m.description)
                                        LIKE LOWER(CONCAT('%', :description, '%')))
          AND (:type         IS NULL OR m.type          = :type)
          AND (:medicineCode IS NULL OR LOWER(m.medicineCode)
                                        LIKE LOWER(CONCAT('%', :medicineCode, '%')))
        ORDER BY m.name ASC
        """)
    List<Medicine> searchMedicinesWithCode(
            @Param("pharmacyId")   Long         pharmacyId,
            @Param("id")           Long         id,
            @Param("name")         String       name,
            @Param("description")  String       description,
            @Param("type")         MedicineType type,
            @Param("medicineCode") String       medicineCode
    );

    // ── Stock ────────────────────────────────────────────────────────────
    List<Medicine> findByStockQuantityLessThanEqualAndPharmacyId(int threshold, Long pharmacyId);
    long           countByStockQuantityLessThanEqualAndPharmacyId(int threshold, Long pharmacyId);
    long           countByPharmacyId(Long pharmacyId);
    List<Medicine> findByPharmacyIsNull();

    // ── Expiry ───────────────────────────────────────────────────────────
    @Query("""
        SELECT COUNT(m) FROM Medicine m
        WHERE m.pharmacy.id = :pharmacyId
          AND m.expiryDate IS NOT NULL
          AND m.expiryDate >= :today
          AND m.expiryDate <= :threshold
        """)
    long countExpiringBetween(
            @Param("pharmacyId") Long      pharmacyId,
            @Param("today")      LocalDate today,
            @Param("threshold")  LocalDate threshold
    );

    @Query("""
        SELECT m FROM Medicine m
        WHERE m.pharmacy.id = :pharmacyId
          AND m.expiryDate IS NOT NULL
          AND m.expiryDate >= :today
          AND m.expiryDate <= :threshold
        ORDER BY m.expiryDate ASC
        """)
    List<Medicine> findExpiringBetween(
            @Param("pharmacyId") Long      pharmacyId,
            @Param("today")      LocalDate today,
            @Param("threshold")  LocalDate threshold
    );

    // ── Profit analytics ─────────────────────────────────────────────────

    /**
     * Total inventory value at purchase price for a pharmacy.
     * Used in profit dashboard widgets.
     */
    @Query("""
        SELECT COALESCE(SUM(m.purchasePrice * m.stockQuantity), 0)
        FROM Medicine m
        WHERE m.pharmacy.id    = :pharmacyId
          AND m.purchasePrice IS NOT NULL
        """)
    BigDecimal sumInventoryValueAtCost(@Param("pharmacyId") Long pharmacyId);

    /**
     * Total inventory value at selling price for a pharmacy.
     */
    @Query("""
        SELECT COALESCE(SUM(m.price * m.stockQuantity), 0)
        FROM Medicine m
        WHERE m.pharmacy.id = :pharmacyId
          AND m.price       IS NOT NULL
        """)
    BigDecimal sumInventoryValueAtSelling(@Param("pharmacyId") Long pharmacyId);

    /**
     * Medicines with highest profit margin — useful for top-margin widget.
     */
    @Query("""
        SELECT m FROM Medicine m
        WHERE m.pharmacy.id    = :pharmacyId
          AND m.purchasePrice IS NOT NULL
          AND m.purchasePrice  > 0
          AND m.price         IS NOT NULL
        ORDER BY (m.price - m.purchasePrice) DESC
        """)
    List<Medicine> findTopProfitMedicines(@Param("pharmacyId") Long pharmacyId);

    /**
     * Medicines with negative margin (selling below cost) — alert widget.
     */
    @Query("""
        SELECT m FROM Medicine m
        WHERE m.pharmacy.id    = :pharmacyId
          AND m.purchasePrice IS NOT NULL
          AND m.price         IS NOT NULL
          AND m.price          < m.purchasePrice
        ORDER BY (m.price - m.purchasePrice) ASC
        """)
    List<Medicine> findNegativeMarginMedicines(@Param("pharmacyId") Long pharmacyId);
}