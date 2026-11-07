package com.example.northstarmarket.cart.model;

import java.util.ArrayList;
import java.util.List;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import jakarta.persistence.Version;

import com.example.northstarmarket.product.ProductEntity;

@Entity
@Table(name = "shopping_cart", schema = "northstar")
public class ShoppingCartEntity {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	@Column(name = "cart_id")
	private Long id;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "customer_id", nullable = false, unique = true)
	private CustomerEntity customer;

	@Version
	@Column(nullable = false)
	private long version;

	@Column(name = "total_quantity", nullable = false)
	private int totalQuantity;

	@OneToMany(mappedBy = "cart")
	private List<CartItemEntity> items = new ArrayList<>();

	protected ShoppingCartEntity() {
	}

	public Long getId() {
		return id;
	}

	public CustomerEntity getCustomer() {
		return customer;
	}

	public long getVersion() {
		return version;
	}

	public int getTotalQuantity() {
		return totalQuantity;
	}

	public List<CartItemEntity> getItems() {
		return items;
	}

	public CartItemEntity setQuantity(ProductEntity product, int quantity) {
		CartItemEntity item = items.stream()
				.filter(candidate -> candidate.getProduct().getId().equals(product.getId()))
				.findFirst()
				.orElseGet(() -> {
					CartItemEntity created = new CartItemEntity(this, product, quantity);
					items.add(created);
					return created;
				});
		item.setQuantity(quantity);
		return item;
	}

	public int calculateTotalQuantity() {
		return items.stream().mapToInt(CartItemEntity::getQuantity).sum();
	}

	public void changeTotalQuantity(int totalQuantity) {
		this.totalQuantity = totalQuantity;
	}
}
