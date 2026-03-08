package com.briefix.ai.service;

import com.briefix.ai.dto.AiGenerateRequest;
import com.briefix.ai.dto.AiLetterResponse;
import com.briefix.contact.model.Contact;
import com.briefix.contact.repository.ContactRepository;
import com.briefix.profile.model.Profile;
import com.briefix.profile.repository.ProfileRepository;
import com.briefix.user.model.User;
import com.briefix.user.repository.UserRepository;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.util.Map;
import java.util.UUID;

/**
 * Default implementation of {@link AiService} that generates letter content by
 * communicating with the Google Gemini generative AI REST API.
 *
 * <p>For each generation request, this service orchestrates the following steps:</p>
 * <ol>
 *   <li><strong>User resolution:</strong> loads the {@link com.briefix.user.model.User}
 *       aggregate by email address to scope all subsequent lookups.</li>
 *   <li><strong>Context enrichment:</strong> optionally resolves the sender's
 *       {@link com.briefix.profile.model.Profile} and the recipient's
 *       {@link com.briefix.contact.model.Contact}, verifying that each record belongs
 *       to the requesting user before including it in the prompt.</li>
 *   <li><strong>Prompt construction:</strong> assembles a German-language,
 *       DIN-5008-aware prompt via {@link #buildPrompt(String, String, String)},
 *       embedding the user's description and any resolved sender/recipient summaries.</li>
 *   <li><strong>Gemini invocation:</strong> sends the prompt to the configured Gemini
 *       model via {@link RestClient} and extracts the plain-text response from the
 *       {@code candidates[0].content.parts[0].text} path.</li>
 *   <li><strong>Response parsing:</strong> locates the outermost JSON object in the
 *       response text (immune to Markdown fences or surrounding prose) and deserialises
 *       it into an {@link AiLetterResponse}.</li>
 * </ol>
 *
 * <p>Any exception thrown during these steps is caught, logged at ERROR level, and
 * converted into a failure {@link AiLetterResponse} ({@code success: false}) so that
 * the REST layer always returns a well-formed response.</p>
 *
 * <p>Configuration properties (from {@code application.yaml} or environment):</p>
 * <ul>
 *   <li>{@code app.gemini.api-key} — Gemini REST API key (injected via {@code @Value})</li>
 *   <li>{@code app.gemini.model} — Gemini model identifier, e.g. {@code gemini-2.5-flash}</li>
 * </ul>
 *
 * <p>Thread-safety: This class is a stateless Spring singleton (field values for
 * {@code apiKey} and {@code model} are set once during bean post-processing) and is
 * safe for concurrent use across multiple HTTP request threads.</p>
 */
@Service
public class AiServiceImpl {

    private static final Logger log = LoggerFactory.getLogger(AiServiceImpl.class);

    /**
     * Domain-layer repository used to resolve the authenticated user by email address.
     */
    private final UserRepository userRepository;

    /**
     * Domain-layer repository used to look up the sender's profile for contextual enrichment.
     */
    private final ProfileRepository profileRepository;

    /**
     * Domain-layer repository used to look up the recipient contact for contextual enrichment.
     */
    private final ContactRepository contactRepository;

    /**
     * Spring {@link RestClient} instance used to issue HTTP POST requests to the Gemini API.
     * Created once in the constructor and reused across all requests.
     */
    private final RestClient restClient;

    /**
     * Jackson {@link ObjectMapper} used to serialise the Gemini request body and
     * deserialise the JSON fragment extracted from the Gemini response text.
     */
    private final ObjectMapper objectMapper;

    /**
     * Google Gemini REST API key, injected from the {@code app.gemini.api-key} property.
     * Appended as a query parameter on every Gemini API call.
     */
    @Value("${app.gemini.api-key}")
    private String apiKey;

    /**
     * The Gemini model identifier to invoke, injected from the {@code app.gemini.model}
     * property (e.g. {@code gemini-2.5-flash}). Embedded in the Gemini endpoint URL.
     */
    @Value("${app.gemini.model}")
    private String model;

