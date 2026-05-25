package com.juan.tfg.service.gemini;

import com.google.genai.Client;
import com.google.genai.types.GenerateContentResponse;
import com.juan.tfg.service.AbstractAITutorService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@ConditionalOnProperty(name = "ai.provider", havingValue = "gemini", matchIfMissing = true)
public class GeminiService extends AbstractAITutorService {

    // 1. Spring inyecta aquí el valor que pusiste en application.properties
    @Value("${GEMINI_API_KEY}")
    private String apiKey;

    @Override
    public String[] getHints(String fen, List<String> solution, List<String> themes) {
        try {
            System.out.println("Starting Gemini request...");

            Client client = Client.builder()
                    .apiKey(apiKey)
                    .build();

            String prompt = getPrompt(fen, solution, themes);

            GenerateContentResponse response = client.models.generateContent(
                    "gemini-3.1-flash-lite",
                    prompt,
                    null
            );
            return parseHints(response.text());

        } catch (Exception e) {
            System.err.println("Error in the connection: " + e.getMessage());
            e.printStackTrace();
        }
        return new String[]{"Error generating hints."};
    }


}
