package com.taskmaster.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

@Service
public class NotificationService {

    @Autowired
    private SimpMessagingTemplate messagingTemplate;

    public void notifyUser(String username, String message) {
        messagingTemplate.convertAndSendToUser(username, "/queue/notifications", message);
    }

    public void broadcastTaskUpdate(Long taskId, String message) {
        messagingTemplate.convertAndSend("/topic/tasks/" + taskId, message);
    }
}
