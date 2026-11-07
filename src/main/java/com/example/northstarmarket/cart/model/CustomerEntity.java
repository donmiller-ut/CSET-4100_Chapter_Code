package com.example.northstarmarket.cart.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "customer", schema = "northstar")
public class CustomerEntity {

	@Id
	@Column(name = "customer_id")
	private Long id;

	@Column(nullable = false, length = 254)
	private String email;

	@Column(name = "display_name", nullable = false, length = 120)
	private String displayName;

	protected CustomerEntity() {
	}

	public Long getId() {
		return id;
	}
}