    /**
     * Constructs an {@code AiServiceImpl} with all required dependencies.
     *
     * <p>A new {@link RestClient} instance is created eagerly in this constructor so that
     * the client is ready before the first request arrives, avoiding any initialization
     * overhead on the hot path.</p>
     *
     * @param userRepository    the repository used to resolve users by email; must not be {@code null}
     * @param profileRepository the repository used to look up sender profiles; must not be {@code null}
     * @param contactRepository the repository used to look up recipient contacts; must not be {@code null}
     * @param objectMapper      the Jackson mapper for JSON serialisation/deserialisation; must not be {@code null}
     */
    public AiServiceImpl(UserRepository userRepository,
                         ProfileRepository profileRepository,
                         ContactRepository contactRepository,
                         ObjectMapper objectMapper) {
        this.userRepository = userRepository;
        this.profileRepository = profileRepository;
        this.contactRepository = contactRepository;
        this.objectMapper = objectMapper;
        this.restClient = RestClient.create();
    }

    /**
     * {@inheritDoc}
     *
     * <p>Resolves the user account by {@code userEmail}, optionally enriches the prompt
     * with sender and recipient context, submits the assembled prompt to Gemini, and
     * parses the resulting JSON into an {@link AiLetterResponse}. Any unhandled exception
     * is caught and translated into a failure response ({@code success: false}).</p>
     *
     * @param request   the validated generation request; must not be {@code null}
     * @param userEmail the email address of the authenticated caller; used to scope
     *                  profile and contact lookups to the correct user account
     * @return an {@link AiLetterResponse} with {@code success: true} and the generated
     *         {@code title} and {@code content}, or {@code success: false} on any failure
     */
    public AiLetterResponse generateLetter(AiGenerateRequest request, String userEmail) {
        try {
            User user = userRepository.findByEmail(userEmail).orElseThrow();

            String senderInfo = resolveSenderInfo(request.profileId(), user.id());
            String recipientInfo = resolveRecipientInfo(request.contactId(), user.id());

            String prompt = buildPrompt(request.description(), senderInfo, recipientInfo);

            String responseText = callGemini(prompt);
            return parseResponse(responseText);
        } catch (Exception e) {
            log.error("AI letter generation failed: {}", e.getMessage(), e);
            return new AiLetterResponse(null, null, false);
        }
    }

