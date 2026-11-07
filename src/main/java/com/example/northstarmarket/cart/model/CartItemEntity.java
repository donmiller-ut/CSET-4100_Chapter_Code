package com.example.northstarmarket.cart.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

import com.example.northstarmarket.product.ProductEntity;

@Entity
@Table(name = "cart_item", schema = "northstar")
public class CartItemEntity {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	@Column(name = "cart_item_id")
	private Long id;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "cart_id", nullable = false)
	private ShoppingCartEntity cart;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "product_id", nullable = false)
	private ProductEntity product;

	@Column(nullable = false)
	private int quantity;

	protected CartItemEntity() {
	}

	CartItemEntity(ShoppingCartEntity cart, ProductEntity product, int quantity) {
		this.cart = cart;
		this.product = product;
		this.quantity = quantity;
	}

	public ProductEntity getProduct() {
		return product;
	}

	public int getQuantity() {
		return quantity;
	}

	void setQuantity(int quantity) {
		this.quantity = quantity;
	}
}
