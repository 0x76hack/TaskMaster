package com.taskmaster.service;

import com.taskmaster.dto.TeamDto;
import com.taskmaster.dto.TeamRequest;
import com.taskmaster.exception.BadRequestException;
import com.taskmaster.exception.ResourceNotFoundException;
import com.taskmaster.model.Team;
import com.taskmaster.model.User;
import com.taskmaster.repository.TeamRepository;
import com.taskmaster.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class TeamService {

    @Autowired
    private TeamRepository teamRepository;

    @Autowired
    private UserRepository userRepository;

    @Transactional
    public TeamDto createTeam(TeamRequest request, Long creatorId) {
        User creator = userRepository.findById(creatorId)
                .orElseThrow(() -> new ResourceNotFoundException("User", "id", creatorId));

        Team team = Team.builder()
                .name(request.name())
                .description(request.description())
                .createdBy(creator)
                .build();
        
        team.getMembers().add(creator);
        Team savedTeam = teamRepository.save(team);
        return TeamDto.fromEntity(savedTeam);
    }

    @Transactional(readOnly = true)
    public List<TeamDto> getUserTeams(Long userId) {
        List<Team> teams = teamRepository.findByMembers_Id(userId);
        return teams.stream().map(TeamDto::fromEntity).collect(Collectors.toList());
    }

    @Transactional
    public TeamDto addMember(Long teamId, String memberEmailOrUsername, Long currentUserId) {
        Team team = teamRepository.findById(teamId)
                .orElseThrow(() -> new ResourceNotFoundException("Team", "id", teamId));

        User currentUser = userRepository.findById(currentUserId)
                .orElseThrow(() -> new ResourceNotFoundException("User", "id", currentUserId));

        if (!team.getMembers().contains(currentUser)) {
            throw new BadRequestException("Only team members can invite others to the team.");
        }

        User newMember = userRepository.findByEmail(memberEmailOrUsername)
                .orElseGet(() -> userRepository.findByUsername(memberEmailOrUsername)
                        .orElseThrow(() -> new ResourceNotFoundException("User", "email/username", memberEmailOrUsername)));

        if (team.getMembers().contains(newMember)) {
            throw new BadRequestException("User is already a member of this team.");
        }

        team.getMembers().add(newMember);
        Team savedTeam = teamRepository.save(team);
        return TeamDto.fromEntity(savedTeam);
    }
}
