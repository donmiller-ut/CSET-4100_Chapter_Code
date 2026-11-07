package com.example.northstarmarket.cart.service;

import java.util.Comparator;

import com.example.northstarmarket.cart.dto.CartItemResponse;
import com.example.northstarmarket.cart.dto.CartResponse;
import com.example.northstarmarket.cart.model.ShoppingCartEntity;
import org.springframework.stereotype.Component;

@Component
public class CartMapper {

	public CartResponse toResponse(ShoppingCartEntity cart) {
		return new CartResponse(
				cart.getId(),
				cart.getCustomer().getId(),
				cart.getVersion(),
				cart.getTotalQuantity(),
				cart.getItems().stream()
						.sorted(Comparator.comparing(item -> item.getProduct().getId()))
						.map(item -> new CartItemResponse(
								item.getProduct().getId(),
								item.getProduct().getName(),
								item.getQuantity()))
						.toList());
	}
}
