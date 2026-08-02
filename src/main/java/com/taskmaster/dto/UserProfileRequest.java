package com.taskmaster.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Size;

public record UserProfileRequest(
    @Size(min = 3, max = 20) String username,
    @Email @Size(max = 50) String email,
    @Size(min = 6, max = 100) String password
) {}
