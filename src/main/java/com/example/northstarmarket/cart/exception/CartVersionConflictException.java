package com.example.northstarmarket.cart.exception;

public final class CartVersionConflictException extends RuntimeException {

	public CartVersionConflictException(long expectedVersion, Long currentVersion) {
		super(currentVersion == null
				? "The shopping cart changed while this update was being saved"
				: "Expected shopping cart version " + expectedVersion + " but found " + currentVersion);
	}
}
