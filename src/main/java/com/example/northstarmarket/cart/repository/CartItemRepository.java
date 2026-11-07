package com.example.northstarmarket.cart.repository;

import com.example.northstarmarket.cart.model.CartItemEntity;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CartItemRepository extends JpaRepository<CartItemEntity, Long> {
}
