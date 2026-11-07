package com.example.northstarmarket.product;

import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
class JpaProductCatalog implements ProductCatalog {

	private final ProductRepository products;
	private final CategoryRepository categories;

	JpaProductCatalog(ProductRepository products, CategoryRepository categories) {
		this.products = products;
		this.categories = categories;
	}

	@Override
	@Transactional(readOnly = true)
	public ProductPage findAll(int page, int size, Boolean active, Long categoryId, ProductSort sort) {
		long requestedStart = (long) page * size;
		if (requestedStart > Integer.MAX_VALUE) {
			long totalElements = products.countMatching(active, categoryId);
			return page(List.of(), page, size, totalElements, active, categoryId, sort);
		}

		PageRequest request = PageRequest.of(page, size, sort.springSort());
		Page<ProductEntity> result = products.search(active, categoryId, request);
		return page(result.getContent(), page, size, result.getTotalElements(), active, categoryId, sort);
	}

	@Override
	@Transactional(readOnly = true)
	public Product findById(long productId) {
		return toProduct(required(productId));
	}

	@Override
	@Transactional
	public Product create(NewProduct input) {
		ProductEntity entity = new ProductEntity(
				input.name(),
				input.description(),
				input.price(),
				input.active(),
				requiredCategory(input.categoryId()));
		return toProduct(products.save(entity));
	}

	@Override
	@Transactional
	public Product replace(long productId, ProductReplacement input) {
		ProductEntity entity = required(productId);
		entity.replace(input.name(), input.description(), input.price(), input.active(),
				requiredCategory(input.categoryId()));
		return toProduct(entity);
	}

	@Override
	@Transactional
	public void delete(long productId) {
		products.delete(required(productId));
	}

	private ProductEntity required(long productId) {
		return products.findById(productId)
				.orElseThrow(() -> new ProductNotFoundException(productId));
	}

	private CategoryEntity requiredCategory(long categoryId) {
		return categories.findById(categoryId)
				.orElseThrow(() -> new CategoryNotFoundException(categoryId));
	}

	private ProductPage page(
			List<ProductEntity> entities,
			int page,
			int size,
			long totalElements,
			Boolean active,
			Long categoryId,
			ProductSort sort) {
		int totalPages = (int) Math.ceil((double) totalElements / size);
		return new ProductPage(
				entities.stream().map(this::toProduct).toList(),
				page,
				size,
				totalElements,
				totalPages,
				links(page, size, totalPages, active, categoryId, sort));
	}

	private Product toProduct(ProductEntity entity) {
		return new Product(
				entity.getId(),
				entity.getName(),
				entity.getDescription(),
				entity.getPrice(),
				entity.isActive(),
				new Category(entity.getCategory().getId(), entity.getCategory().getName()));
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
		return "/api/products?page=" + page + "&size=" + size + activeQuery
				+ categoryQuery + sortQuery;
	}
}
