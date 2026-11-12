package com.supermart.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class CustomerForm {
    private Long id;

    @NotBlank(message = "Customer name is required")
    @Size(max = 120, message = "Name is too long")
    private String name;

    @NotBlank(message = "Phone number is required")
    @Pattern(regexp = "^[0-9+\\- ]{7,15}$", message = "Enter a valid phone number (7-15 digits)")
    private String phone;
}
