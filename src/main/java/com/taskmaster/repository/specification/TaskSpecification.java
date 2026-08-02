package com.taskmaster.repository.specification;

import com.taskmaster.model.Task;
import com.taskmaster.model.TaskStatus;
import org.springframework.data.jpa.domain.Specification;

public class TaskSpecification {

    public static Specification<Task> hasStatus(TaskStatus status) {
        return (root, query, cb) -> status == null ? cb.conjunction() : cb.equal(root.get("status"), status);
    }

    public static Specification<Task> isAssignedTo(Long userId) {
        return (root, query, cb) -> userId == null ? cb.conjunction() : cb.equal(root.get("assignedTo").get("id"), userId);
    }

    public static Specification<Task> belongsToTeam(Long teamId) {
        return (root, query, cb) -> teamId == null ? cb.conjunction() : cb.equal(root.get("team").get("id"), teamId);
    }

    public static Specification<Task> searchByKeyword(String keyword) {
        return (root, query, cb) -> {
            if (keyword == null || keyword.isBlank()) {
                return cb.conjunction();
            }
            String likeKeyword = "%" + keyword.toLowerCase() + "%";
            return cb.or(
                    cb.like(cb.lower(root.get("title")), likeKeyword),
                    cb.like(cb.lower(root.get("description")), likeKeyword)
            );
        };
    }
}
