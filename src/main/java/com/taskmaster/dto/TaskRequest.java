package com.taskmaster.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;

public record TaskRequest(
    @NotBlank @Size(max = 100) String title,
    @Size(max = 1000) String description,
    LocalDate dueDate,
    String status,
    Long assignedToId,
    Long teamId
) {}
