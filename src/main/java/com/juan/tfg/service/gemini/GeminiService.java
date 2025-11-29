package com.juan.tfg.service.gemini;

import com.google.genai.Client;
import com.google.genai.types.GenerateContentResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class GeminiService {

    // 1. Spring inyecta aquí el valor que pusiste en application.properties
    @Value("${gemini.api.key}")
    private String apiKey;

    public void probarConexion() {
        try {
            System.out.println("🤖 Iniciando petición a Gemini...");

            // 2. Creamos el cliente usando tu clave de Spring
            // Nota: Usamos 'builder().apiKey()' en lugar de 'new Client()' a secas
            // para asegurarnos de que usa la clave del archivo properties.
            Client client = Client.builder()
                    .apiKey(apiKey)
                    .build();

            // 3. Tu código de llamada (con el modelo corregido a 1.5)
            GenerateContentResponse response = client.models.generateContent(
                    "gemini-2.0-flash", // "gemini-2.5" aun no existe publicamente, usa 1.5
                    "Explain how AI works in a few words",
                    null
            );

            // 4. Imprimimos el resultado
            System.out.println("✅ Respuesta recibida:");
            System.out.println(response.text());

        } catch (Exception e) {
            System.err.println("❌ Error en la conexión: " + e.getMessage());
            e.printStackTrace();
        }
    }
}
