package com.myspringboot.SpringBootApp.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

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

    // ── Helper: avatar initial for sidebar ──────────────────────────
    public String getAvatarInitial() {
        return (name != null && !name.isEmpty())
                ? String.valueOf(name.charAt(0)).toUpperCase()
                : "P";
    }
}