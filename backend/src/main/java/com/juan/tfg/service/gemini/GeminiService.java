package com.juan.tfg.service.gemini;

import com.google.genai.Client;
import com.google.genai.types.GenerateContentResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class GeminiService {

    // 1. Spring inyecta aquí el valor que pusiste en application.properties
    @Value("${gemini.api.key}")
    private String apiKey;

    public void probarConexion(String fen, List<String> solution, List<String> themes) {
        try {
            System.out.println("🤖 Iniciando petición a Gemini...");

            // 2. Creamos el cliente usando tu clave de Spring
            // Nota: Usamos 'builder().apiKey()' en lugar de 'new Client()' a secas
            // para asegurarnos de que usa la clave del archivo properties.
            Client client = Client.builder()
                    .apiKey(apiKey)
                    .build();

            String prompt = "You are an expert chess coach and puzzle instructor. Carefully analyze the following chess position and provide exactly three hints to help solve the problem.\n" +
                    "\n" +
                    "The hints must be progressively more specific:\n" +
                    "\n" +
                    "Hint 1 should be very general and focus on high-level evaluation or strategic ideas.\n" +
                    "\n" +
                    "Hint 2 should narrow the focus to relevant tactical or positional themes.\n" +
                    "\n" +
                    "Hint 3 should make the solution quite clear, while still not explicitly stating the exact move.\n" +
                    "\n" +
                    "Do NOT mention any specific move, square, or explicit piece movement.\n" +
                    "\n" +
                    "The hints must focus on advanced chess concepts such as king safety, piece activity, weak squares, pins, skewers, discovered attacks, overloaded pieces, coordination, or tactical motifs — not on “which piece to move.”\n" +
                    "\n" +
                    "Use precise chess terminology and the correct names of the pieces (pawn, knight, bishop, rook, queen, king).\n" +
                    "\n" +
                    "Write each hint as a short phrase, clearly labeled as Hint 1, Hint 2, and Hint 3.\n" +
                    "\n" +
                    "The tone should be instructive and clear, as if teaching a strong club player.\n" +
                    "\n" +
                    "The position is given in FEN format:" + fen + " and the solution is " + solution + "\n" +
                    "You can use the themes of the problem as a guide: " + themes;

            // 3. Tu código de llamada (con el modelo corregido a 1.5)
            GenerateContentResponse response = client.models.generateContent(
                    "gemini-2.5-flash",
                    prompt,
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
