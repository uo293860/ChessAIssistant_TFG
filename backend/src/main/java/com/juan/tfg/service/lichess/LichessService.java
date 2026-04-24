package com.juan.tfg.service.lichess;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.juan.tfg.model.Puzzle;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

@Service
public class LichessService {

    public Puzzle probarConexion() throws IOException, InterruptedException {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create("https://lichess.org/api/puzzle/daily"))
                .method("GET", HttpRequest.BodyPublishers.noBody())
                .build();

        HttpResponse<String> response = HttpClient.newHttpClient()
                .send(request, HttpResponse.BodyHandlers.ofString());

// 2. EL PASO QUE FALTABA: Convertir el JSON (String) a Objetos Java
        ObjectMapper mapper = new ObjectMapper();

// Configuración recomendada: Si Lichess añade campos nuevos en el futuro
// que no tengas en tu clase, esto evita que tu app falle.
        mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);

// "Convierte el cuerpo de la respuesta (String) a mi clase LichessPuzzleResponse"
        Puzzle puzzleResponse = mapper.readValue(response.body(), Puzzle.class);

// 3. AHORA SÍ puedes usar los getters

// Si quieres devolver el objeto para usarlo fuera:
        return puzzleResponse;
    }
}
