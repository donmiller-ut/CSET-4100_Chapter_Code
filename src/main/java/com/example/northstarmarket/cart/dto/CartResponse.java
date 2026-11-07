package com.example.northstarmarket.cart.dto;

import java.util.List;

public record CartResponse(
		long id,
		long customerId,
		long version,
		int totalQuantity,
		List<CartItemResponse> items) {

	public CartResponse {
		items = List.copyOf(items);
	}
}
