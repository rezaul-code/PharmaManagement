package com.myspringboot.SpringBootApp.repo;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.myspringboot.SpringBootApp.model.Medicine;

public interface MedicineRepository extends JpaRepository<Medicine, Long>{

	@Query("""
	        SELECT m FROM Medicine m 
	        WHERE (:id IS NULL OR m.id = :id)
	          AND (:name IS NULL OR LOWER(m.name) LIKE LOWER(CONCAT('%', :name, '%')))
	          AND (:description IS NULL OR LOWER(m.description) LIKE LOWER(CONCAT('%', :description, '%')))
	          AND (:type IS NULL OR m.medicineType = :type)
	    """)
	    List<Medicine> searchMedicines(
	            @Param("id") Long id,
	            @Param("name") String name,
	            @Param("description") String description,
	            @Param("type") com.myspringboot.SpringBootApp.model.MedicineType type
	    );
	
}
