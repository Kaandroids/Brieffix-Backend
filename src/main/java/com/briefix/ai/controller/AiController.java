package com.briefix.ai.controller;

import com.briefix.ai.dto.AiGenerateRequest;
import com.briefix.ai.dto.AiLetterResponse;
import com.briefix.ai.service.AiService;
import jakarta.validation.Valid;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/ai")
public class AiController {

    private final AiService aiService;

    public AiController(AiService aiService) {
        this.aiService = aiService;
    }

    @PostMapping("/generate-letter")
    public AiLetterResponse generateLetter(@Valid @RequestBody AiGenerateRequest request,
                                           Authentication authentication) {
        return aiService.generateLetter(request, authentication.getName());
    }
}
