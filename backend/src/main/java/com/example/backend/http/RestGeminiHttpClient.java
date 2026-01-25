package com.example.backend.http;

import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

@Service
public class RestGeminiHttpClient implements GeminiHttpClient {

    private final WebClient webClient;

    public RestGeminiHttpClient(WebClient.Builder webClientBuilder) {
        this.webClient = webClientBuilder.build();
    }

@Override
public <T> T generateContent(String model, String apiKey, Object body, Class<T> responseType) {
    // On s'assure que le nom du modèle ne contient pas déjà "models/" pour éviter les doublons
    String modelPath = model.startsWith("models/") ? model : "models/" + model;
    
    // Utilisation de v1beta (indispensable pour les 'tools')
    String url = String.format("https://generativelanguage.googleapis.com/v1beta/%s:generateContent?key=%s", modelPath, apiKey);

    return webClient.post()
            .uri(url)
            .contentType(MediaType.APPLICATION_JSON)
            .bodyValue(body)
            .retrieve()
            .onStatus(status -> status.isError(), resp -> resp.bodyToMono(String.class)
                    .flatMap(msg -> Mono.error(new RuntimeException("Gemini API error: " + msg))))
            .bodyToMono(responseType)
            .block();
}
}