package com.moveo.aicryptoadvisor.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record RegisterRequest(
        @NotBlank @Email String email,
        @NotBlank @Size(min = 1, max = 100) String name,
        @NotBlank
        @Pattern(
                regexp = "^(?=.*[A-Za-z])(?=.*\\d).{8,72}$",
                message = "Password must be 8-72 characters and contain at least one letter and one digit"
        )
        String password
) {
}
