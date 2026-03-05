package com.briefix.letter.service;

import com.briefix.letter.dto.GenerateLetterRequest;
import com.briefix.letter.dto.LetterDto;

import java.util.List;
import java.util.UUID;

/**
 * Service interface defining the business operations available for managing
 * letters in the Briefix application.
 *
 * <p>{@code LetterService} is the primary entry point for all letter-related
 * business logic.  It is responsible for:</p>
 * <ul>
 *   <li>Enforcing subscription plan access control — premium
 *       {@link com.briefix.letter.model.LetterTemplate}s may only be used by
 *       users with a PREMIUM plan.</li>
 *   <li>Resolving sender profiles and recipient contacts from their respective
 *       repositories and snapshotting their data into the letter at creation
 *       time.</li>
 *   <li>Rendering letter HTML via Thymeleaf and generating PDF bytes using the
 *       OpenHTMLToPDF library.</li>
 *   <li>Enforcing ownership — every operation that accesses or modifies a
 *       specific letter verifies that the authenticated user is the owner.</li>
 * </ul>
 *
 * <p>All methods accept the caller's email address as extracted from the JWT
 * or Spring Security context so that the controller layer does not need to
 * perform any user resolution itself.</p>
 *
 * <p><strong>Thread safety:</strong> implementations must be thread-safe.
 * The provided {@code LetterServiceImpl} is stateless (apart from injected
 * collaborators) and therefore thread-safe.</p>
 */
public interface LetterService {

    /**
     * Generates and returns a PDF preview of the letter described by the
     * request without persisting the letter to the database.
     *
     * <p>The preview uses the same rendering pipeline as letter creation, but
     * no {@code Letter} record is saved.  This allows users to inspect the
     * visual output of their content and template selection before committing.</p>
     *
     * @param req   the validated generation request containing the sender
     *              profile, recipient, content, and template selection; must
     *              not be {@code null}
     * @param email the email address of the authenticated user; used for user
     *              resolution and ownership verification of the profile and
     *              contact (if supplied)
     * @return the raw PDF byte array ready to be streamed to the client as an
     *         {@code application/pdf} response
     * @throws com.briefix.user.exception.UserNotFoundException       if no user
     *         with the given email exists
     * @throws com.briefix.profile.exception.ProfileNotFoundException if the
     *         specified profile does not exist
     * @throws org.springframework.security.access.AccessDeniedException if the
     *         profile or contact does not belong to the authenticated user
     * @throws com.briefix.letter.exception.PremiumRequiredException  if the
     *         selected template requires a PREMIUM plan that the user does not
     *         hold
     */
    byte[] preview(GenerateLetterRequest req, String email);

    /**
     * Generates a letter, persists it to the database, and returns the saved
     * representation as a DTO.
     *
     * <p>The sender snapshot and recipient snapshot are captured at the time of
     * this call, ensuring that the stored letter is not affected by future
     * changes to the referenced profile or contact.</p>
     *
     * @param req   the validated generation request; must not be {@code null}
     * @param email the email address of the authenticated user
     * @return the {@link LetterDto} representing the newly persisted letter,
     *         including the database-generated id and creation timestamp
     * @throws com.briefix.user.exception.UserNotFoundException       if no user
     *         with the given email exists
     * @throws com.briefix.profile.exception.ProfileNotFoundException if the
     *         specified profile does not exist
     * @throws org.springframework.security.access.AccessDeniedException if the
     *         profile or contact does not belong to the authenticated user
     * @throws com.briefix.letter.exception.PremiumRequiredException  if the
     *         selected template requires a PREMIUM plan the user does not hold
     */
    LetterDto create(GenerateLetterRequest req, String email);

    /**
     * Retrieves all letters belonging to the authenticated user.
     *
     * @param email the email address of the authenticated user; must not be
     *              {@code null}
     * @return an unmodifiable list of {@link LetterDto} objects owned by the
     *         user; empty list if the user has no letters
     * @throws com.briefix.user.exception.UserNotFoundException if no user with
     *         the given email exists
     */
    List<LetterDto> getMyLetters(String email);

    /**
     * Retrieves the raw PDF bytes and title for a specific letter, enforcing
     * ownership.
     *
     * <p>The PDF is rendered on-the-fly from the persisted snapshot data and
     * template selection each time this method is called.</p>
     *
     * @param id    the UUID of the letter whose PDF should be rendered; must
     *              not be {@code null}
     * @param email the email address of the authenticated user; used for
     *              ownership verification
     * @return a {@link PdfResult} containing the rendered PDF bytes and the
     *         letter's title (for use as the download file name)
     * @throws com.briefix.letter.exception.LetterNotFoundException  if no
     *         letter with the given id exists
     * @throws org.springframework.security.access.AccessDeniedException if the
     *         authenticated user does not own the letter
     */
    PdfResult getPdf(UUID id, String email);

    /**
     * Permanently deletes the letter identified by the given id, enforcing
     * ownership.
     *
     * @param id    the UUID of the letter to delete; must not be {@code null}
     * @param email the email address of the authenticated user; used for
     *              ownership verification before deletion
     * @throws com.briefix.letter.exception.LetterNotFoundException  if no
     *         letter with the given id exists
     * @throws org.springframework.security.access.AccessDeniedException if the
     *         authenticated user does not own the letter
     */
    void delete(UUID id, String email);

    /**
     * Value object returned by {@link #getPdf(UUID, String)} bundling the
     * rendered PDF bytes together with the letter's title.
     *
     * <p>The {@code title} is provided so that the controller can construct a
     * sensible {@code Content-Disposition} filename for the HTTP response
     * without needing to load the letter a second time.</p>
     *
     * @param bytes the raw bytes of the rendered PDF; never {@code null} or
     *              empty for a successfully rendered document
     * @param title the subject or title of the letter; used by the controller
     *              to build the PDF download file name
     */
    record PdfResult(byte[] bytes, String title) {}
}
