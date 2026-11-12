package com.supermart.dto;

import com.supermart.model.DiscountType;
import com.supermart.model.PaymentMethod;
import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

@Data
public class CheckoutRequest {
    @Size(max = 120, message = "Customer name is too long")
    private String customerName;

    @Pattern(regexp = "^$|^[0-9+\\- ]{7,15}$", message = "Enter a valid phone number")
    private String customerPhone;

    @NotNull(message = "Select a payment method")
    private PaymentMethod paymentMethod;

    private DiscountType discountType = DiscountType.NONE;

    @DecimalMin(value = "0", message = "Discount cannot be negative")
    private BigDecimal discountValue = BigDecimal.ZERO;

    @Size(max = 100)
    private String paymentReference;

    @NotEmpty(message = "Cart is empty")
    @Valid
    private List<CartItemRequest> items = new ArrayList<>();
}
