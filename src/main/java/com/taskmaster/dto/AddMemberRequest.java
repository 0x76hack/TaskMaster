package com.taskmaster.dto;

import jakarta.validation.constraints.NotBlank;

public record AddMemberRequest(
    @NotBlank String emailOrUsername
) {}
