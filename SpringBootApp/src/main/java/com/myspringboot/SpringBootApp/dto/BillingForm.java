package com.myspringboot.SpringBootApp.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotEmpty;
import java.util.ArrayList;
import java.util.List;

import com.myspringboot.SpringBootApp.model.BillingItemForm;

public class BillingForm {

    private String customerName; // optional

    @Valid
    @NotEmpty(message = "Add at least one item")
    private List<BillingItemForm> items = new ArrayList<>();

	public String getCustomerName() {
		return customerName;
	}

	public void setCustomerName(String customerName) {
		this.customerName = customerName;
	}

	public List<BillingItemForm> getItems() {
		return items;
	}

	public void setItems(List<BillingItemForm> items) {
		this.items = items;
	}

   
}
