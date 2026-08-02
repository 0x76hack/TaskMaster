package com.taskmaster.controller;

import com.taskmaster.dto.AiSuggestionRequest;
import com.taskmaster.service.GenerativeAiService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.Map;

@RestController
@RequestMapping("/api/tasks/ai")
public class AiController {

    @Autowired
    private GenerativeAiService generativeAiService;

    @PostMapping("/suggest")
    public ResponseEntity<Map<String, String>> suggestDescription(
            @Valid @RequestBody AiSuggestionRequest request) {
        String description = generativeAiService.generateDescription(request.title(), request.context());
        return ResponseEntity.ok(Map.of("description", description));
    }
}
