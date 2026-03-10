package com.myspringboot.SpringBootApp.repo;

import com.myspringboot.SpringBootApp.model.Medicine;
import com.myspringboot.SpringBootApp.model.MedicineType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface MedicineRepository extends JpaRepository<Medicine, Long> {

    // ── existing methods unchanged ──────────────────────────────────────

    List<Medicine> findByPharmacyId(Long pharmacyId);
    Optional<Medicine> findByIdAndPharmacyId(Long id, Long pharmacyId);
    List<Medicine> findByNameContainingIgnoreCaseAndPharmacyId(String name, Long pharmacyId);

    @Query("""
        SELECT m FROM Medicine m
        WHERE m.pharmacy.id = :pharmacyId
          AND (:id          IS NULL OR m.id           = :id)
          AND (:name        IS NULL OR LOWER(m.name)
                                       LIKE LOWER(CONCAT('%', :name, '%')))
          AND (:description IS NULL OR LOWER(m.description)
                                       LIKE LOWER(CONCAT('%', :description, '%')))
          AND (:type        IS NULL OR m.type = :type)
        ORDER BY m.name ASC
        """)
    List<Medicine> searchMedicines(
            @Param("pharmacyId")  Long         pharmacyId,
            @Param("id")          Long         id,
            @Param("name")        String       name,
            @Param("description") String       description,
            @Param("type")        MedicineType type
    );

    List<Medicine> findByStockQuantityLessThanEqualAndPharmacyId(int threshold, Long pharmacyId);
    long countByStockQuantityLessThanEqualAndPharmacyId(int threshold, Long pharmacyId);
    long countByPharmacyId(Long pharmacyId);
    List<Medicine> findByPharmacyIsNull();

    @Query("""
        SELECT COUNT(m) FROM Medicine m
        WHERE m.pharmacy.id   = :pharmacyId
          AND m.expiryDate   IS NOT NULL
          AND m.expiryDate   >= :today
          AND m.expiryDate   <= :threshold
        """)
    long countExpiringBetween(
            @Param("pharmacyId") Long      pharmacyId,
            @Param("today")      LocalDate today,
            @Param("threshold")  LocalDate threshold
    );

    @Query("""
        SELECT m FROM Medicine m
        WHERE m.pharmacy.id   = :pharmacyId
          AND m.expiryDate   IS NOT NULL
          AND m.expiryDate   >= :today
          AND m.expiryDate   <= :threshold
        ORDER BY m.expiryDate ASC
        """)
    List<Medicine> findExpiringBetween(
            @Param("pharmacyId") Long      pharmacyId,
            @Param("today")      LocalDate today,
            @Param("threshold")  LocalDate threshold
    );

    // ── NEW: medicine code support ──────────────────────────────────────

    /** Used to check uniqueness before saving. */
    boolean existsByMedicineCodeAndPharmacyId(String medicineCode, Long pharmacyId);

    /** Find by exact code (for search/barcode lookup). */
    Optional<Medicine> findByMedicineCodeAndPharmacyId(String medicineCode, Long pharmacyId);

    /**
     * Returns the highest numeric suffix already used for this pharmacy,
     * e.g. if MED-042 exists → returns 42.
     * Uses a regex-safe approach: extracts the trailing digits.
     */
    @Query("""
        SELECT MAX(CAST(SUBSTRING(m.medicineCode, 5) AS int))
        FROM Medicine m
        WHERE m.pharmacy.id = :pharmacyId
          AND m.medicineCode IS NOT NULL
          AND m.medicineCode LIKE 'MED-%'
        """)
    Optional<Integer> findMaxCodeSequence(@Param("pharmacyId") Long pharmacyId);

    /**
     * Extended search that also matches medicine code.
     * Drop-in replacement for the existing searchMedicines query.
     */
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
}