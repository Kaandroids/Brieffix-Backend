package com.briefix.letter.controller;

import com.briefix.letter.dto.PublicLetterPreviewRequest;
import com.briefix.letter.service.PublicLetterService;
import jakarta.validation.Valid;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Public (unauthenticated) REST endpoint for guest letter preview generation.
 *
 * <p>Exposes {@code POST /api/v1/public/letter-preview}, which renders a
 * CLASSIC-template PDF from the caller-supplied sender, recipient, and content
 * data.  No user account is required.  Access is rate-limited to 3 requests
 * per IP per day via the {@link com.briefix.security.RateLimitFilter}.</p>
 */
@RestController
@RequestMapping("/api/v1/public")
public class PublicLetterController {

    /**
     * The service delegate that builds address snapshots and renders the PDF.
     */
    private final PublicLetterService publicLetterService;

    /**
     * Constructs a {@code PublicLetterController} with the required service dependency.
     *
     * @param publicLetterService the service used to render guest letter PDFs;
     *                            must not be {@code null}
     */
    public PublicLetterController(PublicLetterService publicLetterService) {
        this.publicLetterService = publicLetterService;
    }

    /**
     * Generates and returns a CLASSIC-template letter PDF without persisting it.
     *
     * <p>HTTP: {@code POST /api/v1/public/letter-preview}</p>
     * <p>Response: {@code 200 OK} with {@code Content-Type: application/pdf} and
     * {@code Content-Disposition: attachment}.</p>
     *
     * @param req the validated guest letter request
     * @return a {@link ResponseEntity} containing the raw PDF bytes
     */
    @PostMapping("/letter-preview")
    public ResponseEntity<byte[]> preview(@Valid @RequestBody PublicLetterPreviewRequest req) {
        byte[] pdf = publicLetterService.preview(req);
        String filename = sanitizeFilename(req.title()) + ".pdf";
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + filename + "\"")
                .contentType(MediaType.APPLICATION_PDF)
                .body(pdf);
    }

    /**
     * Strips characters that are illegal in common file-system file names from the
     * given string to produce a safe PDF download filename.
     *
     * <p>The following characters are replaced with an underscore ({@code _}):
     * {@code \}, {@code /}, {@code :}, {@code *}, {@code ?}, {@code "}, {@code <},
     * {@code >}, and {@code |}. Leading and trailing whitespace is also trimmed.</p>
     *
     * @param name the raw letter title to sanitise; must not be {@code null}
     * @return a trimmed string safe for use as a file-system file name
     */
    private String sanitizeFilename(String name) {
        return name.replaceAll("[\\\\/:*?\"<>|]", "_").trim();
    }
}
