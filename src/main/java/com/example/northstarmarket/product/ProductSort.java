package com.example.northstarmarket.product;

import org.springframework.data.domain.Sort;

enum ProductSort {
	ID_ASC("id,asc", Sort.by("id").ascending()),
	ID_DESC("id,desc", Sort.by("id").descending()),
	NAME_ASC("name,asc", Sort.by("name").ascending()),
	NAME_DESC("name,desc", Sort.by("name").descending()),
	PRICE_ASC("price,asc", Sort.by("price").ascending()),
	PRICE_DESC("price,desc", Sort.by("price").descending());

	private final String queryValue;
	private final Sort springSort;

	ProductSort(String queryValue, Sort springSort) {
		this.queryValue = queryValue;
		this.springSort = springSort.and(Sort.by("id"));
	}

	static ProductSort parse(String value) {
		for (ProductSort candidate : values()) {
			if (candidate.queryValue.equals(value)) {
				return candidate;
			}
		}
		throw new IllegalArgumentException("Unsupported Product sort");
	}

	String queryValue() {
		return queryValue;
	}

	Sort springSort() {
		return springSort;
	}
}
