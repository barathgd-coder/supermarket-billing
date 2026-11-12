package com.supermart.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class UserForm {
    private Long id;

    @NotBlank(message = "Username is required")
    @Pattern(regexp = "^[A-Za-z0-9_.-]{3,30}$", message = "3-30 characters: letters, digits, dot, dash, underscore")
    private String username;

    @NotBlank(message = "Full name is required")
    @Size(max = 100)
    private String fullName;

    /** Required when creating a user; optional (blank = keep) when editing. */
    private String password;

    @NotBlank(message = "Select a role")
    private String roleName;

    private boolean enabled = true;
}
