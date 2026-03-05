package com.briefix.letter.controller;

import com.briefix.letter.dto.GenerateLetterRequest;
import com.briefix.letter.dto.LetterDto;
import com.briefix.letter.service.LetterService;
import jakarta.validation.Valid;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

/**
 * REST controller exposing endpoints for letter generation, retrieval, PDF
 * download, and deletion.
 *
 * <p>{@code LetterController} handles all HTTP requests under the base path
 * {@code /api/v1/letters} and delegates every operation to
 * {@link LetterService}.  Its responsibilities are:</p>
 * <ul>
 *   <li>Deserialising and validating inbound JSON request bodies via
 *       {@code @Valid} and bean-validation constraints declared on
 *       {@link GenerateLetterRequest}.</li>
 *   <li>Extracting the authenticated principal's email address from the
 *       {@link Authentication} object and forwarding it to the service layer,
 *       which uses it for user resolution, plan enforcement, and ownership
 *       checks.</li>
 *   <li>Building PDF HTTP responses with the correct
 *       {@code Content-Type: application/pdf} header and a sanitised
 *       {@code Content-Disposition} filename derived from the letter title.</li>
 *   <li>Setting appropriate HTTP response status codes.</li>
 * </ul>
 *
 * <p>All endpoints require an authenticated user.  Authentication is enforced
 * by the application's Spring Security configuration; this controller does not
 * perform any security checks itself.</p>
 *
 * <p><strong>Thread safety:</strong> this controller is stateless and therefore
 * safe to use as a singleton-scoped Spring bean.</p>
 */
@RestController
@RequestMapping("/api/v1/letters")
public class LetterController {

    /**
     * Business-logic delegate that handles all letter operations, including
     * plan enforcement, PDF rendering, and persistence.
     */
    private final LetterService letterService;

    /**
     * Constructs a new {@code LetterController} with the required service
     * dependency injected by Spring.
     *
     * @param letterService the letter service providing all business logic;
     *                      must not be {@code null}
     */
    public LetterController(LetterService letterService) {
        this.letterService = letterService;
    }

    /**
     * Generates a PDF preview of the letter described by the request body
     * without persisting anything to the database.
     *
     * <p>HTTP: {@code POST /api/v1/letters/preview}</p>
     * <p>Response: {@code 200 OK} with {@code Content-Type: application/pdf}
     * and {@code Content-Disposition: inline; filename="<title>.pdf"}.  The
     * {@code inline} disposition instructs the browser to render the PDF
     * in-page rather than downloading it.</p>
     *
     * @param req            the validated generation request; must pass all
     *                       bean-validation constraints
     * @param authentication the Spring Security authentication object providing
     *                       the caller's email address via
     *                       {@link Authentication#getName()}
     * @return a {@link ResponseEntity} whose body is the raw PDF byte array
     */
    @PostMapping("/preview")
    public ResponseEntity<byte[]> preview(@Valid @RequestBody GenerateLetterRequest req,
                                          Authentication authentication) {
        byte[] pdf = letterService.preview(req, authentication.getName());
        String filename = sanitizeFilename(req.title()) + ".pdf";
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"" + filename + "\"")
                .contentType(MediaType.APPLICATION_PDF)
                .body(pdf);
    }

    /**
     * Generates a letter, persists it to the database, and returns the saved
     * representation as a JSON DTO.
     *
     * <p>HTTP: {@code POST /api/v1/letters}</p>
     * <p>Response status: {@code 201 Created}</p>
     *
     * @param req            the validated generation request; must pass all
     *                       bean-validation constraints
     * @param authentication the Spring Security authentication object providing
     *                       the caller's email address
     * @return a {@link ResponseEntity} with status {@code 201 Created} and a
     *         body containing the newly created {@link LetterDto}
     */
    @PostMapping
    public ResponseEntity<LetterDto> create(@Valid @RequestBody GenerateLetterRequest req,
                                            Authentication authentication) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(letterService.create(req, authentication.getName()));
    }

    /**
     * Retrieves all letters that belong to the currently authenticated user.
     *
     * <p>HTTP: {@code GET /api/v1/letters}</p>
     *
     * @param authentication the Spring Security authentication object providing
     *                       the caller's email address
     * @return a {@link ResponseEntity} with status {@code 200 OK} and a body
     *         containing the list of the user's {@link LetterDto} objects;
     *         empty list if the user has no letters
     */
    @GetMapping
    public ResponseEntity<List<LetterDto>> getMyLetters(Authentication authentication) {
        return ResponseEntity.ok(letterService.getMyLetters(authentication.getName()));
    }

    /**
     * Renders a persisted letter to PDF and returns the bytes as a downloadable
     * file attachment.
     *
     * <p>HTTP: {@code GET /api/v1/letters/{id}/pdf}</p>
     * <p>Response: {@code 200 OK} with {@code Content-Type: application/pdf}
     * and {@code Content-Disposition: attachment; filename="<title>.pdf"}.  The
     * {@code attachment} disposition causes the browser to offer a download
     * dialog rather than rendering in-page.</p>
     *
     * @param id             the UUID of the letter to render, extracted from the
     *                       URL path variable
     * @param authentication the Spring Security authentication object providing
     *                       the caller's email address for ownership verification
     * @return a {@link ResponseEntity} whose body is the raw PDF byte array
     */
    @GetMapping("/{id}/pdf")
    public ResponseEntity<byte[]> getPdf(@PathVariable UUID id, Authentication authentication) {
        var result = letterService.getPdf(id, authentication.getName());
        String filename = sanitizeFilename(result.title()) + ".pdf";
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + filename + "\"")
                .contentType(MediaType.APPLICATION_PDF)
                .body(result.bytes());
    }

    /**
     * Permanently deletes the letter identified by the given id.
     *
     * <p>HTTP: {@code DELETE /api/v1/letters/{id}}</p>
     * <p>Response status: {@code 204 No Content}</p>
     * <p>Returns {@code 403 Forbidden} if the authenticated user does not own
     * the letter, and {@code 404 Not Found} if the letter does not exist.</p>
     *
     * @param id             the UUID of the letter to delete, extracted from the
     *                       URL path variable
     * @param authentication the Spring Security authentication object providing
     *                       the caller's email address for ownership verification
     * @return a {@link ResponseEntity} with status {@code 204 No Content} and
     *         an empty body
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable UUID id, Authentication authentication) {
        letterService.delete(id, authentication.getName());
        return ResponseEntity.noContent().build();
    }

    /**
     * Sanitises a string for safe use as a file system filename by replacing
     * characters that are illegal in common operating system file names with
     * underscores and trimming surrounding whitespace.
     *
     * <p>The following characters are replaced: {@code \ / : * ? " < > |}</p>
     *
     * @param name the raw string to sanitise (typically a letter title); must
     *             not be {@code null}
     * @return a sanitised string safe to use as a file name component
     */
    private String sanitizeFilename(String name) {
        return name.replaceAll("[\\\\/:*?\"<>|]", "_").trim();
    }
}
