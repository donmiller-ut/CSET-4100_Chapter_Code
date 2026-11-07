package com.example.northstarmarket.cart.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;

public record CartUpdateRequest(
		@NotNull @Min(1) @Max(10) Integer quantity,
		@NotNull @PositiveOrZero Long expectedVersion) {
}
