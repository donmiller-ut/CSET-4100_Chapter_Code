package com.example.northstarmarket.product;

import static org.assertj.core.api.Assertions.*;
import java.math.BigDecimal;
import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import org.hibernate.SessionFactory;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.PageRequest;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@Transactional
class CollectionPersistenceRegressionTests {
    @Autowired ProductCatalog catalog;
    @Autowired ProductRepository products;
    @Autowired CategoryRepository categories;
    @Autowired EntityManager entityManager;
    @Autowired EntityManagerFactory entityManagerFactory;

    @Test void fullFilteredPageCountsTheSameSetWithoutExtraCategoryReads() {
        var stats = entityManagerFactory.unwrap(SessionFactory.class).getStatistics();
        stats.setStatisticsEnabled(true);
        entityManager.clear();
        stats.clear();
        var page = products.search(true, 2L, PageRequest.of(0, 1, ProductSort.PRICE_DESC.springSort()));
        assertThat(page.getContent()).extracting(ProductEntity::getName).containsExactly("Water Filter");
        assertThat(page.getContent().getFirst().getCategory().getName()).isEqualTo("Hydration");
        assertThat(page.getTotalElements()).isEqualTo(2);
        assertThat(page.getTotalPages()).isEqualTo(2);
        assertThat(stats.getPrepareStatementCount()).isEqualTo(2);
    }

    @Test void equalPriceRowsUseIdentifierOrderingAcrossPages() {
        var a = catalog.create(new NewProduct("Tie A", null, new BigDecimal("0.01"), true, 2L));
        var b = catalog.create(new NewProduct("Tie B", null, new BigDecimal("0.01"), true, 2L));
        entityManager.flush();
        entityManager.clear();
        assertThat(catalog.findAll(0, 1, true, 2L, ProductSort.PRICE_ASC).items().getFirst().id()).isEqualTo(a.id());
        assertThat(catalog.findAll(1, 1, true, 2L, ProductSort.PRICE_ASC).items().getFirst().id()).isEqualTo(b.id());
    }

    @Test void storesCodePointLimitsAndTrailingZeroCents() {
        var saved = catalog.create(new NewProduct("🚀".repeat(120), "🚀".repeat(1000), new BigDecimal("1.2300"), true, 2L));
        entityManager.flush();
        entityManager.clear();
        var read = catalog.findById(saved.id());
        assertThat(read.name()).isEqualTo(saved.name());
        assertThat(read.description()).isEqualTo(saved.description());
        assertThat(read.price()).isEqualByComparingTo("1.23");
        assertThat(read.category().id()).isEqualTo(2);
    }

    @Test void hugeFilteredPageRetainsMatchingTotalsAndSafeLinks() {
        var page = catalog.findAll(Integer.MAX_VALUE, 100, true, 2L, ProductSort.PRICE_DESC);
        assertThat(page.items()).isEmpty();
        assertThat(page.totalElements()).isEqualTo(2);
        assertThat(page.links().next()).isNull();
        assertThat(page.links().self()).contains("categoryId=2", "active=true", "sort=price,desc");
    }

    @Test void databaseRejectsFractionalCentsWithoutDtoValidation() {
        assertThatThrownBy(() -> products.saveAndFlush(new ProductEntity("Invalid", null,
                new BigDecimal("1.001"), true, categories.findById(1L).orElseThrow())))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test void contextReusesIdentityAndClearDetachesIt() {
        var first = products.findById(42L).orElseThrow();
        assertThat(products.findById(42L).orElseThrow()).isSameAs(first);
        entityManager.clear();
        assertThat(entityManager.contains(first)).isFalse();
        assertThat(products.findById(42L).orElseThrow()).isNotSameAs(first);
    }

    @Test void categorySelectionFailsBeforeCreatingARow() {
        long before = products.count();
        assertThatThrownBy(() -> catalog.create(new NewProduct("Invalid", null, BigDecimal.ONE, true, 999L)))
                .isInstanceOf(CategoryNotFoundException.class);
        assertThat(products.count()).isEqualTo(before);
    }
}
