package com.example.northstarmarket.product;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(ProductController.class)
@Import({InMemoryProductCatalog.class, ProductMapper.class,
        com.example.northstarmarket.web.ApiExceptionHandler.class,
        com.example.northstarmarket.web.RequestIdFilter.class})
class RelationshipBoundaryTests {
    @Autowired MockMvc mvc;
    @Autowired InMemoryProductCatalog catalog;
    @BeforeEach void reset() { catalog.reset(); }

    @Test void explicitNullCategoryIsAClientErrorForBothWrites() throws Exception {
        String body = """
                {"name":"Valid","description":null,"price":1,"active":true,"categoryId":null}
                """;
        mvc.perform(post("/api/products").contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isUnprocessableContent());
        mvc.perform(put("/api/products/42").contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isUnprocessableContent());
        mvc.perform(get("/api/products/42"))
                .andExpect(jsonPath("$.name").value("Trail Map Set"));
    }

    @Test void omittedCategoryUsesTheCompatibilityDefault() throws Exception {
        mvc.perform(post("/api/products").contentType(MediaType.APPLICATION_JSON)
                .content("{\"name\":\"Valid\",\"price\":1}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.category.id").value(1))
                .andExpect(jsonPath("$.category.products").doesNotExist());
    }

    @Test void nonexistentWriteCategoryHasAStableProblem() throws Exception {
        mvc.perform(post("/api/products").contentType(MediaType.APPLICATION_JSON)
                .content("{\"name\":\"Valid\",\"price\":1,\"categoryId\":999}"))
                .andExpect(status().isUnprocessableContent())
                .andExpect(jsonPath("$.code").value("category_not_found"));
    }

    @Test void rejectsUnapprovedSortAndNonpositiveCategoryFilter() throws Exception {
        mvc.perform(get("/api/products?sort=category.name,asc"))
                .andExpect(status().isBadRequest());
        mvc.perform(get("/api/products?categoryId=0"))
                .andExpect(status().isBadRequest());
    }
}
