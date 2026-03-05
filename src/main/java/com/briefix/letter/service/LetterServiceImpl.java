package com.briefix.letter.service;

import com.briefix.contact.exception.ContactNotFoundException;
import com.briefix.contact.model.Contact;
import com.briefix.contact.repository.ContactRepository;
import com.briefix.letter.dto.GenerateLetterRequest;
import com.briefix.letter.dto.LetterDto;
import com.briefix.letter.exception.LetterNotFoundException;
import com.briefix.letter.exception.PremiumRequiredException;
import com.briefix.letter.mapper.LetterMapper;
import com.briefix.letter.model.*;
import com.briefix.letter.repository.LetterRepository;
import com.briefix.profile.exception.ProfileNotFoundException;
import com.briefix.profile.model.Profile;
import com.briefix.profile.repository.ProfileRepository;
import com.briefix.user.exception.UserNotFoundException;
import com.briefix.user.model.UserPlan;
import com.briefix.user.repository.UserRepository;
import com.openhtmltopdf.pdfboxout.PdfRendererBuilder;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;

import java.io.ByteArrayOutputStream;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Base64;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

/**
 * Default implementation of {@link LetterService} providing the complete
 * business logic for letter generation, persistence, retrieval, and deletion
 * in the Briefix application.
 *
 * <p>{@code LetterServiceImpl} orchestrates the following concerns:</p>
 * <ul>
 *   <li><strong>Plan enforcement:</strong> validates that the requested
 *       {@link LetterTemplate} is accessible under the authenticated user's
 *       {@link UserPlan}.  Premium templates ({@code PROFESSIONAL},
 *       {@code ELEGANT}) throw {@link PremiumRequiredException} for non-premium
 *       users.</li>
 *   <li><strong>Profile resolution:</strong> loads the sender's
 *       {@link Profile} by id, verifies it belongs to the authenticated user,
 *       and snapshots it into a {@link SenderSnapshot}.</li>
 *   <li><strong>Recipient resolution:</strong> either resolves a saved
 *       {@link Contact} by id (verifying ownership) and converts it to a
 *       {@link RecipientSnapshot}, or builds the snapshot directly from the
 *       manual fields in the {@link GenerateLetterRequest}.</li>
 *   <li><strong>PDF rendering:</strong> processes a Thymeleaf HTML template
 *       with the letter's context variables and converts the resulting HTML
 *       to a PDF byte array using OpenHTMLToPDF.</li>
 *   <li><strong>Ownership enforcement:</strong> all operations on a specific
 *       letter verify that the requesting user is the recorded owner before
 *       proceeding.</li>
 * </ul>
 *
 * <p><strong>Thread safety:</strong> this service is stateless beyond its
 * injected (singleton) collaborators.  The only shared mutable state is the
 * static {@code DATE_FORMATTER}, which is thread-safe because
 * {@link DateTimeFormatter} instances are immutable.  This class is therefore
 * safe to use as a singleton-scoped Spring bean.</p>
 */
@Service
public class LetterServiceImpl implements LetterService {

    /**
     * Thread-safe formatter used to render the letter date in the German locale
     * (e.g. "05. März 2026") for display inside the rendered HTML template.
     */
    private static final DateTimeFormatter DATE_FORMATTER =
            DateTimeFormatter.ofPattern("dd. MMMM yyyy", Locale.GERMAN);

    /**
     * Repository providing domain-model-level access to persisted letters.
     * All CRUD operations are delegated here.
     */
    private final LetterRepository letterRepository;

    /**
     * Mapper used to convert between the {@link Letter} domain model,
     * {@link com.briefix.letter.entity.LetterEntity}, and
     * {@link LetterDto}.
     */
    private final LetterMapper letterMapper;

    /**
     * Repository used to resolve the authenticated user's domain model from
     * their email address as provided by the security context.
     */
    private final UserRepository userRepository;

