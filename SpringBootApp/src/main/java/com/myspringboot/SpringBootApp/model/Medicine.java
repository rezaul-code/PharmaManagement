package com.myspringboot.SpringBootApp.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import jakarta.persistence.Transient;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;

@Entity
@Table(
    name = "medicines",
    indexes = {
        @Index(name = "idx_medicine_pharmacy", columnList = "pharmacy_id")
    }
)
public class Medicine {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // ── Medicine Code (unique, auto-generated) ───────────────────────────
    @Column(name = "medicine_code", unique = true, length = 20)
    private String medicineCode;

    // ── Core fields ──────────────────────────────────────────────────────
    @Column(nullable = false)
    private String name;

    private String manufacturer;
    private String batchNo;
    
    @Transient
    public Long getDaysUntilExpiry() {
        if (expiryDate == null) return null;
        return ChronoUnit.DAYS.between(LocalDate.now(), expiryDate);
    }

    // ── Pricing ──────────────────────────────────────────────────────────

    /**
     * purchasePrice — what the pharmacy pays the supplier.
     * Nullable so existing rows are not broken before migration.
     */
    @Column(name = "purchase_price", precision = 10, scale = 2)
    private BigDecimal purchasePrice;

    /**
     * price / sellingPrice — what the pharmacy charges customers.
     * Kept as `price` column in DB for full backward compatibility
     * with billing, stock, and all existing queries.
     */
    @Column(precision = 10, scale = 2)
    private BigDecimal price;          // sellingPrice stored here

    @Column(name = "gst_percentage", precision = 5, scale = 2)
    private BigDecimal gstPercentage = BigDecimal.ZERO;

    // ── Stock ────────────────────────────────────────────────────────────
    @Column(name = "stock_quantity")
    private Integer stockQuantity = 0;

    // ── Classification ───────────────────────────────────────────────────
    @Enumerated(EnumType.STRING)
    private MedicineType type;

    @Column(name = "expiry_date")
    private LocalDate expiryDate;

    private String description;

    // ── Tenant ───────────────────────────────────────────────────────────
    @JsonIgnore
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "pharmacy_id")
    private Pharmacy pharmacy;

    // ── Constructors ─────────────────────────────────────────────────────
    public Medicine() {}

    public Medicine(String name, BigDecimal price, BigDecimal gstPercentage,
                    Integer stockQuantity) {
        this.name          = name;
        this.price         = price;
        this.gstPercentage = gstPercentage;
        this.stockQuantity = stockQuantity;
    }

    // ── Computed / transient helpers ─────────────────────────────────────

    /**
     * Profit per unit = sellingPrice − purchasePrice.
     * Returns null if either price is missing (avoids NPE in templates).
     */
    @Transient
    public BigDecimal getProfit() {
        if (price == null || purchasePrice == null) return null;
        return price.subtract(purchasePrice).setScale(2, RoundingMode.HALF_UP);
    }

    /**
     * Profit percentage = (profit / purchasePrice) × 100.
     * Returns null if purchasePrice is null or zero.
     */
    @Transient
    public BigDecimal getProfitPercentage() {
        if (price == null || purchasePrice == null
                || purchasePrice.compareTo(BigDecimal.ZERO) == 0) return null;
        return price.subtract(purchasePrice)
                    .divide(purchasePrice, 4, RoundingMode.HALF_UP)
                    .multiply(BigDecimal.valueOf(100))
                    .setScale(2, RoundingMode.HALF_UP);
    }

    /** Convenience alias so templates can use medicine.sellingPrice */
    @Transient
    public BigDecimal getSellingPrice() {
        return price;
    }

    public void setSellingPrice(BigDecimal sellingPrice) {
        this.price = sellingPrice;
    }

    /** True when profit margin is negative — used for UI warnings. */
    @Transient
    public boolean isNegativeMargin() {
        BigDecimal p = getProfit();
        return p != null && p.compareTo(BigDecimal.ZERO) < 0;
    }

    @Transient
    public boolean isLowStock() {
        return stockQuantity != null && stockQuantity <= 10;
    }

    // ── Getters & Setters ────────────────────────────────────────────────

    public Long getId()                        { return id; }
    public void setId(Long id)                 { this.id = id; }

    public String getMedicineCode()            { return medicineCode; }
    public void setMedicineCode(String c)      { this.medicineCode = c; }

    public String getName()                    { return name; }
    public void setName(String name)           { this.name = name; }

    public String getManufacturer()            { return manufacturer; }
    public void setManufacturer(String m)      { this.manufacturer = m; }

    public String getBatchNo()                 { return batchNo; }
    public void setBatchNo(String b)           { this.batchNo = b; }

    public BigDecimal getPurchasePrice()       { return purchasePrice; }
    public void setPurchasePrice(BigDecimal p) { this.purchasePrice = p; }

    /** getPrice() kept for 100% backward compatibility with billing. */
    public BigDecimal getPrice()               { return price; }
    public void setPrice(BigDecimal price)     { this.price = price; }

    public BigDecimal getGstPercentage()       { return gstPercentage; }
    public void setGstPercentage(BigDecimal g) { this.gstPercentage = g; }

    public Integer getStockQuantity()          { return stockQuantity; }
    public void setStockQuantity(Integer s)    { this.stockQuantity = s; }

    public MedicineType getType()              { return type; }
    public void setType(MedicineType type)     { this.type = type; }

    public LocalDate getExpiryDate()           { return expiryDate; }
    public void setExpiryDate(LocalDate d)     { this.expiryDate = d; }

    public String getDescription()             { return description; }
    public void setDescription(String d)       { this.description = d; }

    public Pharmacy getPharmacy()              { return pharmacy; }
    public void setPharmacy(Pharmacy p)        { this.pharmacy = p; }
}