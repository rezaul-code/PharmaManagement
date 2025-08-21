package com.myspringboot.SpringBootApp.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "billing")
public class Billing {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String customerName;

    private LocalDateTime billingDate = LocalDateTime.now();

    private Double totalAmount = 0.0;

    @OneToMany(mappedBy = "billing", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<BillingItem> items = new ArrayList<>();

    // helpers
    public void addItem(BillingItem item) {
        this.items.add(item);
        item.setBilling(this);
    }

    // getters/setters
    public Long getId() { return id; }
    public String getCustomerName() { return customerName; }
    public void setCustomerName(String customerName) { this.customerName = customerName; }
    public LocalDateTime getBillingDate() { return billingDate; }
    public void setBillingDate(LocalDateTime billingDate) { this.billingDate = billingDate; }
    public Double getTotalAmount() { return totalAmount; }
    public void setTotalAmount(Double totalAmount) { this.totalAmount = totalAmount; }
    public List<BillingItem> getItems() { return items; }
    public void setItems(List<BillingItem> items) { this.items = items; }
}
