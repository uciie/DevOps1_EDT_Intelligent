package com.example.backend.service;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import com.example.backend.http.GeminiHttpClient;

import java.util.List;
import java.util.Map;
import java.util.HashMap;

@Service
public class GeminiService {

    @Value("${google.ai.api-key}")
    private String apiKey;

    @Value("${google.ai.model}")
    private String model;

    private final GeminiHttpClient httpClient;

    public GeminiService(GeminiHttpClient httpClient, ObjectMapper objectMapper) {
        this.httpClient = httpClient;
    }

    public GeminiResponse chatWithGemini(String userMessage) {
        // Construction de la requête
        Map<String, Object> requestBody = new HashMap<>();
        
        // 1. Message utilisateur
        requestBody.put("contents", List.of(
            Map.of("parts", List.of(Map.of("text", userMessage)))
        ));

        // 2. Outils (Fonctions de planification)
        var functionDeclarations = List.of(
            defineTool("cancel_afternoon", "Annule les événements d'un après-midi donné.",
                Map.of("date", Map.of("type", "STRING", "description", "Date YYYY-MM-DD")), List.of("date")),
            defineTool("move_activity", "Déplace une activité.",
                Map.of(
                    "activityName", Map.of("type", "STRING", "description", "Nom de l'activité"),
                    "targetDate", Map.of("type", "STRING", "description", "Nouvelle date YYYY-MM-DD")
                ), List.of("activityName", "targetDate")),
            defineTool("add_task", "Ajoute une nouvelle tâche au backlog.",
                Map.of(
                    "name", Map.of("type", "STRING", "description", "Nom de la tâche"),
                    "durationMinutes", Map.of("type", "INTEGER", "description", "Durée en minutes")
                ), List.of("name", "durationMinutes"))
        );
        
        requestBody = new HashMap<>();
        requestBody.put("contents", List.of(
            Map.of("parts", List.of(Map.of("text", userMessage)))
        ));
        //requestBody.put("tools", List.of(Map.of("function_declarations", functionDeclarations)));

        return httpClient.generateContent(model, apiKey, requestBody, GeminiResponse.class);
    }

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

    // --- DTOs ---
    @JsonIgnoreProperties(ignoreUnknown = true)
    public record GeminiResponse(List<Candidate> candidates) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Candidate(Content content) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Content(List<Part> parts) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Part(
        String text, 
        @JsonInclude(JsonInclude.Include.NON_NULL)
        FunctionCall functionCall
    ) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record FunctionCall(String name, Map<String, Object> args) {}
}