    /**
     * Repository used to load and verify sender profiles referenced in letter
     * generation requests.
     */
    private final ProfileRepository profileRepository;

    /**
     * Repository used to load and verify contacts that serve as letter
     * recipients when the caller supplies a {@code contactId}.
     */
    private final ContactRepository contactRepository;

    /**
     * Thymeleaf template engine used to render the letter HTML from the
     * template name and context variables.  Template files reside under
     * {@code src/main/resources/templates/letters/}.
     */
    private final TemplateEngine templateEngine;

    /**
     * Constructs a new {@code LetterServiceImpl} with all required
     * collaborators injected by the Spring container.
     *
     * @param letterRepository  the repository for letter persistence operations;
     *                          must not be {@code null}
     * @param letterMapper      the mapper for domain/entity/DTO conversion;
     *                          must not be {@code null}
     * @param userRepository    the user repository for principal resolution;
     *                          must not be {@code null}
     * @param profileRepository the profile repository for sender data
     *                          resolution; must not be {@code null}
     * @param contactRepository the contact repository for recipient data
     *                          resolution; must not be {@code null}
     * @param templateEngine    the Thymeleaf engine for HTML rendering;
     *                          must not be {@code null}
     */
    public LetterServiceImpl(LetterRepository letterRepository,
                             LetterMapper letterMapper,
                             UserRepository userRepository,
                             ProfileRepository profileRepository,
                             ContactRepository contactRepository,
                             TemplateEngine templateEngine) {
        this.letterRepository = letterRepository;
        this.letterMapper = letterMapper;
        this.userRepository = userRepository;
        this.profileRepository = profileRepository;
        this.contactRepository = contactRepository;
        this.templateEngine = templateEngine;
    }

    /**
     * {@inheritDoc}
     *
     * <p>Resolves the user, enforces plan access, resolves the sender profile
     * and recipient, then delegates to {@link #renderPdf} to produce the PDF
     * bytes.  The letter is not persisted.</p>
     *
     * @param req   the generation request; must not be {@code null}
     * @param email the authenticated user's email address
     * @return raw PDF bytes for the preview
     * @throws UserNotFoundException       if no user with the given email exists
     * @throws ProfileNotFoundException    if the specified profile does not exist
     * @throws AccessDeniedException       if the profile or contact is not
     *                                     owned by the user
     * @throws PremiumRequiredException    if the template requires a PREMIUM plan
     */
    @Override
    public byte[] preview(GenerateLetterRequest req, String email) {
        var user = userRepository.findByEmail(email)
                .orElseThrow(() -> new UserNotFoundException(email));
        checkPlanAccess(req.template(), user.plan());
        var profile = resolveProfile(req.profileId(), user.id());
        var sender = buildSenderSnapshot(profile);
        var recipient = buildRecipientSnapshot(req, user.id());
        return renderPdf(req.template(), sender, recipient, req.title(), req.body(),
                req.letterDate() != null ? req.letterDate() : LocalDate.now());
    }

    /**
     * {@inheritDoc}
     *
     * <p>Resolves the user, enforces plan access, resolves the sender profile
     * and recipient, constructs a new {@link Letter} domain record with a
     * {@code null} id (triggering an insert), persists it, and returns the
     * saved state as a DTO.</p>
     *
     * @param req   the generation request; must not be {@code null}
     * @param email the authenticated user's email address
     * @return the persisted {@link LetterDto} with database-generated id and
     *         creation timestamp
     * @throws UserNotFoundException    if no user with the given email exists
     * @throws ProfileNotFoundException if the specified profile does not exist
     * @throws AccessDeniedException    if the profile or contact is not owned
     *                                  by the user
     * @throws PremiumRequiredException if the template requires a PREMIUM plan
     */
    @Override
    public LetterDto create(GenerateLetterRequest req, String email) {
        var user = userRepository.findByEmail(email)
                .orElseThrow(() -> new UserNotFoundException(email));
        checkPlanAccess(req.template(), user.plan());
        var profile = resolveProfile(req.profileId(), user.id());
        var sender = buildSenderSnapshot(profile);
        var recipient = buildRecipientSnapshot(req, user.id());
        var letterDate = req.letterDate() != null ? req.letterDate() : LocalDate.now();

        var letter = new Letter(
                null,
                user.id(),
                req.title(),
                req.body(),
                letterDate,
                sender,
                recipient,
                req.template(),
                null,
                null
        );
        return letterMapper.toDto(letterRepository.save(letter, user.id()));
    }

