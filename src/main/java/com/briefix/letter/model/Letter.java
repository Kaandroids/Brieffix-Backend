package com.briefix.letter.model;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Immutable domain model representing a generated letter in the Briefix
 * application.
 *
 * <p>A {@code Letter} is the core domain object produced when a user generates
 * a formal letter.  It captures all the data needed to reproduce or re-render
 * the letter at any point in time:</p>
 * <ul>
 *   <li>The letter's textual content ({@code title} and {@code body}).</li>
 *   <li>The date printed on the letter ({@code letterDate}).</li>
 *   <li>An immutable {@link SenderSnapshot} containing the sender's profile
 *       data as it existed at generation time.</li>
 *   <li>An immutable {@link RecipientSnapshot} containing the recipient's
 *       address data as it existed at generation time.</li>
 *   <li>The {@link LetterTemplate} that governs the visual design used when
 *       rendering the PDF.</li>
 *   <li>An optional URL ({@code pdfUrl}) pointing to a pre-generated PDF
 *       file, if the letter has been exported to storage.</li>
 * </ul>
 *
 * <p>This record is the canonical in-memory representation exchanged between
 * the service and repository layers.  It is distinct from {@code LetterEntity}
 * (the JPA persistence model) and {@code LetterDto} (the API response model).</p>
 *
 * <p><strong>Thread safety:</strong> instances are immutable Java records and
 * therefore inherently thread-safe.</p>
 *
 * @param id                unique identifier of the letter; {@code null} for a
 *                          letter that has not yet been persisted
 * @param userId            identifier of the user who owns this letter; used
 *                          for ownership validation and scoped queries
 * @param title             user-supplied subject or title of the letter; used
 *                          in the letter header and as the PDF file name
 * @param body              full text body of the letter as entered by the user;
 *                          stored as plain text and rendered into the selected
 *                          template when generating the PDF
 * @param letterDate        the date printed on the letter; defaults to
 *                          {@link java.time.LocalDate#now()} if not explicitly
 *                          provided in the generation request
 * @param senderSnapshot    immutable snapshot of the sender's profile data at
 *                          the time the letter was generated; never {@code null}
 * @param recipientSnapshot immutable snapshot of the recipient's address data
 *                          at the time the letter was generated; never
 *                          {@code null}
 * @param template          the visual design template applied when rendering
 *                          the letter to PDF; never {@code null}
 * @param pdfUrl            URL of the pre-generated PDF file in external
 *                          storage; {@code null} if the PDF has not been stored
 *                          externally (the PDF is rendered on-the-fly instead)
 * @param createdAt         timestamp recording when the letter was first
 *                          persisted; set automatically on insert and never
 *                          modified thereafter
 */
public record Letter(
        UUID id,
        UUID userId,
        String title,
        String body,
        LocalDate letterDate,
        SenderSnapshot senderSnapshot,
        RecipientSnapshot recipientSnapshot,
        LetterTemplate template,
        String pdfUrl,
        LocalDateTime createdAt,
        UUID profileId
) {}
