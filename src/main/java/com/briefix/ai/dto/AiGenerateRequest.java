package com.briefix.ai.dto;

import jakarta.validation.constraints.NotBlank;

public record AiGenerateRequest(
        @NotBlank String description,
        String profileId,
        String contactId
) {}
