package com.example.northstarmarket.cart.service;

import com.example.northstarmarket.cart.dto.CartResponse;
import com.example.northstarmarket.cart.exception.CartCapacityExceededException;
import com.example.northstarmarket.cart.exception.CartNotFoundException;
import com.example.northstarmarket.cart.exception.CartVersionConflictException;
import com.example.northstarmarket.cart.exception.InactiveProductException;
import com.example.northstarmarket.cart.model.CartItemEntity;
import com.example.northstarmarket.cart.model.ShoppingCartEntity;
import com.example.northstarmarket.cart.repository.CartItemRepository;
import com.example.northstarmarket.cart.repository.ShoppingCartRepository;
import com.example.northstarmarket.config.NorthstarProperties;
import com.example.northstarmarket.product.ProductEntity;
import com.example.northstarmarket.product.ProductNotFoundException;
import com.example.northstarmarket.product.ProductRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class CartService implements CartOperations {

	private static final Logger logger = LoggerFactory.getLogger(CartService.class);

	private final ShoppingCartRepository carts;
	private final CartItemRepository items;
	private final ProductRepository products;
	private final CartMapper mapper;
	private final NorthstarProperties properties;

	public CartService(
			ShoppingCartRepository carts,
			CartItemRepository items,
			ProductRepository products,
			CartMapper mapper,
			NorthstarProperties properties) {
		this.carts = carts;
		this.items = items;
		this.products = products;
		this.mapper = mapper;
		this.properties = properties;
	}

	@Override
	@Transactional(readOnly = true)
	public CartResponse findByCustomerId(long customerId) {
		return mapper.toResponse(requiredCart(customerId));
	}

	@Override
	@Transactional
	public CartResponse setItemQuantity(
			long customerId,
			long productId,
			int quantity,
			long expectedVersion) {
		ShoppingCartEntity cart = requiredCart(customerId);
		if (cart.getVersion() != expectedVersion) {
			throw new CartVersionConflictException(expectedVersion, cart.getVersion());
		}

		ProductEntity product = products.findById(productId)
				.orElseThrow(() -> new ProductNotFoundException(productId));
		if (!product.isActive()) {
			throw new InactiveProductException(productId);
		}

		CartItemEntity item = cart.setQuantity(product, quantity);
		items.saveAndFlush(item);

		int totalQuantity = cart.calculateTotalQuantity();
		if (totalQuantity > properties.cart().maximumTotalQuantity()) {
			logger.warn("Cart update rejected: attemptedTotal={}, configuredMaximum={}",
					totalQuantity, properties.cart().maximumTotalQuantity());
			throw new CartCapacityExceededException(
					totalQuantity, properties.cart().maximumTotalQuantity());
		}

		cart.changeTotalQuantity(totalQuantity);
		try {
			carts.flush();
		}
		catch (OptimisticLockingFailureException exception) {
			throw new CartVersionConflictException(expectedVersion, null);
		}
		logger.info("Cart update prepared for commit: productId={}, quantity={}, resultingTotal={}",
				productId, quantity, totalQuantity);
		return mapper.toResponse(cart);
	}

	private ShoppingCartEntity requiredCart(long customerId) {
		return carts.findByCustomerId(customerId)
				.orElseThrow(() -> new CartNotFoundException(customerId));
	}

}
