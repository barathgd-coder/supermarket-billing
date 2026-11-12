package com.supermart.dto;

import jakarta.validation.constraints.*;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class SettingsForm {
    @NotBlank(message = "Store name is required")
    @Size(max = 120)
    private String storeName;

    @Size(max = 250)
    private String address;

    @Pattern(regexp = "^$|^[0-9+\\- ]{7,15}$", message = "Enter a valid phone number")
    private String phone;

    @Size(max = 30)
    private String gstin;

    @NotNull(message = "Default GST % is required")
    @DecimalMin(value = "0", message = "GST cannot be negative")
    @DecimalMax(value = "100", message = "GST cannot exceed 100")
    private BigDecimal defaultGst;

    @NotNull(message = "Default reorder level is required")
    @Min(value = 0, message = "Cannot be negative")
    private Integer defaultReorderLevel;

    @NotBlank(message = "Invoice prefix is required")
    @Pattern(regexp = "^[A-Za-z0-9]{2,6}$", message = "2-6 letters or digits")
    private String invoicePrefix;

    @Size(max = 200)
    private String invoiceFooter;

    @NotBlank(message = "Currency symbol is required")
    @Size(max = 3, message = "At most 3 characters")
    private String currency;
}
