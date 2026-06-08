package com.juan.tfg.service.aitutor.gemini;

import com.juan.tfg.service.aitutor.AbstractAITutorService;
import lombok.Getter;
import lombok.Setter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@ConditionalOnProperty(name = "ai.provider", havingValue = "gemma")
public class GemmaService extends AbstractAITutorService {

    @Value("${ollama.api.url}")
    private String ollamaUrl;

    @Value("${ollama.model}")
    private String modelName;


    private final RestTemplate restTemplate = new RestTemplate();

    @Override
    public String[] getHints(String fen, List<String> solution, List<String> themes) {
        String prompt = getPrompt(fen, solution, themes);
        Map<String, Object> requestBody = new HashMap<>();
        requestBody.put("model", modelName);
        requestBody.put("prompt", prompt);
        requestBody.put("stream", false);

        try {
            OllamaResponse response = restTemplate.postForObject(ollamaUrl, requestBody, OllamaResponse.class);

            if (response != null && response.getResponse() != null) {
                return parseHints(response.getResponse());
            } else {
                throw new RuntimeException("La respuesta de Ollama está vacía.");
            }

        } catch (Exception e) {
            System.err.println("Error comunicándose con Ollama local: " + e.getMessage());
            throw new RuntimeException("El tutor local no está disponible en este momento.");
        }
    }

    @Setter
    @Getter
    private static class OllamaResponse {
        private String response;

    }
}
