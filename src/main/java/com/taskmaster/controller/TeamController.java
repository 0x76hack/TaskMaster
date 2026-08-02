package com.taskmaster.controller;

import com.taskmaster.dto.AddMemberRequest;
import com.taskmaster.dto.TeamDto;
import com.taskmaster.dto.TeamRequest;
import com.taskmaster.security.UserPrincipal;
import com.taskmaster.service.TeamService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/api/teams")
public class TeamController {

    @Autowired
    private TeamService teamService;

    @PostMapping
    public ResponseEntity<TeamDto> createTeam(
            @Valid @RequestBody TeamRequest teamRequest,
            @AuthenticationPrincipal UserPrincipal currentUser) {
        TeamDto teamDto = teamService.createTeam(teamRequest, currentUser.getId());
        return new ResponseEntity<>(teamDto, HttpStatus.CREATED);
    }

    @GetMapping
    public ResponseEntity<List<TeamDto>> getUserTeams(@AuthenticationPrincipal UserPrincipal currentUser) {
        List<TeamDto> teams = teamService.getUserTeams(currentUser.getId());
        return ResponseEntity.ok(teams);
    }

    @PostMapping("/{teamId}/members")
    public ResponseEntity<TeamDto> addMember(
            @PathVariable Long teamId,
            @Valid @RequestBody AddMemberRequest addMemberRequest,
            @AuthenticationPrincipal UserPrincipal currentUser) {
        TeamDto teamDto = teamService.addMember(teamId, addMemberRequest.emailOrUsername(), currentUser.getId());
        return ResponseEntity.ok(teamDto);
    }
}
