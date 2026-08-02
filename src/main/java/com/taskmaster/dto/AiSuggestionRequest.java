package com.taskmaster.dto;

import jakarta.validation.constraints.NotBlank;

public record AiSuggestionRequest(
    @NotBlank String title,
    String context
) {}
