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

    /**
     * German-locale date formatter that renders dates in the DIN 5008 style,
     * e.g. {@code "08. März 2026"}. Used when populating the Thymeleaf template context.
     */
    private static final DateTimeFormatter DATE_FORMATTER =
            DateTimeFormatter.ofPattern("dd. MMMM yyyy", Locale.GERMAN);

    /**
     * The Thymeleaf template engine used to render the CLASSIC letter HTML template
     * before it is converted to PDF by OpenHTMLToPDF.
     */
    private final TemplateEngine templateEngine;

    /**
     * Constructs a {@code PublicLetterService} with the required Thymeleaf template engine.
     *
     * @param templateEngine the Thymeleaf engine; must not be {@code null}
     */
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

    /**
     * Constructs a {@link SenderSnapshot} from the sender fields of the guest request.
     *
     * <p>The sender type defaults to {@code "INDIVIDUAL"} if the {@code senderType}
     * field is not exactly {@code "ORGANIZATION"}. The country defaults to
     * {@code "Deutschland"} when the supplied value is blank. Fields that are not
     * applicable to the guest preview (logo, IBAN, BIC, tax number, etc.) are always
     * set to {@code null}.</p>
     *
     * @param req the validated guest request; must not be {@code null}
     * @return a fully populated {@link SenderSnapshot} ready for the template context
     */
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

    /**
     * Constructs a {@link RecipientSnapshot} from the recipient fields of the guest request.
     *
     * <p>The recipient type defaults to {@code "INDIVIDUAL"} if the {@code recipientType}
     * field is not exactly {@code "ORGANIZATION"}. For organization recipients the
     * salutation is taken from {@code recipientContactPersonSalutation}; for individual
     * recipients it comes from {@code recipientSalutation}. The country defaults to
     * {@code "Deutschland"} when the supplied value is blank.</p>
     *
     * @param req the validated guest request; must not be {@code null}
     * @return a fully populated {@link RecipientSnapshot} ready for the template context
     */
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

    /**
     * Renders the CLASSIC letter template to PDF bytes using Thymeleaf and OpenHTMLToPDF.
     *
     * <p>Populates a Thymeleaf {@link Context} with the sender, recipient, title, body,
     * and formatted date, then processes the {@code letters/classic} template to produce
     * an XHTML string. OpenHTMLToPDF ({@link PdfRendererBuilder}) converts the XHTML to
     * a DIN A4 PDF. Logo and decorative image variables are set to {@code null} for
     * guest previews.</p>
     *
     * @param sender      the sender snapshot to include in the template context
     * @param recipient   the recipient snapshot to include in the template context
     * @param title       the letter subject line; must not be blank
     * @param body        the letter body text; must not be blank
     * @param letterDate  the date to print on the letter
     * @return the rendered PDF as a byte array
     * @throws RuntimeException if Thymeleaf template processing or PDF generation fails
     */
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

    /**
     * Returns {@code true} if the given string is non-null and not blank.
     *
     * <p>Used as a concise null-and-blank guard throughout this service to decide
     * whether optional address fields should be included in snapshot objects or
     * defaulted to fallback values.</p>
     *
     * @param s the string to test; may be {@code null}
     * @return {@code true} if {@code s} is non-null and contains at least one
     *         non-whitespace character; {@code false} otherwise
     */
    private boolean notBlank(String s) {
        return s != null && !s.isBlank();
    }
}
