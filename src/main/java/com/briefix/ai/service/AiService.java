package com.briefix.ai.service;

import com.briefix.ai.dto.AiGenerateRequest;
import com.briefix.ai.dto.AiLetterResponse;

public interface AiService {
    AiLetterResponse generateLetter(AiGenerateRequest request, String userEmail);
}
