package com.briefix.ai.dto;

public record AiLetterResponse(
        String title,
        String content,
        boolean success
) {}