    /**
     * {@inheritDoc}
     *
     * <p>Resolves the user from their email address, then retrieves all letters
     * whose {@code userId} matches and maps them to DTOs.</p>
     *
     * @param email the authenticated user's email address
     * @return list of {@link LetterDto} objects; never {@code null}, may be empty
     * @throws UserNotFoundException if no user with the given email exists
     */
    @Override
    public List<LetterDto> getMyLetters(String email) {
        var user = userRepository.findByEmail(email)
                .orElseThrow(() -> new UserNotFoundException(email));
        return letterRepository.findByUserId(user.id()).stream()
                .map(letterMapper::toDto)
                .toList();
    }

    /**
     * {@inheritDoc}
     *
     * <p>Resolves the user, loads the letter, verifies ownership, then renders
     * the PDF on-the-fly from the persisted snapshot data.</p>
     *
     * @param id    the UUID of the letter to render; must not be {@code null}
     * @param email the authenticated user's email address
     * @return a {@link PdfResult} containing the rendered PDF bytes and title
     * @throws UserNotFoundException   if no user with the given email exists
     * @throws LetterNotFoundException if no letter with the given id exists
     * @throws AccessDeniedException   if the authenticated user does not own
     *                                 the letter
     */
    @Override
    public PdfResult getPdf(UUID id, String email) {
        var user = userRepository.findByEmail(email)
                .orElseThrow(() -> new UserNotFoundException(email));
        var letter = letterRepository.findById(id)
                .orElseThrow(() -> new LetterNotFoundException(id));
        if (!letter.userId().equals(user.id())) {
            throw new AccessDeniedException("Access denied to letter: " + id);
        }
        byte[] bytes = renderPdf(letter.template(), letter.senderSnapshot(), letter.recipientSnapshot(),
                letter.title(), letter.body(), letter.letterDate());
        return new PdfResult(bytes, letter.title());
    }

    /**
     * {@inheritDoc}
     *
     * <p>Resolves the user, loads the letter, verifies ownership, then
     * permanently deletes the letter record via the repository.</p>
     *
     * @param id    the UUID of the letter to delete; must not be {@code null}
     * @param email the authenticated user's email address
     * @throws UserNotFoundException   if no user with the given email exists
     * @throws LetterNotFoundException if no letter with the given id exists
     * @throws AccessDeniedException   if the authenticated user does not own
     *                                 the letter
     */
    @Override
    public void delete(UUID id, String email) {
        var user = userRepository.findByEmail(email)
                .orElseThrow(() -> new UserNotFoundException(email));
        var letter = letterRepository.findById(id)
                .orElseThrow(() -> new LetterNotFoundException(id));
        if (!letter.userId().equals(user.id())) {
            throw new AccessDeniedException("Access denied to letter: " + id);
        }
        letterRepository.deleteById(id);
    }

    // -------------------------------------------------------------------------

    /**
     * Verifies that the given user plan permits use of the specified template.
     *
     * <p>Templates {@code PROFESSIONAL} and {@code ELEGANT} are classified as
     * PREMIUM and require the user to hold a {@link UserPlan#PREMIUM}
     * subscription.  All other templates ({@code CLASSIC}, {@code MODERN})
     * are available to all plans.</p>
     *
     * @param template the template whose access tier is to be checked; must
     *                 not be {@code null}
     * @param plan     the authenticated user's current subscription plan; must
     *                 not be {@code null}
     * @throws PremiumRequiredException if {@code template} is a PREMIUM
     *                                  template and {@code plan} is not
     *                                  {@link UserPlan#PREMIUM}
     */
    private void checkPlanAccess(LetterTemplate template, UserPlan plan) {
        if ((template == LetterTemplate.PROFESSIONAL || template == LetterTemplate.ELEGANT)
                && plan != UserPlan.PREMIUM) {
            throw new PremiumRequiredException(template.name());
        }
    }

