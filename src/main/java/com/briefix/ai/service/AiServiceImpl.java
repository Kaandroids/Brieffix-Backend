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

@Service
public class AiServiceImpl implements AiService {

    private static final Logger log = LoggerFactory.getLogger(AiServiceImpl.class);

    private final UserRepository userRepository;
    private final ProfileRepository profileRepository;
    private final ContactRepository contactRepository;
    private final RestClient restClient;
    private final ObjectMapper objectMapper;

    @Value("${app.gemini.api-key}")
    private String apiKey;

    @Value("${app.gemini.model}")
    private String model;

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

    @Override
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

    private String buildPrompt(String description, String senderInfo, String recipientInfo) {
        StringBuilder prompt = new StringBuilder();
        prompt.append("Du bist ein professioneller Briefschreiber für deutsche Geschäftsbriefe nach DIN 5008.\n");
        prompt.append("Erstelle einen formellen Brief basierend auf folgender Nutzerbeschreibung.\n\n");
        prompt.append("Beschreibung: ").append(description).append("\n");
        if (senderInfo != null && !senderInfo.isBlank()) {
            prompt.append("Absender: ").append(senderInfo).append("\n");
        }
        if (recipientInfo != null && !recipientInfo.isBlank()) {
            prompt.append("Empfänger: ").append(recipientInfo).append("\n");
        }
        prompt.append("\nAntworte AUSSCHLIESSLICH mit folgendem JSON (kein Markdown, kein Text davor oder danach).\n");
        prompt.append("Wenn die Beschreibung zu unklar, sinnlos oder zu kurz ist um einen Brief zu erstellen, setze success auf false und lasse title und content leer.\n");
        prompt.append("{\"success\":true,\"title\":\"Betreff des Briefes\",\"content\":\"Vollständiger Brieftext hier...\"}");
        return prompt.toString();
    }

    private String callGemini(String prompt) throws Exception {
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
