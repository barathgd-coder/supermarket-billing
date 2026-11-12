package com.supermart.dto;

import jakarta.validation.constraints.*;
import lombok.Data;
import org.springframework.format.annotation.DateTimeFormat;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
public class ProductForm {
    private Long id;

    @NotBlank(message = "Product ID / barcode is required")
    @Size(max = 50, message = "Barcode can be at most 50 characters")
    private String barcode;

    @NotBlank(message = "Product name is required")
    @Size(max = 150, message = "Name can be at most 150 characters")
    private String name;

    @NotNull(message = "Select a category")
    private Long categoryId;

    private Long supplierId;

    @NotNull(message = "Purchase price is required")
    @DecimalMin(value = "0.00", message = "Purchase price cannot be negative")
    @Digits(integer = 10, fraction = 2, message = "Use at most 2 decimal places")
    private BigDecimal purchasePrice;

    @NotNull(message = "Selling price is required")
    @DecimalMin(value = "0.00", message = "Selling price cannot be negative")
    @Digits(integer = 10, fraction = 2, message = "Use at most 2 decimal places")
    private BigDecimal sellingPrice;

    @NotNull(message = "Quantity is required")
    @Min(value = 0, message = "Quantity cannot be negative")
    private Integer quantity;

    @NotNull(message = "GST % is required")
    @DecimalMin(value = "0", message = "GST cannot be negative")
    @DecimalMax(value = "100", message = "GST cannot exceed 100")
    private BigDecimal gstPercent;

    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    private LocalDate expiryDate;

    @NotNull(message = "Reorder level is required")
    @Min(value = 0, message = "Reorder level cannot be negative")
    private Integer reorderLevel;
}
