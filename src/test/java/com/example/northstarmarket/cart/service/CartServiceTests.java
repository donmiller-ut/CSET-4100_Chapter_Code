package com.example.northstarmarket.cart.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Optional;

import com.example.northstarmarket.cart.exception.CartCapacityExceededException;
import com.example.northstarmarket.cart.exception.CartVersionConflictException;
import com.example.northstarmarket.cart.model.ShoppingCartEntity;
import com.example.northstarmarket.cart.repository.CartItemRepository;
import com.example.northstarmarket.cart.repository.ShoppingCartRepository;
import com.example.northstarmarket.config.NorthstarProperties;
import com.example.northstarmarket.product.ProductEntity;
import com.example.northstarmarket.product.ProductRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class CartServiceTests {

	private final ShoppingCartRepository carts = mock(ShoppingCartRepository.class);
	private final CartItemRepository items = mock(CartItemRepository.class);
	private final ProductRepository products = mock(ProductRepository.class);
	private final CartMapper mapper = mock(CartMapper.class);
	private final ShoppingCartEntity cart = mock(ShoppingCartEntity.class);
	private final ProductEntity product = mock(ProductEntity.class);
	private final CartService service = new CartService(
			carts,
			items,
			products,
			mapper,
			new NorthstarProperties(new NorthstarProperties.Cart(20)));

	@BeforeEach
	void arrangeExistingCartAndProduct() {
		when(carts.findByCustomerId(100L)).thenReturn(Optional.of(cart));
		when(products.findById(42L)).thenReturn(Optional.of(product));
		when(product.isActive()).thenReturn(true);
	}

	@Test
	void rejectsAStaleVersionBeforeLookingUpTheProduct() {
		when(cart.getVersion()).thenReturn(3L);

		assertThatThrownBy(() -> service.setItemQuantity(100L, 42L, 2, 2L))
				.isInstanceOf(CartVersionConflictException.class);

		verify(products, never()).findById(42L);
		verify(items, never()).saveAndFlush(org.mockito.ArgumentMatchers.any());
	}

	@Test
	void enforcesTheConfiguredCapacityAfterTheItemWriteIsFlushed() {
		when(cart.getVersion()).thenReturn(0L);
		when(cart.setQuantity(product, 10)).thenReturn(mock(com.example.northstarmarket.cart.model.CartItemEntity.class));
		when(cart.calculateTotalQuantity()).thenReturn(21);

		assertThatThrownBy(() -> service.setItemQuantity(100L, 42L, 10, 0L))
				.isInstanceOf(CartCapacityExceededException.class)
				.hasMessageContaining("at most 20")
				.hasMessageContaining("attempted 21");

		verify(items).saveAndFlush(org.mockito.ArgumentMatchers.any());
		verify(carts, never()).flush();
	}

	@Test
	void delegatesReadMappingToTheMapperBoundary() {
		var expected = new com.example.northstarmarket.cart.dto.CartResponse(200, 100, 0, 1, java.util.List.of());
		when(mapper.toResponse(cart)).thenReturn(expected);

		assertThat(service.findByCustomerId(100L)).isSameAs(expected);
	}
}