    /**
     * Loads the sender {@link Profile} by id and verifies that it belongs to
     * the requesting user.
     *
     * @param profileId the UUID of the profile to resolve; must not be
     *                  {@code null}
     * @param userId    the UUID of the authenticated user; the resolved
     *                  profile's {@code userId} must match this value
     * @return the resolved {@link Profile} domain model
     * @throws ProfileNotFoundException if no profile with the given id exists
     * @throws AccessDeniedException    if the resolved profile does not belong
     *                                  to the specified user
     */
    private Profile resolveProfile(UUID profileId, UUID userId) {
        var profile = profileRepository.findById(profileId)
                .orElseThrow(() -> new ProfileNotFoundException(profileId));
        if (!profile.userId().equals(userId)) {
            throw new AccessDeniedException("Access denied to profile: " + profileId);
        }
        return profile;
    }

    /**
     * Builds the {@link RecipientSnapshot} for a letter from the generation
     * request.
     *
     * <p>If {@code req.contactId()} is non-null, the contact is loaded from the
     * repository, its ownership is verified, and its data is converted to a
     * snapshot via {@link #fromContact(Contact)}.  Otherwise, the manual
     * {@code recipient*} fields from the request are used directly to construct
     * the snapshot.</p>
     *
     * @param req    the generation request providing either a contact id or
     *               manual recipient fields; must not be {@code null}
     * @param userId the UUID of the authenticated user; used to verify
     *               ownership of the referenced contact
     * @return the constructed {@link RecipientSnapshot}; never {@code null}
     * @throws ContactNotFoundException if a {@code contactId} is provided but
     *                                  no matching contact exists
     * @throws AccessDeniedException    if the referenced contact does not
     *                                  belong to the authenticated user
     */
    private RecipientSnapshot buildRecipientSnapshot(GenerateLetterRequest req, UUID userId) {
        if (req.contactId() != null) {
            var contact = contactRepository.findById(req.contactId())
                    .orElseThrow(() -> new ContactNotFoundException(req.contactId()));
            if (!contact.userId().equals(userId)) {
                throw new AccessDeniedException("Access denied to contact: " + req.contactId());
            }
            return fromContact(contact);
        }
        return new RecipientSnapshot(
                null,
                req.recipientSalutation(),
                req.recipientFirstName(),
                req.recipientLastName(),
                req.recipientCompany(),
                req.recipientContactPerson(),
                req.recipientStreet(),
                req.recipientStreetNumber(),
                req.recipientPostalCode(),
                req.recipientCity(),
                req.recipientCountry(),
                req.recipientEmail(),
                req.recipientPhone()
        );
    }

    /**
     * Converts a resolved sender {@link Profile} into a {@link SenderSnapshot},
     * capturing all profile fields at the moment of letter generation.
     *
     * @param p the sender profile to snapshot; must not be {@code null}
     * @return a new {@link SenderSnapshot} containing the profile's current
     *         field values
     */
    private SenderSnapshot buildSenderSnapshot(Profile p) {
        return new SenderSnapshot(
                p.type().name(),
                p.profileLabel(),
                p.salutation(),
                p.title(),
                p.firstName(),
                p.lastName(),
                p.companyName(),
                p.department(),
                p.street(),
                p.streetNumber(),
                p.postalCode(),
                p.city(),
                p.country(),
                p.phone(),
                p.fax(),
                p.email(),
                p.website(),
                p.vatId(),
                p.taxNumber(),
                p.managingDirector(),
                p.registerCourt(),
                p.registerNumber(),
                p.iban(),
                p.bic(),
                p.bankName(),
                p.contactPerson()
        );
    }

