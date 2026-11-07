package com.example.northstarmarket.cart.exception;

public final class CartNotFoundException extends RuntimeException {

	public CartNotFoundException(long customerId) {
		super("No shopping cart was found for Customer " + customerId);
	}
}
