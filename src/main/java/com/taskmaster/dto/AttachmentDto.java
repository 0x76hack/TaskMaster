package com.taskmaster.dto;

import com.taskmaster.model.Attachment;
import java.time.LocalDateTime;

public record AttachmentDto(
    Long id,
    String fileName,
    String fileUrl,
    String contentType,
    UserDto uploadedBy,
    LocalDateTime uploadedAt
) {
    public static AttachmentDto fromEntity(Attachment attachment) {
        if (attachment == null) return null;
        String fileUrl = "/api/tasks/attachments/download/" + attachment.getId();
        return new AttachmentDto(
            attachment.getId(),
            attachment.getFileName(),
            fileUrl,
            attachment.getContentType(),
            UserDto.fromEntity(attachment.getUploadedBy()),
            attachment.getUploadedAt()
        );
    }
}
