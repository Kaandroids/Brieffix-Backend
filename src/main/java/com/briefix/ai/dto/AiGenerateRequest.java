package com.briefix.ai.dto;

import jakarta.validation.constraints.NotBlank;

/**
 * Inbound DTO carrying the data required to trigger AI-assisted letter generation.
 *
 * <p>This record is deserialised from the JSON body of a
 * {@code POST /api/v1/ai/generate-letter} request. The {@code description} field
 * provides the natural-language prompt that is forwarded (along with optional sender
 * and recipient context) to the Google Gemini language model.</p>
 *
 * <p>If {@code profileId} is supplied, the service resolves the matching sender
 * profile and appends a summary of the sender's name and city to the Gemini prompt
 * to improve the relevance of the generated content. The same applies to
 * {@code contactId} for the recipient. Both are optional and may be {@code null}.</p>
 *
 * <p>This record is immutable and safe for concurrent use across threads.</p>
 *
 * @param description a natural-language description of the letter to generate
 *                    (e.g. "Write a formal complaint about a delayed delivery");
 *                    must not be blank
 * @param profileId   optional UUID string of the sender profile to use for contextual
 *                    enrichment; {@code null} if no profile should be included
 * @param contactId   optional UUID string of the recipient contact to use for contextual
 *                    enrichment; {@code null} if no contact should be included
 */
public record AiGenerateRequest(
        @NotBlank String description,
        String profileId,
        String contactId
) {}
