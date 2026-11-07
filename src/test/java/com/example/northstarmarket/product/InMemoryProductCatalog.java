package com.example.northstarmarket.product;

import java.math.BigDecimal;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

class InMemoryProductCatalog implements ProductCatalog {

	private final Map<Long, Product> products = new ConcurrentHashMap<>();
	private final AtomicLong nextId = new AtomicLong(43);

	InMemoryProductCatalog() {
		reset();
	}

	@Override
	public ProductPage findAll(int page, int size, Boolean active, Long categoryId, ProductSort sort) {
		List<Product> matchingProducts = products.values().stream()
				.filter(product -> active == null || product.active() == active)
				.filter(product -> categoryId == null || product.category().id().equals(categoryId))
				.sorted(Comparator.comparing(Product::id))
				.toList();

		long totalElements = matchingProducts.size();
		int totalPages = (int) Math.ceil((double) totalElements / size);
		long requestedStart = (long) page * size;
		int fromIndex = (int) Math.min(requestedStart, matchingProducts.size());
		int toIndex = Math.min(fromIndex + size, matchingProducts.size());
		List<Product> items = matchingProducts.subList(fromIndex, toIndex);

		return new ProductPage(
				items,
				page,
				size,
				totalElements,
				totalPages,
				links(page, size, totalPages, active, categoryId, sort));
	}

	@Override
	public Product findById(long productId) {
		Product product = products.get(productId);
		if (product == null) {
			throw new ProductNotFoundException(productId);
		}
		return product;
	}

	@Override
	public Product create(NewProduct input) {
		long id = nextId.getAndIncrement();
		Product product = new Product(id, input.name(), input.description(), input.price(), input.active(), category(input.categoryId()));
		products.put(id, product);
		return product;
	}

	@Override
	public Product replace(long productId, ProductReplacement input) {
		findById(productId);
		Product replacement = new Product(
				productId,
				input.name(),
				input.description(),
				input.price(),
				input.active(),
				category(input.categoryId()));
		products.put(productId, replacement);
		return replacement;
	}

	@Override
	public void delete(long productId) {
		if (products.remove(productId) == null) {
			throw new ProductNotFoundException(productId);
		}
	}

	void reset() {
		products.clear();
		products.put(42L, new Product(
				42L,
				"Trail Map Set",
				"Water-resistant regional trail maps",
				new BigDecimal("18.50"),
				true,
				category(1L)));
		nextId.set(43);
	}

	private Category category(Long categoryId) {
		if (categoryId == null || categoryId != 1L) {
			throw new CategoryNotFoundException(categoryId == null ? 0 : categoryId);
		}
		return new Category(1L, "Maps and Navigation");
	}

	private PageLinks links(int page, int size, int totalPages, Boolean active, Long categoryId, ProductSort sort) {
		int lastPage = Math.max(totalPages - 1, 0);
		return new PageLinks(
				pageLink(page, size, active, categoryId, sort),
				pageLink(0, size, active, categoryId, sort),
				page > 0 ? pageLink(page - 1, size, active, categoryId, sort) : null,
				page < totalPages - 1 ? pageLink(page + 1, size, active, categoryId, sort) : null,
				pageLink(lastPage, size, active, categoryId, sort));
	}

	private String pageLink(int page, int size, Boolean active, Long categoryId, ProductSort sort) {
		String activeQuery = active == null ? "" : "&active=" + active;
		String categoryQuery = categoryId == null ? "" : "&categoryId=" + categoryId;
		String sortQuery = sort == ProductSort.ID_ASC ? "" : "&sort=" + sort.queryValue();
		return "/api/products?page=" + page + "&size=" + size + activeQuery + categoryQuery + sortQuery;
	}
}
