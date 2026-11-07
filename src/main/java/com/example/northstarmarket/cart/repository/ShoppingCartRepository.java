package com.example.northstarmarket.cart.repository;

import java.util.Optional;

import com.example.northstarmarket.cart.model.ShoppingCartEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ShoppingCartRepository extends JpaRepository<ShoppingCartEntity, Long> {

	@Query("""
			select distinct cart from ShoppingCartEntity cart
			join fetch cart.customer
			left join fetch cart.items item
			left join fetch item.product
			where cart.customer.id = :customerId
			""")
	Optional<ShoppingCartEntity> findByCustomerId(@Param("customerId") long customerId);
}
