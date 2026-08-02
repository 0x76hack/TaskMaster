package com.taskmaster.dto;

import com.taskmaster.model.User;
import java.time.LocalDateTime;

public record UserDto(
    Long id,
    String username,
    String email,
    LocalDateTime createdAt
) {
    public static UserDto fromEntity(User user) {
        if (user == null) return null;
        return new UserDto(user.getId(), user.getUsername(), user.getEmail(), user.getCreatedAt());
    }
}
