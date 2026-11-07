package com.example.northstarmarket.product;

import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ProductRepository extends JpaRepository<ProductEntity, Long> {

	@Override
	@EntityGraph(attributePaths = "category")
	Optional<ProductEntity> findById(Long id);

	@Query(value = """
			select p from ProductEntity p join fetch p.category c
			where (:active is null or p.active = :active)
			  and (:categoryId is null or c.id = :categoryId)
			""", countQuery = """
			select count(p) from ProductEntity p
			where (:active is null or p.active = :active)
			  and (:categoryId is null or p.category.id = :categoryId)
			""")
	Page<ProductEntity> search(
			@Param("active") Boolean active,
			@Param("categoryId") Long categoryId,
			Pageable pageable);

	@Query("""
			select count(p) from ProductEntity p
			where (:active is null or p.active = :active)
			  and (:categoryId is null or p.category.id = :categoryId)
			""")
	long countMatching(@Param("active") Boolean active, @Param("categoryId") Long categoryId);

	@Query("select p from ProductEntity p order by p.id")
	List<ProductEntity> findAllWithoutCategoryFetch();
}
