package com.supermart.dto;

import com.supermart.model.TransactionType;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class StockEntryForm {
    @NotNull(message = "Select a product")
    private Long productId;

    @NotNull(message = "Select stock-in or stock-out")
    private TransactionType type = TransactionType.STOCK_IN;

    @NotNull(message = "Quantity is required")
    @Min(value = 1, message = "Quantity must be at least 1")
    private Integer quantity;

    @Size(max = 100, message = "Reference is too long")
    private String reference;

    @Size(max = 255, message = "Note is too long")
    private String note;
}
