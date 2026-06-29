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

    /**
     * Fetches the daily puzzle from the Lichess API.
     *
     * @return the daily puzzle response mapped to a puzzle entity.
     * @throws IOException if the HTTP request or JSON parsing fails.
     * @throws InterruptedException if the HTTP request is interrupted.
     */
    public Puzzle getDailyPuzzle() throws IOException, InterruptedException {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create("https://lichess.org/api/puzzle/daily"))
                .method("GET", HttpRequest.BodyPublishers.noBody())
                .build();

        HttpResponse<String> response = HttpClient.newHttpClient()
                .send(request, HttpResponse.BodyHandlers.ofString());

        ObjectMapper mapper = new ObjectMapper();

        mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);

        Puzzle puzzleResponse = mapper.readValue(response.body(), Puzzle.class);

        return puzzleResponse;
    }
}
