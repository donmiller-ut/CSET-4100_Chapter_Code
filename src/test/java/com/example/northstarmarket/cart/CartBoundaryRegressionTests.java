package com.example.northstarmarket.cart;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import com.example.northstarmarket.cart.controller.CartController;
import com.example.northstarmarket.cart.service.CartOperations;
import com.example.northstarmarket.web.ApiExceptionHandler;
import com.example.northstarmarket.web.RequestIdFilter;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(CartController.class)
@Import({ApiExceptionHandler.class, RequestIdFilter.class})
class CartBoundaryRegressionTests {
    @Autowired MockMvc mvc;
    @MockitoBean CartOperations carts;

    @Test void nonpositiveCustomerUsesSafeParameterProblem() throws Exception {
        mvc.perform(get("/api/customers/0/cart"))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.code").value("invalid_parameter"));
    }
    @Test void nonpositiveProductUsesSafeParameterProblem() throws Exception {
        mvc.perform(put("/api/customers/100/cart/items/0")
            .contentType("application/json").content("{\"quantity\":1,\"expectedVersion\":0}"))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.code").value("invalid_parameter"));
    }
    @Test void nullFieldsFailBeforeServiceInvocation() throws Exception {
        mvc.perform(put("/api/customers/100/cart/items/43")
            .contentType("application/json").content("{\"quantity\":null,\"expectedVersion\":null}"))
            .andExpect(status().isUnprocessableContent())
            .andExpect(jsonPath("$.code").value("validation_failed"));
    }
}
