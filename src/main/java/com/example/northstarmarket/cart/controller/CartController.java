package com.example.northstarmarket.cart.controller;

import com.example.northstarmarket.cart.dto.CartResponse;
import com.example.northstarmarket.cart.dto.CartUpdateRequest;
import com.example.northstarmarket.cart.service.CartOperations;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Positive;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping(path = "/api/customers/{customerId}/cart", produces = MediaType.APPLICATION_JSON_VALUE)
public class CartController {

	private final CartOperations carts;

	public CartController(CartOperations carts) {
		this.carts = carts;
	}

	@GetMapping
	CartResponse getCart(@PathVariable @Positive long customerId) {
		return carts.findByCustomerId(customerId);
	}

	@PutMapping(path = "/items/{productId}", consumes = MediaType.APPLICATION_JSON_VALUE)
	CartResponse setItemQuantity(
			@PathVariable @Positive long customerId,
			@PathVariable @Positive long productId,
			@Valid @RequestBody CartUpdateRequest request) {
		return carts.setItemQuantity(
				customerId,
				productId,
				request.quantity(),
				request.expectedVersion());
	}
}
