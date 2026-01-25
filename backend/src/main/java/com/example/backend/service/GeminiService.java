package com.example.backend.service;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.util.List;
import java.util.Map;

@Service
public class GeminiService {

    @Value("${google.ai.api-key}")
    private String apiKey;

    @Value("${google.ai.model}")
    private String model;

    private final RestClient restClient;
    private final ObjectMapper objectMapper;

    public GeminiService(RestClient.Builder restClientBuilder, ObjectMapper objectMapper) {
        this.restClient = restClientBuilder.baseUrl("https://generativelanguage.googleapis.com/v1beta/models").build();
        this.objectMapper = objectMapper;
    }

    public GeminiResponse chatWithGemini(String userMessage) {
        // 1. Définition des Outils (Function Definitions)
        var tools = List.of(Map.of("function_declarations", List.of(
            defineTool("cancel_afternoon", "Annule les événements d'un après-midi donné.",
                Map.of("date", Map.of("type", "STRING", "description", "Date YYYY-MM-DD")), List.of("date")),
            defineTool("move_activity", "Déplace une activité.",
                Map.of(
                    "activityName", Map.of("type", "STRING", "description", "Nom de l'activité"),
                    "targetDate", Map.of("type", "STRING", "description", "Nouvelle date YYYY-MM-DD"),
                    "targetTime", Map.of("type", "STRING", "description", "Nouvelle heure HH:mm")
                ), List.of("activityName", "targetDate", "targetTime")),
            defineTool("add_task", "Ajoute une nouvelle tâche à optimiser.",
                Map.of(
                    "name", Map.of("type", "STRING", "description", "Nom de la tâche"),
                    "durationMinutes", Map.of("type", "INTEGER", "description", "Durée en minutes")
                ), List.of("name", "durationMinutes"))
        )));

        // 2. Construction du Payload
        var requestBody = Map.of(
            "contents", List.of(Map.of("parts", List.of(Map.of("text", userMessage)))),
            "tools", tools
        );

        // 3. Appel API
        return restClient.post()
                .uri(uriBuilder -> uriBuilder.path("/" + model + ":generateContent").queryParam("key", apiKey).build())
                .contentType(MediaType.APPLICATION_JSON)
                .body(requestBody)
                .retrieve()
                .body(GeminiResponse.class);
    }

    // Helper pour construire la structure JSON verbeuse de Gemini
    private Map<String, Object> defineTool(String name, String desc, Map<String, Object> props, List<String> required) {
        return Map.of(
            "name", name,
            "description", desc,
            "parameters", Map.of(
                "type", "OBJECT",
                "properties", props,
                "required", required
            )
        );
    }

    // --- DTOs pour Mapper la réponse JSON de Gemini ---
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public record GeminiResponse(List<Candidate> candidates) {}
    public record Candidate(Content content) {}
    public record Content(List<Part> parts) {}
    public record Part(String text, FunctionCall functionCall) {}
    public record FunctionCall(String name, Map<String, Object> args) {}
}