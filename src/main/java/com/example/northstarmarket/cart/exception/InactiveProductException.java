package com.example.northstarmarket.cart.exception;

public final class InactiveProductException extends RuntimeException {

	public InactiveProductException(long productId) {
		super("Product " + productId + " is not available for cart updates");
	}
}
