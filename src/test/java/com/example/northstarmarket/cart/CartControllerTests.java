package com.example.northstarmarket.cart;

import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.matchesPattern;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.List;

import com.example.northstarmarket.cart.controller.CartController;
import com.example.northstarmarket.cart.dto.CartItemResponse;
import com.example.northstarmarket.cart.dto.CartResponse;
import com.example.northstarmarket.cart.exception.CartNotFoundException;
import com.example.northstarmarket.cart.exception.CartVersionConflictException;
import com.example.northstarmarket.cart.service.CartOperations;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import com.example.northstarmarket.web.ApiExceptionHandler;
import com.example.northstarmarket.web.RequestIdFilter;

@WebMvcTest(CartController.class)
@Import({ApiExceptionHandler.class, RequestIdFilter.class})
class CartControllerTests {

	@Autowired
	private MockMvc mockMvc;

	@MockitoBean
	private CartOperations carts;

	@Test
	void getsTheCustomerCart() throws Exception {
		when(carts.findByCustomerId(100L)).thenReturn(cart(0, 1));

		mockMvc.perform(get("/api/customers/100/cart").accept(MediaType.APPLICATION_JSON))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.id").value(200))
				.andExpect(jsonPath("$.customerId").value(100))
				.andExpect(jsonPath("$.version").value(0))
				.andExpect(jsonPath("$.items", hasSize(1)));
	}

	@Test
	void updatesAnItemThroughTheTransactionalUseCase() throws Exception {
		when(carts.setItemQuantity(100L, 43L, 2, 0L)).thenReturn(cart(1, 3));

		mockMvc.perform(put("/api/customers/100/cart/items/43")
				.contentType(MediaType.APPLICATION_JSON)
				.accept(MediaType.APPLICATION_JSON)
				.content("""
						{"quantity":2,"expectedVersion":0}
						"""))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.version").value(1))
				.andExpect(jsonPath("$.totalQuantity").value(3));

		verify(carts).setItemQuantity(100L, 43L, 2, 0L);
	}

	@Test
	void rejectsQuantityOutsideTheTransportBoundary() throws Exception {
		mockMvc.perform(put("/api/customers/100/cart/items/43")
				.contentType(MediaType.APPLICATION_JSON)
				.content("""
						{"quantity":11,"expectedVersion":0}
						"""))
				.andExpect(status().isUnprocessableContent())
				.andExpect(jsonPath("$.code").value("validation_failed"))
				.andExpect(jsonPath("$.errors[0].field").value("quantity"));
	}

	@Test
	void reportsAStaleUpdateAsAStableConflict() throws Exception {
		when(carts.setItemQuantity(100L, 43L, 2, 0L))
				.thenThrow(new CartVersionConflictException(0, 1L));

		mockMvc.perform(put("/api/customers/100/cart/items/43")
				.contentType(MediaType.APPLICATION_JSON)
				.content("""
						{"quantity":2,"expectedVersion":0}
						"""))
				.andExpect(status().isConflict())
				.andExpect(header().string("X-Request-ID", matchesPattern("req-[0-9a-f]{32}")))
				.andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
				.andExpect(jsonPath("$.code").value("cart_version_conflict"))
				.andExpect(jsonPath("$.detail").value("Expected shopping cart version 0 but found 1"));
	}

	@Test
	void reportsAMissingCartWithoutImplyingACustomerCredential() throws Exception {
		when(carts.findByCustomerId(999L)).thenThrow(new CartNotFoundException(999L));

		mockMvc.perform(get("/api/customers/999/cart"))
				.andExpect(status().isNotFound())
				.andExpect(jsonPath("$.code").value("cart_not_found"));
	}

	private CartResponse cart(long version, int totalQuantity) {
		return new CartResponse(
				200,
				100,
				version,
				totalQuantity,
				List.of(new CartItemResponse(42, "Trail Map Set", 1)));
	}
}
