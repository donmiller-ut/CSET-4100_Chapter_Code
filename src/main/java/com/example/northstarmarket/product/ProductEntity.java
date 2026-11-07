package com.example.northstarmarket.product;

import java.math.BigDecimal;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

@Entity
@Table(name = "product", schema = "northstar")
public class ProductEntity {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	@Column(name = "product_id")
	private Long id;

	@Column(nullable = false, length = 120)
	private String name;

	@Column(length = 1000)
	private String description;

	@Column(nullable = false)
	private BigDecimal price;

	@Column(nullable = false)
	private boolean active;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "category_id", nullable = false)
	private CategoryEntity category;

	protected ProductEntity() {
	}

	ProductEntity(String name, String description, BigDecimal price, boolean active, CategoryEntity category) {
		this.name = name;
		this.description = description;
		this.price = price;
		this.active = active;
		this.category = category;
	}

	public Long getId() {
		return id;
	}

	public String getName() {
		return name;
	}

	String getDescription() {
		return description;
	}

	BigDecimal getPrice() {
		return price;
	}

	public boolean isActive() {
		return active;
	}

	CategoryEntity getCategory() {
		return category;
	}

	void replace(String name, String description, BigDecimal price, boolean active, CategoryEntity category) {
		this.name = name;
		this.description = description;
		this.price = price;
		this.active = active;
		this.category = category;
	}
}
