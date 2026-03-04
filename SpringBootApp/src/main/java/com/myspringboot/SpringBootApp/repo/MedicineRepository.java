package com.myspringboot.SpringBootApp.repo;

import com.myspringboot.SpringBootApp.model.Medicine;
import com.myspringboot.SpringBootApp.model.MedicineType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface MedicineRepository extends JpaRepository<Medicine, Long> {

    // ── Autocomplete search (BillingController) ──────────────────────
    List<Medicine> findByNameContainingIgnoreCase(String name);

    // ── Advanced multi-field search (MedicineController /show_medicine)
    /**
     * Resolves:  medicineRepository.searchMedicines(id, name, description, type)
     *
     * Every parameter is optional — pass null to skip that filter.
     * Works with your existing show_medicine page's @RequestParam filters.
     */
    @Query("""
        SELECT m FROM Medicine m
        WHERE (:id          IS NULL OR m.id           = :id)
          AND (:name        IS NULL OR LOWER(m.name)
                                       LIKE LOWER(CONCAT('%', :name, '%')))
          AND (:description IS NULL OR LOWER(m.description)
                                       LIKE LOWER(CONCAT('%', :description, '%')))
          AND (:type        IS NULL OR m.type = :type)
        ORDER BY m.name ASC
        """)
    List<Medicine> searchMedicines(
            @Param("id")          Long         id,
            @Param("name")        String       name,
            @Param("description") String       description,
            @Param("type")        MedicineType type
    );

    // ── Stock / dashboard queries ─────────────────────────────────────
    List<Medicine> findByStockQuantityLessThanEqual(int threshold);

    long countByStockQuantityLessThanEqual(int threshold);
}