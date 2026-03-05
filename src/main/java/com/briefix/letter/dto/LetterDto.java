package com.briefix.letter.dto;

import com.briefix.letter.model.LetterTemplate;
import com.briefix.letter.model.RecipientSnapshot;
import com.briefix.letter.model.SenderSnapshot;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Data Transfer Object representing a generated letter returned to API
 * consumers.
 *
 * <p>{@code LetterDto} is the read-facing representation of a
 * {@link com.briefix.letter.model.Letter} that is serialised to JSON and
 * returned in HTTP responses.  It carries the full set of fields a client
 * needs to display a letter summary or to re-render the letter, including the
 * frozen sender and recipient snapshots and the template selection.</p>
 *
 * <p>The {@link SenderSnapshot} and {@link RecipientSnapshot} fields are
 * included inline so that clients receive all the data they need to display
 * letter details without issuing additional API requests.</p>
 *
 * <p>Instances are produced exclusively by
 * {@link com.briefix.letter.mapper.LetterMapper#toDto(com.briefix.letter.model.Letter)}
 * and should never be created directly in service or controller code.</p>
 *
 * <p><strong>Thread safety:</strong> instances are immutable Java records and
 * therefore inherently thread-safe.</p>
 *
 * @param id                unique identifier of the letter
 * @param userId            identifier of the user who generated and owns this
 *                          letter; included to allow client-side ownership checks
 * @param title             subject or title of the letter as entered by the user
 * @param body              full text body of the letter
 * @param letterDate        the date printed on the letter
 * @param senderSnapshot    frozen snapshot of the sender's profile data at the
 *                          time the letter was generated
 * @param recipientSnapshot frozen snapshot of the recipient's address data at
 *                          the time the letter was generated
 * @param template          the visual design template applied when rendering the
 *                          letter to PDF
 * @param pdfUrl            URL of a pre-generated PDF stored in external
 *                          storage; {@code null} if not available
 * @param createdAt         timestamp at which the letter was first persisted in
 *                          the system
 */
public record LetterDto(
        UUID id,
        UUID userId,
        String title,
        String body,
        LocalDate letterDate,
        SenderSnapshot senderSnapshot,
        RecipientSnapshot recipientSnapshot,
        LetterTemplate template,
        String pdfUrl,
        LocalDateTime createdAt
) {}
