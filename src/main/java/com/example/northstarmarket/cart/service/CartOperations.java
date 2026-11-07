package com.example.northstarmarket.cart.service;

import com.example.northstarmarket.cart.dto.CartResponse;

public interface CartOperations {

	CartResponse findByCustomerId(long customerId);

	CartResponse setItemQuantity(long customerId, long productId, int quantity, long expectedVersion);
}
