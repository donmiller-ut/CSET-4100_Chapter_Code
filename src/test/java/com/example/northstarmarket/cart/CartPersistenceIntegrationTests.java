package com.example.northstarmarket.cart;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.example.northstarmarket.cart.dto.CartItemResponse;
import com.example.northstarmarket.cart.dto.CartResponse;
import com.example.northstarmarket.cart.exception.CartCapacityExceededException;
import com.example.northstarmarket.cart.exception.CartVersionConflictException;
import com.example.northstarmarket.cart.model.ShoppingCartEntity;
import com.example.northstarmarket.cart.service.CartOperations;
import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import jakarta.persistence.OptimisticLockException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.JdbcTemplate;

@SpringBootTest
class CartPersistenceIntegrationTests {

	@Autowired
	private CartOperations carts;

	@Autowired
	private JdbcTemplate jdbc;

	@Autowired
	private EntityManagerFactory entityManagerFactory;

	@BeforeEach
	void restoreSeedCart() {
		jdbc.update("delete from cart_item where cart_id = 200");
		jdbc.update("update shopping_cart set total_quantity = 1, version = 0 where cart_id = 200");
		jdbc.update("insert into cart_item (cart_id, product_id, quantity) values (200, 42, 1)");
	}

	@AfterEach
	void leaveSeedCartForOtherSuites() {
		restoreSeedCart();
	}

	@Test
	void migrationsProvideCustomerCartAndItemIntegrity() {
		CartResponse cart = carts.findByCustomerId(100L);

		assertThat(cart.id()).isEqualTo(200L);
		assertThat(cart.customerId()).isEqualTo(100L);
		assertThat(cart.version()).isZero();
		assertThat(cart.totalQuantity()).isEqualTo(1);
		assertThat(cart.items()).extracting(CartItemResponse::productId).containsExactly(42L);
	}

	@Test
	void successfulUpdateCommitsItemAndCartSummaryTogether() {
		CartResponse updated = carts.setItemQuantity(100L, 43L, 2, 0L);

		assertThat(updated.version()).isEqualTo(1L);
		assertThat(updated.totalQuantity()).isEqualTo(3);
		assertThat(jdbc.queryForObject(
				"select quantity from cart_item where cart_id = 200 and product_id = 43",
				Integer.class)).isEqualTo(2);
		assertThat(jdbc.queryForObject(
				"select total_quantity from shopping_cart where cart_id = 200",
				Integer.class)).isEqualTo(3);
	}

	@Test
	void failureAfterItemFlushRollsBackTheWholeUseCase() {
		CartResponse first = carts.setItemQuantity(100L, 43L, 10, 0L);
		CartResponse full = carts.setItemQuantity(100L, 44L, 9, first.version());

		assertThat(full.totalQuantity()).isEqualTo(20);
		assertThatThrownBy(() -> carts.setItemQuantity(100L, 46L, 1, full.version()))
				.isInstanceOf(CartCapacityExceededException.class)
				.hasMessageContaining("attempted 21");

		CartResponse afterFailure = carts.findByCustomerId(100L);
		assertThat(afterFailure.totalQuantity()).isEqualTo(20);
		assertThat(afterFailure.version()).isEqualTo(full.version());
		assertThat(afterFailure.items()).extracting(CartItemResponse::productId).doesNotContain(46L);
	}

	@Test
	void databaseConstraintRejectsInvalidQuantityOutsideTheHttpBoundary() {
		assertThatThrownBy(() -> jdbc.update(
				"insert into cart_item (cart_id, product_id, quantity) values (200, 43, 0)"))
				.isInstanceOf(DataIntegrityViolationException.class);
	}

	@Test
	void staleExpectedVersionCannotOverwriteANewerCart() {
		CartResponse first = carts.setItemQuantity(100L, 43L, 2, 0L);

		assertThat(first.version()).isEqualTo(1L);
		assertThatThrownBy(() -> carts.setItemQuantity(100L, 44L, 2, 0L))
				.isInstanceOf(CartVersionConflictException.class)
				.hasMessage("Expected shopping cart version 0 but found 1");
		assertThat(carts.findByCustomerId(100L).items())
				.extracting(CartItemResponse::productId)
				.doesNotContain(44L);
	}

	@Test
	void entityVersionDetectsOverlappingLostUpdateAttempts() {
		EntityManager firstManager = entityManagerFactory.createEntityManager();
		EntityManager secondManager = entityManagerFactory.createEntityManager();
		try {
			firstManager.getTransaction().begin();
			secondManager.getTransaction().begin();
			ShoppingCartEntity first = firstManager.find(ShoppingCartEntity.class, 200L);
			ShoppingCartEntity second = secondManager.find(ShoppingCartEntity.class, 200L);

			first.changeTotalQuantity(2);
			firstManager.flush();
			firstManager.getTransaction().commit();

			second.changeTotalQuantity(3);
			assertThatThrownBy(secondManager::flush)
					.isInstanceOf(OptimisticLockException.class);
		}
		finally {
			if (firstManager.getTransaction().isActive()) {
				firstManager.getTransaction().rollback();
			}
			if (secondManager.getTransaction().isActive()) {
				secondManager.getTransaction().rollback();
			}
			firstManager.close();
			secondManager.close();
		}
	}
}