    /**
     * Converts a {@link Contact} domain model into a {@link RecipientSnapshot},
     * capturing the contact's current field values at the time of letter
     * generation.
     *
     * <p>The snapshot's {@code type} field is set to the contact's
     * {@link com.briefix.profile.model.PartyType} name (e.g. {@code "COMPANY"}
     * or {@code "INDIVIDUAL"}) so that templates can conditionally render the
     * appropriate address format.</p>
     *
     * @param c the contact to convert; must not be {@code null}
     * @return a new {@link RecipientSnapshot} reflecting the contact's current
     *         data
     */
    private RecipientSnapshot fromContact(Contact c) {
        return new RecipientSnapshot(
                c.type().name(),
                c.salutation(),
                c.firstName(),
                c.lastName(),
                c.companyName(),
                c.contactPerson(),
                c.street(),
                c.streetNumber(),
                c.postalCode(),
                c.city(),
                c.country(),
                c.email(),
                c.phone()
        );
    }

    /**
     * Renders a letter to PDF bytes using the Thymeleaf template engine and
     * OpenHTMLToPDF.
     *
     * <p>The method:</p>
     * <ol>
     *   <li>Populates a Thymeleaf {@link Context} with the sender snapshot,
     *       recipient snapshot, letter title, body, formatted date, and an
     *       optional decorative image encoded as a Base64 data URI.</li>
     *   <li>Processes the template located at
     *       {@code letters/<templateName-lowercase>.html}.</li>
     *   <li>Passes the resulting HTML string to
     *       {@link PdfRendererBuilder} to produce the PDF output stream.</li>
     * </ol>
     *
     * @param template   the design template that determines which Thymeleaf
     *                   HTML file is processed; must not be {@code null}
     * @param sender     the sender snapshot to expose to the template engine;
     *                   must not be {@code null}
     * @param recipient  the recipient snapshot to expose to the template engine;
     *                   must not be {@code null}
     * @param title      the letter title to expose to the template engine; must
     *                   not be blank
     * @param body       the letter body text to expose to the template engine;
     *                   must not be blank
     * @param letterDate the date to format and expose to the template engine;
     *                   must not be {@code null}
     * @return a byte array containing the rendered PDF document
     * @throws RuntimeException if HTML-to-PDF conversion fails for any reason
     */
    private byte[] renderPdf(LetterTemplate template, SenderSnapshot sender,
                              RecipientSnapshot recipient, String title, String body,
                              LocalDate letterDate) {
        var ctx = new Context();
        ctx.setVariable("sender", sender);
        ctx.setVariable("recipient", recipient);
        ctx.setVariable("title", title);
        ctx.setVariable("body", body);
        ctx.setVariable("date", letterDate.format(DATE_FORMATTER));
        String decorBase64 = loadDecorImageBase64();
        ctx.setVariable("decorImage", decorBase64 != null ? "data:image/png;base64," + decorBase64 : null);

        String html = templateEngine.process("letters/" + template.name().toLowerCase(), ctx);

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
     * Loads the decorative image resource bundled with the application and
     * returns its content as a Base64-encoded string.
     *
     * <p>The image is read from the classpath at
     * {@code /templates/images/standard_letter_decor.png}.  If the resource
     * cannot be found or read (e.g. it is absent in a particular build), the
     * method returns {@code null} rather than failing, allowing the template to
     * render without the decorative element.</p>
     *
     * @return a Base64-encoded string of the PNG image bytes, or {@code null}
     *         if the resource is unavailable
     */
    private String loadDecorImageBase64() {
        try (var is = getClass().getResourceAsStream("/templates/images/standard_letter_decor.png")) {
            if (is == null) return null;
            return Base64.getEncoder().encodeToString(is.readAllBytes());
        } catch (Exception e) {
            return null;
        }
    }
}
