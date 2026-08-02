package com.taskmaster.dto;

import com.taskmaster.model.Team;
import java.time.LocalDateTime;
import java.util.Set;
import java.util.stream.Collectors;

public record TeamDto(
    Long id,
    String name,
    String description,
    UserDto createdBy,
    Set<UserDto> members,
    LocalDateTime createdAt
) {
    public static TeamDto fromEntity(Team team) {
        if (team == null) return null;
        return new TeamDto(
            team.getId(),
            team.getName(),
            team.getDescription(),
            UserDto.fromEntity(team.getCreatedBy()),
            team.getMembers().stream().map(UserDto::fromEntity).collect(Collectors.toSet()),
            team.getCreatedAt()
        );
    }
}
