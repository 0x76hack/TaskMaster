package com.taskmaster.dto;

import com.taskmaster.model.Comment;
import java.time.LocalDateTime;

public record CommentDto(
    Long id,
    String content,
    UserDto createdBy,
    LocalDateTime createdAt
) {
    public static CommentDto fromEntity(Comment comment) {
        if (comment == null) return null;
        return new CommentDto(
            comment.getId(),
            comment.getContent(),
            UserDto.fromEntity(comment.getCreatedBy()),
            comment.getCreatedAt()
        );
    }
}
