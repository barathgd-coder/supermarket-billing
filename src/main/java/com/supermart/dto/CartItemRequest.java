package com.supermart.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class CartItemRequest {
    @NotNull
    private Long productId;

    @Min(value = 1, message = "Quantity must be at least 1")
    @Max(value = 10000, message = "Quantity is too large")
    private int quantity;
}
