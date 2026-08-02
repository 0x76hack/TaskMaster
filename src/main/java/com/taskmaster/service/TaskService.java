package com.taskmaster.service;

import com.taskmaster.dto.AttachmentDto;
import com.taskmaster.dto.CommentDto;
import com.taskmaster.dto.CommentRequest;
import com.taskmaster.dto.TaskDto;
import com.taskmaster.dto.TaskRequest;
import com.taskmaster.exception.BadRequestException;
import com.taskmaster.exception.ResourceNotFoundException;
import com.taskmaster.model.*;
import org.springframework.security.access.AccessDeniedException;
import com.taskmaster.repository.*;
import com.taskmaster.repository.specification.TaskSpecification;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class TaskService {

    @Autowired
    private TaskRepository taskRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private TeamRepository teamRepository;

    @Autowired
    private CommentRepository commentRepository;

    @Autowired
    private AttachmentRepository attachmentRepository;

    @Autowired
    private StorageService fileStorageService;

    @Autowired
    private NotificationService notificationService;

    @Transactional
    public TaskDto createTask(TaskRequest request, Long creatorId) {
        User creator = userRepository.findById(creatorId)
                .orElseThrow(() -> new ResourceNotFoundException("User", "id", creatorId));

        Task.TaskBuilder taskBuilder = Task.builder()
                .title(request.title())
                .description(request.description())
                .dueDate(request.dueDate())
                .createdBy(creator)
                .status(TaskStatus.OPEN);

        if (request.teamId() != null) {
            Team team = teamRepository.findById(request.teamId())
                    .orElseThrow(() -> new ResourceNotFoundException("Team", "id", request.teamId()));
            if (!team.getMembers().contains(creator)) {
                throw new BadRequestException("You must be a member of the team to create tasks in it.");
            }
            taskBuilder.team(team);
        }

        if (request.assignedToId() != null) {
            User assignee = userRepository.findById(request.assignedToId())
                    .orElseThrow(() -> new ResourceNotFoundException("User", "id", request.assignedToId()));
            
            // If team is present, verify assignee is also in the team
            if (request.teamId() != null) {
                Team team = teamRepository.findById(request.teamId()).orElse(null);
                if (team != null && !team.getMembers().contains(assignee)) {
                    throw new BadRequestException("Assignee must be a member of the task's team.");
                }
            }
            taskBuilder.assignedTo(assignee);
        }

        Task task = taskBuilder.build();
        Task savedTask = taskRepository.save(task);

        // Notify Assignee
        if (task.getAssignedTo() != null) {
            notificationService.notifyUser(
                task.getAssignedTo().getUsername(),
                "You have been assigned a new task: " + task.getTitle()
            );
        }

        return TaskDto.fromEntity(savedTask);
    }

    @Transactional
    public TaskDto updateTask(Long taskId, TaskRequest request, Long userId) {
        Task task = taskRepository.findById(taskId)
                .orElseThrow(() -> new ResourceNotFoundException("Task", "id", taskId));

        verifyTaskAccess(task, userId);

        task.setTitle(request.title());
        task.setDescription(request.description());
        task.setDueDate(request.dueDate());

        if (request.status() != null) {
            try {
                task.setStatus(TaskStatus.valueOf(request.status().toUpperCase()));
            } catch (IllegalArgumentException e) {
                throw new BadRequestException("Invalid status. Supported values: OPEN, IN_PROGRESS, COMPLETED");
            }
        }

        if (request.assignedToId() != null) {
            User assignee = userRepository.findById(request.assignedToId())
                    .orElseThrow(() -> new ResourceNotFoundException("User", "id", request.assignedToId()));

            if (task.getTeam() != null && !task.getTeam().getMembers().contains(assignee)) {
                throw new BadRequestException("Assignee must be a member of the task's team.");
            }
            
            // Notify if assignment changed
            if (task.getAssignedTo() == null || !task.getAssignedTo().getId().equals(assignee.getId())) {
                notificationService.notifyUser(
                    assignee.getUsername(),
                    "You have been assigned to task: " + task.getTitle()
                );
            }
            task.setAssignedTo(assignee);
        } else {
            task.setAssignedTo(null);
        }

        Task updatedTask = taskRepository.save(task);

        // Broadcast update to anyone watching the task
        notificationService.broadcastTaskUpdate(taskId, "Task '" + task.getTitle() + "' was updated.");

        return TaskDto.fromEntity(updatedTask);
    }

    @Transactional
    public void deleteTask(Long taskId, Long userId) {
        Task task = taskRepository.findById(taskId)
                .orElseThrow(() -> new ResourceNotFoundException("Task", "id", taskId));

        // Only the creator can delete a task
        if (!task.getCreatedBy().getId().equals(userId)) {
            throw new AccessDeniedException("Only the creator of the task can delete it.");
        }

        taskRepository.delete(task);
    }

    @Transactional(readOnly = true)
    public TaskDto getTaskById(Long taskId, Long userId) {
        Task task = taskRepository.findById(taskId)
                .orElseThrow(() -> new ResourceNotFoundException("Task", "id", taskId));

        verifyTaskAccess(task, userId);
        return TaskDto.fromEntity(task);
    }

    @Transactional(readOnly = true)
    public List<TaskDto> getAllTasks(String keyword, String status, Long teamId, Long assignedToId,
                                      String sortBy, String sortDir, Long userId) {
        // Enforce security: get user's teams
        List<Long> userTeamIds = teamRepository.findByMembers_Id(userId).stream()
                .map(Team::getId)
                .collect(Collectors.toList());

        Specification<Task> securitySpec = (root, query, cb) -> {
            // Task has a team that user is in
            var inTeamPredicate = root.get("team").get("id").in(userTeamIds);
            
            // Or Task is personal and user is creator or assignee
            var noTeamPredicate = cb.isNull(root.get("team"));
            var isCreatorPredicate = cb.equal(root.get("createdBy").get("id"), userId);
            var isAssigneePredicate = cb.equal(root.get("assignedTo").get("id"), userId);
            var personalAccessPredicate = cb.and(noTeamPredicate, cb.or(isCreatorPredicate, isAssigneePredicate));

            return cb.or(inTeamPredicate, personalAccessPredicate);
        };

        Specification<Task> searchSpec = Specification.where(securitySpec);

        if (status != null && !status.isBlank()) {
            try {
                searchSpec = searchSpec.and(TaskSpecification.hasStatus(TaskStatus.valueOf(status.toUpperCase())));
            } catch (IllegalArgumentException e) {
                throw new BadRequestException("Invalid status filter. Supported values: OPEN, IN_PROGRESS, COMPLETED");
            }
        }

        if (teamId != null) {
            if (!userTeamIds.contains(teamId)) {
                throw new BadRequestException("You do not have access to tasks of this team.");
            }
            searchSpec = searchSpec.and(TaskSpecification.belongsToTeam(teamId));
        }

        if (assignedToId != null) {
            searchSpec = searchSpec.and(TaskSpecification.isAssignedTo(assignedToId));
        }

        if (keyword != null && !keyword.isBlank()) {
            searchSpec = searchSpec.and(TaskSpecification.searchByKeyword(keyword));
        }

        Sort sort = Sort.by(Sort.Direction.fromString(sortDir != null ? sortDir : "ASC"), sortBy != null ? sortBy : "dueDate");
        List<Task> tasks = taskRepository.findAll(searchSpec, sort);

        return tasks.stream().map(TaskDto::fromEntity).collect(Collectors.toList());
    }

    @Transactional
    public CommentDto addComment(Long taskId, CommentRequest request, Long userId) {
        Task task = taskRepository.findById(taskId)
                .orElseThrow(() -> new ResourceNotFoundException("Task", "id", taskId));

        verifyTaskAccess(task, userId);

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User", "id", userId));

        Comment comment = Comment.builder()
                .content(request.content())
                .task(task)
                .createdBy(user)
                .build();

        Comment savedComment = commentRepository.save(comment);

        // Notify task assignee if someone else comments
        if (task.getAssignedTo() != null && !task.getAssignedTo().getId().equals(userId)) {
            notificationService.notifyUser(
                task.getAssignedTo().getUsername(),
                user.getUsername() + " commented on task: " + task.getTitle()
            );
        }

        notificationService.broadcastTaskUpdate(taskId, "New comment added to task '" + task.getTitle() + "'");

        return CommentDto.fromEntity(savedComment);
    }

    @Transactional
    public AttachmentDto addAttachment(Long taskId, MultipartFile file, Long userId) {
        Task task = taskRepository.findById(taskId)
                .orElseThrow(() -> new ResourceNotFoundException("Task", "id", taskId));

        verifyTaskAccess(task, userId);

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User", "id", userId));

        String fileName = fileStorageService.storeFile(file);

        Attachment attachment = Attachment.builder()
                .fileName(file.getOriginalFilename())
                .filePath(fileName)
                .contentType(file.getContentType())
                .task(task)
                .uploadedBy(user)
                .build();

        Attachment savedAttachment = attachmentRepository.save(attachment);

        notificationService.broadcastTaskUpdate(taskId, "Attachment added to task '" + task.getTitle() + "'");

        return AttachmentDto.fromEntity(savedAttachment);
    }

    @Transactional(readOnly = true)
    public Attachment getAttachment(Long attachmentId, Long userId) {
        Attachment attachment = attachmentRepository.findById(attachmentId)
                .orElseThrow(() -> new ResourceNotFoundException("Attachment", "id", attachmentId));

        verifyTaskAccess(attachment.getTask(), userId);
        return attachment;
    }

    private void verifyTaskAccess(Task task, Long userId) {
        if (task.getTeam() != null) {
            // Check if user belongs to the team
            boolean isMember = teamRepository.findByMembers_Id(userId).stream()
                    .anyMatch(team -> team.getId().equals(task.getTeam().getId()));
            if (!isMember) {
                throw new AccessDeniedException("You do not have access to tasks in this team.");
            }
        } else {
            // Personal task: creator or assignee
            boolean isCreator = task.getCreatedBy().getId().equals(userId);
            boolean isAssignee = task.getAssignedTo() != null && task.getAssignedTo().getId().equals(userId);
            if (!isCreator && !isAssignee) {
                throw new AccessDeniedException("You do not have access to this personal task.");
            }
        }
    }
}
