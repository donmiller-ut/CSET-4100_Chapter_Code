package com.example.northstarmarket.cart.exception;

public final class CartCapacityExceededException extends RuntimeException {

	public CartCapacityExceededException(int attemptedTotal, int maximumTotal) {
		super("A shopping cart may contain at most " + maximumTotal
				+ " total units; the update attempted " + attemptedTotal);
	}
}
