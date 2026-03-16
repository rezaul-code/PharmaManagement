package com.myspringboot.SpringBootApp.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.FetchType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.EnumType;
import java.time.LocalDate;

/**
 * Represents a single pharmacy tenant.
 * All medicines, bills, and users are scoped to a Pharmacy.
 */
@Entity
@Table(name = "pharmacies")
public class Pharmacy {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String name;

    private String address;
    private String phone;
    private String email;

    /** Licence / registration number — useful for invoice headers */
    @Column(name = "license_number")
    private String licenseNumber;

    /** GST Number — displayed on invoices */
    @Column(name = "gst_number")
    private String gstNumber;

    /** Footer text printed at the bottom of every bill */
    @Column(name = "invoice_footer", length = 500)
    private String invoiceFooter;

    /** Relative path to the uploaded pharmacy logo, e.g. "uploads/logos/1.png" */
    @Column(name = "logo_path", length = 300)
    private String logoPath;

    @Column(name = "tenant_id", unique = true)
    private String tenantId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "subscription_id")
    private Subscription subscription;

    @Column(name = "subscription_end_date")
    private LocalDate subscriptionEndDate;

    @Enumerated(EnumType.STRING)
    @Column(name = "tenant_status")
    private TenantStatus status = TenantStatus.ACTIVE;

    @Column(name = "created_at")
    private java.time.LocalDateTime createdAt;

    @Column(name = "plan_type")
    private String planType;

    public Pharmacy() {}

    public Pharmacy(String name) {
        this.name = name;
    }

    // ── Getters & Setters ────────────────────────────────────────────

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getAddress() { return address; }
    public void setAddress(String address) { this.address = address; }

    public String getPhone() { return phone; }
    public void setPhone(String phone) { this.phone = phone; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public String getLicenseNumber() { return licenseNumber; }
    public void setLicenseNumber(String licenseNumber) { this.licenseNumber = licenseNumber; }

    public String getGstNumber() { return gstNumber; }
    public void setGstNumber(String gstNumber) { this.gstNumber = gstNumber; }

    public String getInvoiceFooter() { return invoiceFooter; }
    public void setInvoiceFooter(String invoiceFooter) { this.invoiceFooter = invoiceFooter; }

    public String getLogoPath() { return logoPath; }
    public void setLogoPath(String logoPath) { this.logoPath = logoPath; }

    public String getTenantId() { return tenantId; }
    public void setTenantId(String tenantId) { this.tenantId = tenantId; }

    public Subscription getSubscription() { return subscription; }
    public void setSubscription(Subscription subscription) { this.subscription = subscription; }

    public LocalDate getSubscriptionEndDate() { return subscriptionEndDate; }
    public void setSubscriptionEndDate(LocalDate subscriptionEndDate) { this.subscriptionEndDate = subscriptionEndDate; }

    public TenantStatus getStatus() { return status; }
    public void setStatus(TenantStatus status) { this.status = status; }

    public java.time.LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(java.time.LocalDateTime createdAt) { this.createdAt = createdAt; }

    public String getPlanType() { return planType; }
    public void setPlanType(String planType) { this.planType = planType; }

    // ── Helper: avatar initial for sidebar ──────────────────────────
    public String getAvatarInitial() {
        return (name != null && !name.isEmpty())
                ? String.valueOf(name.charAt(0)).toUpperCase()
                : "P";
    }
}