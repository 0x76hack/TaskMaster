package com.taskmaster.dto;

import com.taskmaster.model.Task;
import com.taskmaster.model.TaskStatus;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

public record TaskDto(
    Long id,
    String title,
    String description,
    LocalDate dueDate,
    TaskStatus status,
    UserDto assignedTo,
    TeamDto team,
    UserDto createdBy,
    LocalDateTime createdAt,
    LocalDateTime updatedAt,
    List<CommentDto> comments,
    List<AttachmentDto> attachments
) {
    public static TaskDto fromEntity(Task task) {
        if (task == null) return null;
        
        TeamDto teamDto = null;
        if (task.getTeam() != null) {
            teamDto = new TeamDto(
                task.getTeam().getId(),
                task.getTeam().getName(),
                task.getTeam().getDescription(),
                UserDto.fromEntity(task.getTeam().getCreatedBy()),
                null,
                task.getTeam().getCreatedAt()
            );
        }

        return new TaskDto(
            task.getId(),
            task.getTitle(),
            task.getDescription(),
            task.getDueDate(),
            task.getStatus(),
            UserDto.fromEntity(task.getAssignedTo()),
            teamDto,
            UserDto.fromEntity(task.getCreatedBy()),
            task.getCreatedAt(),
            task.getUpdatedAt(),
            task.getComments() != null ? task.getComments().stream().map(CommentDto::fromEntity).collect(Collectors.toList()) : List.of(),
            task.getAttachments() != null ? task.getAttachments().stream().map(AttachmentDto::fromEntity).collect(Collectors.toList()) : List.of()
        );
    }
}