    /**
     * Resolves a brief textual summary of the sender's profile for prompt enrichment.
     *
     * <p>Looks up the profile by {@code profileId}, verifies that it belongs to
     * {@code userId} (ownership guard), and delegates to {@link #formatProfile(Profile)}
     * to produce a single-line summary. Returns {@code null} if the ID is blank, the
     * record does not exist, the ownership check fails, or the ID is not a valid UUID.</p>
     *
     * @param profileId the UUID string of the sender profile, or {@code null}/blank to skip
     * @param userId    the ID of the authenticated user; used to verify profile ownership
     * @return a formatted sender summary string, or {@code null} if unavailable
     */
    private String resolveSenderInfo(String profileId, UUID userId) {
        if (profileId == null || profileId.isBlank()) return null;
        try {
            UUID id = UUID.fromString(profileId);
            return profileRepository.findById(id)
                    .filter(p -> p.userId().equals(userId))
                    .map(this::formatProfile)
                    .orElse(null);
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * Resolves a brief textual summary of the recipient contact for prompt enrichment.
     *
     * <p>Mirrors {@link #resolveSenderInfo(String, UUID)} but operates on the
     * {@link ContactRepository} and delegates to {@link #formatContact(Contact)}.
     * Returns {@code null} if the ID is blank, the record does not exist, the ownership
     * check fails, or the ID is not a valid UUID.</p>
     *
     * @param contactId the UUID string of the recipient contact, or {@code null}/blank to skip
     * @param userId    the ID of the authenticated user; used to verify contact ownership
     * @return a formatted recipient summary string, or {@code null} if unavailable
     */
    private String resolveRecipientInfo(String contactId, UUID userId) {
        if (contactId == null || contactId.isBlank()) return null;
        try {
            UUID id = UUID.fromString(contactId);
            return contactRepository.findById(id)
                    .filter(c -> c.userId().equals(userId))
                    .map(this::formatContact)
                    .orElse(null);
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * Produces a single-line human-readable summary of a sender profile for inclusion
     * in the Gemini prompt.
     *
     * <p>For {@code ORGANIZATION} type profiles the company name is used; for
     * {@code INDIVIDUAL} profiles the first and last name are concatenated. The
     * city is appended to both variants when present, separated by a comma and space.</p>
     *
     * @param p the resolved sender profile; must not be {@code null}
     * @return a trimmed summary string (e.g. {@code "Max Mustermann, Berlin"} or
     *         {@code "Muster GmbH, München"}); never {@code null} but may be empty
     */
    private String formatProfile(Profile p) {
        StringBuilder sb = new StringBuilder();
        if (p.type() != null && p.type().name().equals("ORGANIZATION")) {
            if (p.companyName() != null) sb.append(p.companyName());
        } else {
            if (p.firstName() != null) sb.append(p.firstName()).append(" ");
            if (p.lastName() != null) sb.append(p.lastName());
        }
        if (p.city() != null) sb.append(", ").append(p.city());
        return sb.toString().trim();
    }

    /**
     * Produces a single-line human-readable summary of a recipient contact for inclusion
     * in the Gemini prompt.
     *
     * <p>For {@code ORGANIZATION} type contacts the company name is used; for
     * {@code INDIVIDUAL} contacts the salutation, first name, and last name are
     * concatenated. The city is appended when present.</p>
     *
     * @param c the resolved recipient contact; must not be {@code null}
     * @return a trimmed summary string (e.g. {@code "Herr Max Mustermann, Hamburg"} or
     *         {@code "Mustermann AG, Frankfurt"}); never {@code null} but may be empty
     */
    private String formatContact(Contact c) {
        StringBuilder sb = new StringBuilder();
        if (c.type() != null && c.type().name().equals("ORGANIZATION")) {
            if (c.companyName() != null) sb.append(c.companyName());
        } else {
            if (c.salutation() != null) sb.append(c.salutation()).append(" ");
            if (c.firstName() != null) sb.append(c.firstName()).append(" ");
            if (c.lastName() != null) sb.append(c.lastName());
        }
        if (c.city() != null) sb.append(", ").append(c.city());
        return sb.toString().trim();
    }

    /**
     * Assembles the full German-language prompt that is submitted to the Gemini API.
     *
     * <p>The prompt instructs the model to act as a professional German letter writer
     * following DIN 5008 conventions and to return its response as a strict JSON object
     * with {@code success}, {@code title}, and {@code content} fields. Optional sender
     * and recipient lines are appended only when the corresponding info strings are
     * non-blank. The explicit JSON-only constraint minimises post-processing effort
     * and reduces the risk of Markdown fences or preamble text in the response.</p>
     *
     * @param description   the user's natural-language letter description; must not be blank
     * @param senderInfo    a formatted sender summary to include in the prompt, or {@code null}
     *                      to omit the sender line
     * @param recipientInfo a formatted recipient summary to include in the prompt, or {@code null}
     *                      to omit the recipient line
     * @return the complete prompt string ready for submission to the Gemini API
     */
    private String buildPrompt(String description, String senderInfo, String recipientInfo) {
        log.info("Building prompt for description: '{}'", description);
        StringBuilder prompt = new StringBuilder();
        prompt.append("Du bist ein professioneller Briefschreiber für deutsche Geschäftsbriefe nach DIN 5008.\n");
        prompt.append("Erstelle einen vollständigen formellen Brief basierend auf folgender Nutzerbeschreibung.\n\n");
        prompt.append("Beschreibung: ").append(description).append("\n");
        if (senderInfo != null && !senderInfo.isBlank()) {
            prompt.append("Absender: ").append(senderInfo).append("\n");
        }
        if (recipientInfo != null && !recipientInfo.isBlank()) {
            prompt.append("Empfänger: ").append(recipientInfo).append("\n");
        }
        prompt.append("\nWichtig:\n");
        prompt.append("- Erstelle IMMER einen Brief, auch wenn die Beschreibung kurz ist. Nutze sinnvolle Standardformulierungen.\n");
        prompt.append("- Setze success nur dann auf false, wenn die Beschreibung völlig sinnlos oder unleserlich ist (z.B. zufällige Zeichen).\n");
        prompt.append("- Das Feld 'title' enthält NUR den Betreff (eine Zeile, ohne 'Betreff:').\n");
        prompt.append("- Das Feld 'content' enthält NUR den Brieftext ab der Anrede (z.B. 'Sehr geehrte Damen und Herren,') bis zur Grußformel inkl. Unterschrift. KEINE Absenderadresse, KEINE Empfängeradresse, KEIN Datum, KEIN Betreff.\n");
        prompt.append("\nAntworte AUSSCHLIESSLICH mit folgendem JSON (kein Markdown, kein Text davor oder danach):\n");
        prompt.append("{\"success\":true,\"title\":\"Betreff des Briefes\",\"content\":\"Sehr geehrte Damen und Herren,\\n\\n...\"}");

        return prompt.toString();
    }

    /**
     * Submits the given prompt to the configured Google Gemini model and returns
     * the plain-text response fragment.
     *
     * <p>The request body follows the Gemini v1beta {@code generateContent} schema.
     * The response is parsed with Jackson, and the text is extracted from
     * {@code candidates[0].content.parts[0].text}. This method performs no retry
     * logic — transient failures propagate as exceptions to the caller.</p>
     *
     * @param prompt the fully assembled prompt string to send to Gemini
     * @return the raw text from the first candidate response part
     * @throws Exception if the HTTP call fails, the response cannot be parsed, or the
     *                   expected JSON path is absent in the response
     */
    private String callGemini(String prompt) throws Exception {
        log.info("Calling Gemini model='{}', apiKey set={}", model, apiKey != null && !apiKey.isBlank());
        String url = "https://generativelanguage.googleapis.com/v1beta/models/" + model + ":generateContent?key=" + apiKey;

        Map<String, Object> body = Map.of(
                "contents", new Object[]{
                        Map.of("parts", new Object[]{
                                Map.of("text", prompt)
                        })
                }
        );

        String response = restClient.post()
                .uri(url)
                .header("Content-Type", "application/json")
                .body(body)
                .retrieve()
                .body(String.class);

        JsonNode root = objectMapper.readTree(response);
        return root.path("candidates").get(0)
                .path("content").path("parts").get(0)
                .path("text").asText();
    }

    private AiLetterResponse parseResponse(String text) throws Exception {
        log.info("Gemini raw response: {}", text);
        // Extract JSON by finding outermost { ... } — immune to markdown fences or extra text
        int start = text.indexOf('{');
        int end   = text.lastIndexOf('}');
        if (start == -1 || end == -1 || end <= start) {
            log.warn("No JSON object found in Gemini response");
            return new AiLetterResponse(null, null, false);
        }
        String json = text.substring(start, end + 1);
        JsonNode node = objectMapper.readTree(json);
        boolean success = node.path("success").asBoolean(true);
        if (!success) return new AiLetterResponse(null, null, false);
        String title   = node.path("title").asText(null);
        String content = node.path("content").asText(null);
        if (title == null || title.isBlank() || content == null || content.isBlank()) {
            return new AiLetterResponse(null, null, false);
        }
        return new AiLetterResponse(title, content, true);
    }
}
