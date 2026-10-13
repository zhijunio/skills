package com.example.sb4jpavueproduct.product;

import java.math.BigDecimal;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;

public record ProductRequest(
		@NotBlank @Size(max = 120) String name,
		@NotBlank @Size(max = 32) @Pattern(regexp = "[A-Z0-9][A-Z0-9-]{1,31}", message = "must contain 2-32 uppercase letters, digits, or hyphens") String sku,
		@NotNull @DecimalMin("0.00") @Digits(integer = 10, fraction = 2) BigDecimal price,
		@PositiveOrZero int quantity) {
}
