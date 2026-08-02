package com.taskmaster.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.taskmaster.dto.*;
import com.taskmaster.exception.BadRequestException;
import com.taskmaster.model.TaskStatus;
import com.taskmaster.security.CustomUserDetailsService;
import com.taskmaster.security.JwtAuthenticationEntryPoint;
import com.taskmaster.security.UserPrincipal;
import com.taskmaster.service.FileStorageService;
import com.taskmaster.service.TaskService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.web.servlet.MockMvc;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(TaskController.class)
@AutoConfigureMockMvc(addFilters = false)
public class TaskControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private TaskService taskService;

    @MockBean
    private FileStorageService fileStorageService;

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
    public void createTask_Success() throws Exception {
        TaskRequest request = new TaskRequest("Test Task", "Task description", LocalDate.now(), "OPEN", null, null);
        UserDto creator = new UserDto(1L, "testuser", "test@example.com", LocalDateTime.now());
        TaskDto taskDto = new TaskDto(1L, "Test Task", "Task description", LocalDate.now(), TaskStatus.OPEN, null, null, creator, LocalDateTime.now(), LocalDateTime.now(), List.of(), List.of());

        Mockito.when(taskService.createTask(any(TaskRequest.class), eq(1L))).thenReturn(taskDto);

        mockMvc.perform(post("/api/tasks")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.title").value("Test Task"))
                .andExpect(jsonPath("$.status").value("OPEN"));
    }

    @Test
    public void getTaskById_ForbiddenAccess() throws Exception {
        Mockito.when(taskService.getTaskById(eq(99L), eq(1L)))
                .thenThrow(new AccessDeniedException("You do not have access to tasks in this team."));

        mockMvc.perform(get("/api/tasks/99"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.error").value("Forbidden"))
                .andExpect(jsonPath("$.message").value("You do not have access to tasks in this team."));
    }

    @Test
    public void deleteTask_Unauthorized() throws Exception {
        Mockito.doThrow(new AccessDeniedException("Only the creator of the task can delete it."))
                .when(taskService).deleteTask(eq(1L), eq(1L));

        mockMvc.perform(delete("/api/tasks/1"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.error").value("Forbidden"))
                .andExpect(jsonPath("$.message").value("Only the creator of the task can delete it."));
    }

    @Test
    public void addComment_Success() throws Exception {
        CommentRequest request = new CommentRequest("This is a comment");
        UserDto commenter = new UserDto(1L, "testuser", "test@example.com", LocalDateTime.now());
        CommentDto commentDto = new CommentDto(1L, "This is a comment", commenter, LocalDateTime.now());

        Mockito.when(taskService.addComment(eq(1L), any(CommentRequest.class), eq(1L))).thenReturn(commentDto);

        mockMvc.perform(post("/api/tasks/1/comments")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.content").value("This is a comment"));
    }

    @Test
    public void addAttachment_Success() throws Exception {
        MockMultipartFile file = new MockMultipartFile("file", "test.txt", MediaType.TEXT_PLAIN_VALUE, "File contents".getBytes());
        UserDto uploader = new UserDto(1L, "testuser", "test@example.com", LocalDateTime.now());
        AttachmentDto attachmentDto = new AttachmentDto(1L, "test.txt", "/api/tasks/attachments/download/1", MediaType.TEXT_PLAIN_VALUE, uploader, LocalDateTime.now());

        Mockito.when(taskService.addAttachment(eq(1L), any(), eq(1L))).thenReturn(attachmentDto);

        mockMvc.perform(multipart("/api/tasks/1/attachments").file(file))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.fileName").value("test.txt"))
                .andExpect(jsonPath("$.fileUrl").value("/api/tasks/attachments/download/1"));
    }

    @Test
    public void addAttachment_EmptyFile() throws Exception {
        MockMultipartFile file = new MockMultipartFile("file", "test.txt", MediaType.TEXT_PLAIN_VALUE, new byte[0]);

        Mockito.when(taskService.addAttachment(eq(1L), any(), eq(1L)))
                .thenThrow(new BadRequestException("Cannot upload empty file."));

        mockMvc.perform(multipart("/api/tasks/1/attachments").file(file))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Cannot upload empty file."));
    }
}
