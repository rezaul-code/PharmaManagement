package com.myspringboot.SpringBootApp.controller.api;

import com.myspringboot.SpringBootApp.Service.MedicineService;
import com.myspringboot.SpringBootApp.model.Medicine;
import com.myspringboot.SpringBootApp.model.MedicineType;
import com.myspringboot.SpringBootApp.security.CustomUserDetails;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/medicine")
public class MedicineRestController {

    @Autowired
    private MedicineService medicineService;

    // Helper method to check role authorization for WRITE operations (OWNER or PHARMACIST)
    private boolean canManageMedicines() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated() || auth instanceof AnonymousAuthenticationToken) {
            return false;
        }
        Object principal = auth.getPrincipal();
        if (principal instanceof CustomUserDetails userDetails) {
            return userDetails.getUser().canManageMedicines();
        }
        return false;
    }

    // ── READ (All roles) ──────────────────────────────────────────────────

    @GetMapping
    public ResponseEntity<Page<Medicine>> getMedicines(
            @RequestParam(required = false) Long id,
            @RequestParam(required = false) String name,
            @RequestParam(required = false) String description,
            @RequestParam(required = false) MedicineType type,
            @RequestParam(required = false) String medicineCode,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "name") String sortField,
            @RequestParam(defaultValue = "asc") String sortDir) {

        Page<Medicine> result = medicineService.searchMedicines(id, name, description, type, medicineCode, page, size, sortField, sortDir);
        return ResponseEntity.ok(result);
    }

    @GetMapping("/{id}")
    public ResponseEntity<?> getMedicineById(@PathVariable Long id) {
        try {
            Medicine medicine = medicineService.getMedicineById(id);
            return ResponseEntity.ok(medicine);
        } catch (IllegalArgumentException e) {
            Map<String, Object> errorBody = new HashMap<>();
            errorBody.put("success", false);
            errorBody.put("message", e.getMessage());
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(errorBody);
        }
    }

    @GetMapping("/search")
    public ResponseEntity<List<Medicine>> searchMedicine(@RequestParam("q") String query) {
        List<Medicine> result = medicineService.searchByName(query);
        return ResponseEntity.ok(result);
    }

    @GetMapping("/low-stock")
    public ResponseEntity<List<Medicine>> getLowStock() {
        List<Medicine> result = medicineService.getLowStockMedicines();
        return ResponseEntity.ok(result);
    }

    @GetMapping("/expiring")
    public ResponseEntity<List<Medicine>> getExpiring() {
        List<Medicine> result = medicineService.getExpiringWithin30Days();
        return ResponseEntity.ok(result);
    }

    @GetMapping("/categories")
    public ResponseEntity<MedicineType[]> getCategories() {
        return ResponseEntity.ok(MedicineType.values());
    }

    // ── WRITE (OWNER + PHARMACIST only) ───────────────────────────────────

    @PostMapping
    public ResponseEntity<?> createMedicine(@RequestBody Medicine medicine) {
        if (!canManageMedicines()) {
            Map<String, Object> errorBody = new HashMap<>();
            errorBody.put("success", false);
            errorBody.put("message", "Access denied: only OWNER or PHARMACIST can manage medicines.");
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(errorBody);
        }
        try {
            Medicine saved = medicineService.saveMedicine(medicine);
            return ResponseEntity.status(HttpStatus.CREATED).body(saved);
        } catch (Exception e) {
            Map<String, Object> errorBody = new HashMap<>();
            errorBody.put("success", false);
            errorBody.put("message", e.getMessage());
            return ResponseEntity.badRequest().body(errorBody);
        }
    }

    @PutMapping("/{id}")
    public ResponseEntity<?> updateMedicine(@PathVariable Long id, @RequestBody Medicine medicine) {
        if (!canManageMedicines()) {
            Map<String, Object> errorBody = new HashMap<>();
            errorBody.put("success", false);
            errorBody.put("message", "Access denied: only OWNER or PHARMACIST can manage medicines.");
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(errorBody);
        }
        try {
            medicine.setId(id);
            Medicine saved = medicineService.saveMedicine(medicine);
            return ResponseEntity.ok(saved);
        } catch (Exception e) {
            Map<String, Object> errorBody = new HashMap<>();
            errorBody.put("success", false);
            errorBody.put("message", e.getMessage());
            return ResponseEntity.badRequest().body(errorBody);
        }
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<?> deleteMedicine(@PathVariable Long id) {
        if (!canManageMedicines()) {
            Map<String, Object> errorBody = new HashMap<>();
            errorBody.put("success", false);
            errorBody.put("message", "Access denied: only OWNER or PHARMACIST can manage medicines.");
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(errorBody);
        }
        try {
            medicineService.deleteMedicine(id);
            Map<String, Object> responseBody = new HashMap<>();
            responseBody.put("success", true);
            responseBody.put("message", "Medicine deleted successfully");
            return ResponseEntity.ok(responseBody);
        } catch (Exception e) {
            Map<String, Object> errorBody = new HashMap<>();
            errorBody.put("success", false);
            errorBody.put("message", e.getMessage());
            return ResponseEntity.badRequest().body(errorBody);
        }
    }
}
