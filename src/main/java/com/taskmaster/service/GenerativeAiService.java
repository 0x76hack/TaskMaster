package com.taskmaster.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class GenerativeAiService {

    @Value("${app.gemini.api-key:}")
    private String geminiApiKey;

    private final RestTemplate restTemplate = new RestTemplate();

    public String generateDescription(String title, String context) {
        if (geminiApiKey == null || geminiApiKey.isBlank()) {
            return generateMockDescription(title, context);
        }

        try {
            String url = "https://generativelanguage.googleapis.com/v1beta/models/gemini-1.5-flash:generateContent?key=" + geminiApiKey;

            Map<String, Object> textPart = new HashMap<>();
            textPart.put("text", "Write a professional, structured task description (bullet points of subtasks and goals) for a task titled: \"" 
                    + title + "\". Additional context/input from user: \"" + context + "\". Keep the description concise and action-oriented.");

            Map<String, Object> contentObj = new HashMap<>();
            contentObj.put("parts", List.of(textPart));

            Map<String, Object> requestBody = new HashMap<>();
            requestBody.put("contents", List.of(contentObj));

            @SuppressWarnings("unchecked")
            Map<String, Object> response = restTemplate.postForObject(url, requestBody, Map.class);

            if (response != null && response.containsKey("candidates")) {
                List<?> candidates = (List<?>) response.get("candidates");
                if (!candidates.isEmpty()) {
                    Map<?, ?> candidate = (Map<?, ?>) candidates.get(0);
                    Map<?, ?> content = (Map<?, ?>) candidate.get("content");
                    List<?> parts = (List<?>) content.get("parts");
                    if (!parts.isEmpty()) {
                        Map<?, ?> part = (Map<?, ?>) parts.get(0);
                        return (String) part.get("text");
                    }
                }
            }
            return generateMockDescription(title, context);
        } catch (Exception e) {
            // Log error in production, fallback to mock
            return generateMockDescription(title, context);
        }
    }

    private String generateMockDescription(String title, String context) {
        StringBuilder sb = new StringBuilder();
        sb.append("Objective:\nSuccessfully complete the task: ").append(title).append("\n\n");
        sb.append("Scope / Tasks to Complete:\n");
        sb.append("- Define requirements and deliverables.\n");
        if (context != null && !context.isBlank()) {
            sb.append("- Address context: ").append(context).append("\n");
        }
        sb.append("- Verify completion and perform testing.\n- Review results with the team.");
        return sb.toString();
    }
}
