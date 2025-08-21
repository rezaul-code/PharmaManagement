package com.myspringboot.SpringBootApp.model;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

public class BillingItemForm {
    @NotNull
    private Long medicineId;

    @NotNull
    @Min(1)
    private Integer quantity;

	public Long getMedicineId() {
		return medicineId;
	}

	public void setMedicineId(Long medicineId) {
		this.medicineId = medicineId;
	}

	public Integer getQuantity() {
		return quantity;
	}

	public void setQuantity(Integer quantity) {
		this.quantity = quantity;
	}

    // getters/setters
}
