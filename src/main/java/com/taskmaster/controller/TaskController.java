package com.taskmaster.controller;

import com.taskmaster.dto.*;
import com.taskmaster.model.Attachment;
import com.taskmaster.security.UserPrincipal;
import com.taskmaster.service.StorageService;
import com.taskmaster.service.TaskService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import java.util.List;

@RestController
@RequestMapping("/api/tasks")
public class TaskController {

    @Autowired
    private TaskService taskService;

    @Autowired
    private StorageService fileStorageService;

    @PostMapping
    public ResponseEntity<TaskDto> createTask(
            @Valid @RequestBody TaskRequest taskRequest,
            @AuthenticationPrincipal UserPrincipal currentUser) {
        TaskDto taskDto = taskService.createTask(taskRequest, currentUser.getId());
        return new ResponseEntity<>(taskDto, HttpStatus.CREATED);
    }

    @GetMapping
    public ResponseEntity<List<TaskDto>> getTasks(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) Long teamId,
            @RequestParam(required = false) Long assignedToId,
            @RequestParam(defaultValue = "dueDate") String sortBy,
            @RequestParam(defaultValue = "asc") String sortDir,
            @AuthenticationPrincipal UserPrincipal currentUser) {
        List<TaskDto> tasks = taskService.getAllTasks(keyword, status, teamId, assignedToId, sortBy, sortDir, currentUser.getId());
        return ResponseEntity.ok(tasks);
    }

    @GetMapping("/{taskId}")
    public ResponseEntity<TaskDto> getTaskById(
            @PathVariable Long taskId,
            @AuthenticationPrincipal UserPrincipal currentUser) {
        TaskDto taskDto = taskService.getTaskById(taskId, currentUser.getId());
        return ResponseEntity.ok(taskDto);
    }

    @PutMapping("/{taskId}")
    public ResponseEntity<TaskDto> updateTask(
            @PathVariable Long taskId,
            @Valid @RequestBody TaskRequest taskRequest,
            @AuthenticationPrincipal UserPrincipal currentUser) {
        TaskDto taskDto = taskService.updateTask(taskId, taskRequest, currentUser.getId());
        return ResponseEntity.ok(taskDto);
    }

    @DeleteMapping("/{taskId}")
    public ResponseEntity<Void> deleteTask(
            @PathVariable Long taskId,
            @AuthenticationPrincipal UserPrincipal currentUser) {
        taskService.deleteTask(taskId, currentUser.getId());
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{taskId}/comments")
    public ResponseEntity<CommentDto> addComment(
            @PathVariable Long taskId,
            @Valid @RequestBody CommentRequest commentRequest,
            @AuthenticationPrincipal UserPrincipal currentUser) {
        CommentDto commentDto = taskService.addComment(taskId, commentRequest, currentUser.getId());
        return new ResponseEntity<>(commentDto, HttpStatus.CREATED);
    }

    @PostMapping("/{taskId}/attachments")
    public ResponseEntity<AttachmentDto> addAttachment(
            @PathVariable Long taskId,
            @RequestParam("file") MultipartFile file,
            @AuthenticationPrincipal UserPrincipal currentUser) {
        AttachmentDto attachmentDto = taskService.addAttachment(taskId, file, currentUser.getId());
        return new ResponseEntity<>(attachmentDto, HttpStatus.CREATED);
    }

    @GetMapping("/attachments/download/{attachmentId}")
    public ResponseEntity<Resource> downloadAttachment(
            @PathVariable Long attachmentId,
            @AuthenticationPrincipal UserPrincipal currentUser,
            HttpServletRequest request) {
        
        Attachment attachment = taskService.getAttachment(attachmentId, currentUser.getId());
        Resource resource = fileStorageService.loadFileAsResource(attachment.getFilePath());

        String contentType = attachment.getContentType();
        if (contentType == null) {
            contentType = "application/octet-stream";
        }

        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(contentType))
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + attachment.getFileName() + "\"")
                .body(resource);
    }
}
