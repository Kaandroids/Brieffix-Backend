package com.briefix.ai.dto;

/**
 * Outbound DTO carrying the result of an AI-assisted letter generation request.
 *
 * <p>This record is returned from {@code POST /api/v1/ai/generate-letter} and
 * contains the letter subject, body text, and a flag indicating whether generation
 * was successful. Consumers must check {@link #success()} before using the other
 * fields, as both {@code title} and {@code content} are {@code null} when the
 * generation fails.</p>
 *
 * <p>Generation is considered unsuccessful only when the provided description is
 * completely unintelligible (e.g., random characters). For brief or ambiguous
 * descriptions the model applies sensible defaults and returns {@code success: true}.</p>
 *
 * <p>This record is immutable and safe for concurrent use across threads.</p>
 *
 * @param title   the AI-generated subject line of the letter (the Betreff),
 *                without the "Betreff:" prefix; {@code null} when {@code success} is {@code false}
 * @param content the AI-generated body of the letter, starting from the salutation
 *                (e.g., "Sehr geehrte Damen und Herren,") through to the closing signature;
 *                excludes the address block, date, and subject line;
 *                {@code null} when {@code success} is {@code false}
 * @param success {@code true} if the model produced a valid letter;
 *                {@code false} if the description was unusable or the API call failed
 */
public record AiLetterResponse(
        String title,
        String content,
        boolean success
) {}
