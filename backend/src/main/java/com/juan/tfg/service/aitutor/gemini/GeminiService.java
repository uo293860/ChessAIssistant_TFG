package com.juan.tfg.service.aitutor.gemini;

import com.fasterxml.jackson.databind.JsonNode;
import com.juan.tfg.service.aitutor.AbstractAITutorService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.util.List;
import java.util.Map;

@Service
@ConditionalOnProperty(name = "ai.provider", havingValue = "gemini", matchIfMissing = true)
public class GeminiService extends AbstractAITutorService {

    private static final String MODEL_NAME = "gemini-3.1-flash-lite";
    private static final String THINKING_LEVEL = "low";
    private static final String GEMINI_API_URL =
            "https://generativelanguage.googleapis.com/v1beta/models/%s:generateContent";

    private final RestClient restClient = RestClient.create();

    @Value("${GEMINI_API_KEY}")
    private String apiKey;

    /**
     * Requests puzzle hints from Gemini and parses the provider response.
     *
     * @param fen the puzzle position in FEN notation.
     * @param solution the puzzle solution moves.
     * @param themes the puzzle themes.
     * @param initialMove the initial opponent move that starts the puzzle.
     * @return generated hint lines or a fallback error hint.
     */
    @Override
    public String[] getHints(String fen, List<String> solution, List<String> themes, String initialMove) {
        try {
            System.out.println("Starting Gemini request...");

            String prompt = getPrompt(fen, solution, themes,  initialMove);

            JsonNode response = restClient.post()
                    .uri(GEMINI_API_URL.formatted(MODEL_NAME))
                    .header("x-goog-api-key", apiKey)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(buildRequestBody(prompt))
                    .retrieve()
                    .body(JsonNode.class);

            return parseHints(extractResponseText(response));

        } catch (Exception e) {
            System.err.println("Error in the connection: " + e.getMessage());
            e.printStackTrace();
        }
        return new String[]{"Error generating hints."};
    }

    /**
     * Builds the Gemini request payload for text generation.
     *
     * @param prompt the prompt to send to Gemini.
     * @return the JSON-serializable request body.
     */
    private Map<String, Object> buildRequestBody(String prompt) {
        return Map.of(
                "contents", List.of(Map.of(
                        "parts", List.of(Map.of("text", prompt))
                )),
                "generationConfig", Map.of(
                        "thinkingConfig", Map.of("thinkingLevel", THINKING_LEVEL)
                )
        );
    }

    /**
     * Extracts non-thought text from the Gemini response.
     *
     * @param response the raw Gemini JSON response.
     * @return the concatenated response text.
     * @throws IllegalStateException if the response does not contain usable text.
     */
    private String extractResponseText(JsonNode response) {
        if (response == null) {
            throw new IllegalStateException("Gemini response is empty.");
        }

        JsonNode parts = response.path("candidates").path(0).path("content").path("parts");
        if (!parts.isArray()) {
            throw new IllegalStateException("Gemini response does not contain text parts.");
        }

        StringBuilder text = new StringBuilder();
        for (JsonNode part : parts) {
            if (!part.path("thought").asBoolean(false) && part.hasNonNull("text")) {
                text.append(part.path("text").asText());
            }
        }

        if (text.isEmpty()) {
            throw new IllegalStateException("Gemini response text is empty.");
        }
        return text.toString();
    }
}
