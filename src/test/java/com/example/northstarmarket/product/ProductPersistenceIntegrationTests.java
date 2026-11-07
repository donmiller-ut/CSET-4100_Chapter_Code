package com.example.northstarmarket.product;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.math.BigDecimal;

import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import org.hibernate.SessionFactory;
import org.hibernate.stat.Statistics;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@Transactional
class ProductPersistenceIntegrationTests {

	@Autowired
	private ProductRepository products;

	@Autowired
	private CategoryRepository categories;

	@Autowired
	private ProductCatalog catalog;

	@Autowired
	private EntityManager entityManager;

	@Autowired
	private EntityManagerFactory entityManagerFactory;

	@BeforeEach
	void isolateTheControlledCatalogSeed() {
		products.deleteAll(products.findAll().stream()
				.filter(product -> product.getId() > 46L)
				.toList());
		products.flush();
		entityManager.clear();
	}

	@Test
	void migrationsProvideTheSeededProductAndDefaultCategory() {
		ProductEntity product = products.findById(42L).orElseThrow();
		CategoryEntity category = categories.findById(1L).orElseThrow();

		assertThat(product.getName()).isEqualTo("Trail Map Set");
		assertThat(product.getCategory().getId()).isEqualTo(category.getId());
		assertThat(category.getName()).isEqualTo("Maps and Navigation");
	}

	@Test
	void repositoryUsesPostgresqlGeneratedIdentity() {
		CategoryEntity category = categories.findById(1L).orElseThrow();
		ProductEntity saved = products.saveAndFlush(new ProductEntity(
				"Repository Compass",
				"Created through the repository test",
				new BigDecimal("29.95"),
				true,
				category));

		assertThat(saved.getId()).isGreaterThan(42L);
		assertThat(products.findById(saved.getId())).isPresent();
	}

	@Test
	void managedReplacementIsWrittenThroughDirtyChecking() {
		ProductReplacement replacement = new ProductReplacement(
				"Persistent Trail Map Set",
				null,
				new BigDecimal("19.75"),
				false,
				1L);

		Product changed = catalog.replace(42L, replacement);
		entityManager.flush();
		entityManager.clear();

		ProductEntity reloaded = products.findById(42L).orElseThrow();
		assertThat(changed.name()).isEqualTo("Persistent Trail Map Set");
		assertThat(reloaded.getName()).isEqualTo("Persistent Trail Map Set");
		assertThat(reloaded.isActive()).isFalse();
	}

	@Test
	void databaseConstraintRejectsAnInvalidProductEvenWithoutDtoValidation() {
		ProductEntity invalid = new ProductEntity(
				"Invalid Database Price",
				null,
				new BigDecimal("0.00"),
				true,
				categories.findById(1L).orElseThrow());

		assertThatThrownBy(() -> products.saveAndFlush(invalid))
				.isInstanceOf(DataIntegrityViolationException.class);
	}

	@Test
	void unoptimizedRelationshipTraversalExposesTheNPlusOnePattern() {
		Statistics statistics = statistics();
		entityManager.clear();
		statistics.clear();

		var unoptimized = products.findAllWithoutCategoryFetch();
		assertThat(unoptimized).hasSize(5);
		assertThat(unoptimized.stream().map(product -> product.getCategory().getName()).toList())
				.contains("Maps and Navigation", "Hydration", "Camp Kitchen");

		assertThat(statistics.getPrepareStatementCount()).isEqualTo(4);
	}

	@Test
	void fetchJoinKeepsAFilteredSortedPageToOneStatementWhenThePageIsNotFull() {
		Statistics statistics = statistics();
		entityManager.clear();
		statistics.clear();

		var page = products.search(true, 2L,
				org.springframework.data.domain.PageRequest.of(
						0, 10, org.springframework.data.domain.Sort.by("price").descending()));
		assertThat(page.getContent()).extracting(ProductEntity::getName)
				.containsExactly("Water Filter", "Insulated Bottle");
		assertThat(page.getContent().stream().map(product -> product.getCategory().getName()).toList())
				.containsOnly("Hydration");

		assertThat(statistics.getPrepareStatementCount()).isEqualTo(1);
	}

	private Statistics statistics() {
		Statistics statistics = entityManagerFactory.unwrap(SessionFactory.class).getStatistics();
		statistics.setStatisticsEnabled(true);
		return statistics;
	}
}
