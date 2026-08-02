package com.taskmaster.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.taskmaster.dto.AddMemberRequest;
import com.taskmaster.dto.TeamDto;
import com.taskmaster.dto.TeamRequest;
import com.taskmaster.dto.UserDto;
import com.taskmaster.exception.BadRequestException;
import com.taskmaster.security.CustomUserDetailsService;
import com.taskmaster.security.JwtAuthenticationEntryPoint;
import com.taskmaster.security.UserPrincipal;
import com.taskmaster.service.TeamService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.web.servlet.MockMvc;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.Set;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(TeamController.class)
@AutoConfigureMockMvc(addFilters = false)
public class TeamControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private TeamService teamService;

    @MockBean
    private CustomUserDetailsService customUserDetailsService;

    @MockBean
    private JwtAuthenticationEntryPoint jwtAuthenticationEntryPoint;

    @BeforeEach
    public void setup() {
        UserPrincipal principal = new UserPrincipal(1L, "testuser", "test@example.com", "password", Collections.emptyList());
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(principal, null, principal.getAuthorities())
        );
    }

    @Test
    public void createTeam_Success() throws Exception {
        TeamRequest request = new TeamRequest("Development", "Backend work");
        UserDto creatorDto = new UserDto(1L, "testuser", "test@example.com", LocalDateTime.now());
        TeamDto teamDto = new TeamDto(1L, "Development", "Backend work", creatorDto, Set.of(creatorDto), LocalDateTime.now());

        Mockito.when(teamService.createTeam(any(TeamRequest.class), eq(1L))).thenReturn(teamDto);

        mockMvc.perform(post("/api/teams")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.name").value("Development"))
                .andExpect(jsonPath("$.description").value("Backend work"));
    }

    @Test
    public void addMember_Success() throws Exception {
        AddMemberRequest request = new AddMemberRequest("otheruser");
        UserDto creatorDto = new UserDto(1L, "testuser", "test@example.com", LocalDateTime.now());
        UserDto memberDto = new UserDto(2L, "otheruser", "other@example.com", LocalDateTime.now());
        TeamDto teamDto = new TeamDto(1L, "Development", "Backend work", creatorDto, Set.of(creatorDto, memberDto), LocalDateTime.now());

        Mockito.when(teamService.addMember(eq(1L), eq("otheruser"), eq(1L))).thenReturn(teamDto);

        mockMvc.perform(post("/api/teams/1/members")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.members[?(@.username == 'otheruser')]").exists());
    }

    @Test
    public void addMember_DuplicateMember() throws Exception {
        AddMemberRequest request = new AddMemberRequest("otheruser");

        Mockito.when(teamService.addMember(eq(1L), eq("otheruser"), eq(1L)))
                .thenThrow(new BadRequestException("User is already a member of this team."));

        mockMvc.perform(post("/api/teams/1/members")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("User is already a member of this team."));
    }
}
