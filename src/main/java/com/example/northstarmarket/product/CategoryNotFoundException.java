package com.example.northstarmarket.product;

public final class CategoryNotFoundException extends RuntimeException {

	public CategoryNotFoundException(long categoryId) {
		super("Category " + categoryId + " was not found");
	}
}
