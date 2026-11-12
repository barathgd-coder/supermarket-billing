package com.supermart.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class SupplierForm {
    private Long id;

    @NotBlank(message = "Supplier name is required")
    @Size(max = 120, message = "Name is too long")
    private String name;

    @Pattern(regexp = "^$|^[0-9+\\- ]{7,15}$", message = "Enter a valid contact number")
    private String contactNumber;

    @Email(message = "Enter a valid email address")
    @Size(max = 120)
    private String email;

    @Size(max = 255)
    private String address;
}
