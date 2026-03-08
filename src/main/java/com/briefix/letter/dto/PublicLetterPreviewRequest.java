package com.briefix.letter.dto;

import jakarta.validation.constraints.NotBlank;

import java.time.LocalDate;

/**
 * Inbound DTO for the unauthenticated public letter-preview endpoint.
 *
 * <p>Accepts manual sender and recipient details together with the letter
 * content. No profile or contact IDs are accepted — guest users enter all
 * address data directly. The template is always forced to {@code CLASSIC}
 * by the service layer.</p>
 *
 * <p>Rate-limited to 3 requests per IP per day via {@link com.briefix.security.RateLimitFilter}.</p>
 *
 * <p><strong>Sender fields</strong> ({@code senderType} … {@code senderContactPerson}):
 * provide the sender's identity and address. {@code senderType} must be either
 * {@code "INDIVIDUAL"} or {@code "ORGANIZATION"}; defaults to {@code "INDIVIDUAL"} if
 * absent. {@code senderPostalCode} and {@code senderCity} are required ({@code @NotBlank}).
 * All other sender fields are optional.</p>
 *
 * <p><strong>Recipient fields</strong> ({@code recipientType} … {@code recipientCountry}):
 * provide the recipient's identity and address. {@code recipientType} follows the same
 * convention as {@code senderType}. All recipient fields are optional; missing address
 * components simply produce gaps in the rendered letter.</p>
 *
 * <p><strong>Content fields</strong>: {@code title} and {@code body} are required
 * ({@code @NotBlank}). {@code letterDate} is optional; the current date is used when
 * {@code null}.</p>
 *
 * <p>This record is immutable and safe for concurrent use across threads.</p>
 */
public record PublicLetterPreviewRequest(

        // ── Sender ────────────────────────────────────────────────────────────
        String senderType,           // INDIVIDUAL | ORGANIZATION
        String senderSalutation,
        String senderTitle,
        String senderFirstName,
        String senderLastName,
        String senderCompanyName,
        String senderDepartment,
        String senderContactPerson,
        String senderStreet,
        String senderStreetNumber,
        @NotBlank String senderPostalCode,
        @NotBlank String senderCity,
        String senderCountry,
        String senderEmail,
        String senderPhone,

        // ── Recipient ─────────────────────────────────────────────────────────
        String recipientType,        // INDIVIDUAL | ORGANIZATION
        String recipientSalutation,
        String recipientFirstName,
        String recipientLastName,
        String recipientCompany,
        String recipientDepartment,
        String recipientContactPerson,
        String recipientContactPersonSalutation,
        String recipientStreet,
        String recipientStreetNumber,
        String recipientPostalCode,
        String recipientCity,
        String recipientCountry,

        // ── Content ───────────────────────────────────────────────────────────
        @NotBlank String title,
        @NotBlank String body,
        LocalDate letterDate

) {}
