package com.taskmaster.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record SignUpRequest(
    @NotBlank @Size(min = 3, max = 20) String username,
    @NotBlank @Email @Size(max = 50) String email,
    @NotBlank @Size(min = 6, max = 100) String password
) {}
