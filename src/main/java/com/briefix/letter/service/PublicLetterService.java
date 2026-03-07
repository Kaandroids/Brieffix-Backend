package com.briefix.letter.service;

import com.briefix.letter.dto.PublicLetterPreviewRequest;
import com.briefix.letter.model.LetterTemplate;
import com.briefix.letter.model.RecipientSnapshot;
import com.briefix.letter.model.SenderSnapshot;
import com.openhtmltopdf.pdfboxout.PdfRendererBuilder;
import org.springframework.stereotype.Service;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;

import java.io.ByteArrayOutputStream;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

/**
 * Service responsible for rendering guest (unauthenticated) letter previews.
 *
 * <p>Unlike {@link LetterServiceImpl}, this service does not require a user account,
 * a saved profile, or a saved contact. It builds {@link SenderSnapshot} and
 * {@link RecipientSnapshot} directly from the supplied request fields and renders
 * the result using the {@code CLASSIC} template.</p>
 *
 * <p>No letter is persisted — the PDF bytes are returned immediately to the caller.</p>
 */
@Service
public class PublicLetterService {

    private static final DateTimeFormatter DATE_FORMATTER =
            DateTimeFormatter.ofPattern("dd. MMMM yyyy", Locale.GERMAN);

    private final TemplateEngine templateEngine;

    public PublicLetterService(TemplateEngine templateEngine) {
        this.templateEngine = templateEngine;
    }

    /**
     * Renders a CLASSIC-template letter PDF from the provided guest request.
     *
     * @param req the guest letter request; must be valid
     * @return raw PDF bytes
     */
    public byte[] preview(PublicLetterPreviewRequest req) {
        var sender = buildSender(req);
        var recipient = buildRecipient(req);
        LocalDate date = req.letterDate() != null ? req.letterDate() : LocalDate.now();
        return renderPdf(sender, recipient, req.title(), req.body(), date);
    }

    // ── Private helpers ───────────────────────────────────────────────────────

    private SenderSnapshot buildSender(PublicLetterPreviewRequest req) {
        String type = "ORGANIZATION".equals(req.senderType()) ? "ORGANIZATION" : "INDIVIDUAL";
        String country = notBlank(req.senderCountry()) ? req.senderCountry() : "Deutschland";
        return new SenderSnapshot(
                type, null,
                req.senderSalutation(),
                req.senderTitle(),
                req.senderFirstName(),
                req.senderLastName(),
                req.senderCompanyName(),
                req.senderDepartment(),
                req.senderStreet(),
                req.senderStreetNumber(),
                req.senderPostalCode(),
                req.senderCity(),
                country,
                req.senderPhone(), null,
                req.senderEmail(), null,
                null, null, null, null, null,
                null, null, null,
                req.senderContactPerson(),
                null
        );
    }

    private RecipientSnapshot buildRecipient(PublicLetterPreviewRequest req) {
        String type = "ORGANIZATION".equals(req.recipientType()) ? "ORGANIZATION" : "INDIVIDUAL";
        String country = notBlank(req.recipientCountry()) ? req.recipientCountry() : "Deutschland";
        // For ORGANIZATION: salutation comes from contactPersonSalutation
        String salutation = "ORGANIZATION".equals(type)
                ? req.recipientContactPersonSalutation()
                : req.recipientSalutation();
        return new RecipientSnapshot(
                type,
                salutation,
                req.recipientFirstName(),
                req.recipientLastName(),
                req.recipientCompany(),
                req.recipientContactPerson(),
                req.recipientDepartment(),
                req.recipientStreet(),
                req.recipientStreetNumber(),
                req.recipientPostalCode(),
                req.recipientCity(),
                country,
                null, null
        );
    }

    private byte[] renderPdf(SenderSnapshot sender, RecipientSnapshot recipient,
                              String title, String body, LocalDate letterDate) {
        var ctx = new Context();
        ctx.setVariable("sender", sender);
        ctx.setVariable("recipient", recipient);
        ctx.setVariable("title", title);
        ctx.setVariable("body", body);
        ctx.setVariable("date", letterDate.format(DATE_FORMATTER));
        ctx.setVariable("decorImage", null);
        ctx.setVariable("logoImage", null);

        String html = templateEngine.process("letters/" + LetterTemplate.CLASSIC.name().toLowerCase(), ctx);

        try (var os = new ByteArrayOutputStream()) {
            new PdfRendererBuilder()
                    .withHtmlContent(html, null)
                    .toStream(os)
                    .run();
            return os.toByteArray();
        } catch (Exception e) {
            throw new RuntimeException("PDF generation failed", e);
        }
    }

    private boolean notBlank(String s) {
        return s != null && !s.isBlank();
    }
}
