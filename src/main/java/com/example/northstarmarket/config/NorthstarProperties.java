package com.example.northstarmarket.config;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@Validated
@ConfigurationProperties("northstar")
public record NorthstarProperties(@NotNull @Valid Cart cart) {

	public record Cart(@Min(1) @Max(20) int maximumTotalQuantity) {
	}
}
