package com.briefix.ai.controller;

import com.briefix.ai.dto.AiGenerateRequest;
import com.briefix.ai.dto.AiLetterResponse;
import com.briefix.ai.service.AiServiceImpl;
import jakarta.validation.Valid;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * REST controller exposing AI-assisted letter generation endpoints under
 * {@code /api/v1/ai}.
 *
 * <p>This controller provides a single endpoint that accepts a natural-language
 * description of a desired letter and, optionally, references to a saved sender
 * profile and recipient contact. It delegates the actual content generation to
 * {@link AiService}, which submits the request to the configured Google Gemini
 * model and returns a structured title and body for the letter.</p>
 *
 * <p>All endpoints in this controller require an authenticated security context.
 * The authenticated principal's email address is extracted from the
 * {@link Authentication} object and passed to the service layer to scope
 * profile and contact lookups to the correct user account.</p>
 *
 * <p>Thread-safety: This class is a stateless Spring singleton and is safe
 * for concurrent use across multiple HTTP request threads.</p>
 */
@RestController
@RequestMapping("/api/v1/ai")
public class AiController {

    /**
     * The AI service delegate that orchestrates prompt construction and
     * Gemini API communication.
     */
    private final AiServiceImpl aiService;

    /**
     * Constructs an {@code AiController} with the required AI service dependency.
     *
     * @param aiService the service that handles AI letter generation; must not be {@code null}
     */
    public AiController(AiServiceImpl aiService) {
        this.aiService = aiService;
    }

    /**
     * Generates a letter title and body using the Google Gemini language model,
     * based on a natural-language description provided by the authenticated user.
     *
     * <p>HTTP Method: {@code POST}</p>
     * <p>Path: {@code /api/v1/ai/generate-letter}</p>
     * <p>Request body: A JSON object conforming to {@link AiGenerateRequest}.
     * The {@code description} field is required and must not be blank.</p>
     * <p>Response: {@code 200 OK} with an {@link AiLetterResponse} body containing
     * {@code title}, {@code content}, and {@code success} fields. When {@code success}
     * is {@code false}, {@code title} and {@code content} will be {@code null}.</p>
     *
     * @param request        the validated generation request bound from the JSON request body;
     *                       must not be {@code null}
     * @param authentication the Spring Security authentication token for the current request;
     *                       provides the caller's email address via {@link Authentication#getName()}
     * @return an {@link AiLetterResponse} containing the AI-generated letter subject and body,
     *         or a failure response if the description was unintelligible
     */
    @PostMapping("/generate-letter")
    public AiLetterResponse generateLetter(@Valid @RequestBody AiGenerateRequest request,
                                           Authentication authentication) {
        return aiService.generateLetter(request, authentication.getName());
    }
}
