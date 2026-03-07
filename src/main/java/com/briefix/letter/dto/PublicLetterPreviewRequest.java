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
