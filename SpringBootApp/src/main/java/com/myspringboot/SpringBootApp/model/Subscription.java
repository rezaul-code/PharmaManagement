package com.myspringboot.SpringBootApp.model;

import jakarta.persistence.*;
import java.math.BigDecimal;

@Entity
@Table(name = "subscriptions")
public class Subscription {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String name; // e.g. 'Starter', 'Pro', 'Enterprise'

    @Column(nullable = false)
    private BigDecimal price;

    @Column(name = "duration_months", nullable = false)
    private Integer durationMonths;

    public Subscription() {}

    public Subscription(String name, BigDecimal price, Integer durationMonths) {
        this.name = name;
        this.price = price;
        this.durationMonths = durationMonths;
    }

    // Getters and Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    
    public BigDecimal getPrice() { return price; }
    public void setPrice(BigDecimal price) { this.price = price; }
    
    public Integer getDurationMonths() { return durationMonths; }
    public void setDurationMonths(Integer durationMonths) { this.durationMonths = durationMonths; }
}
