package com.example.backend.service;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.example.backend.http.GeminiHttpClient;
import com.example.backend.model.ChatMessage;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;

@Service
public class GeminiService {

    @Value("${google.ai.api-key}")
    private String apiKey;

    @Value("${google.ai.model}")
    private String model;

    private final GeminiHttpClient httpClient;

    public GeminiService(GeminiHttpClient httpClient) {
        this.httpClient = httpClient;
    }

    public GeminiResponse chatWithGemini(String userMessage, List<ChatMessage> history) {
        // Injection de la date actuelle dans le prompt système
        String currentDate = LocalDate.now().format(DateTimeFormatter.ofPattern("dd/MM/yyyy (EEEE)", java.util.Locale.FRENCH));
        
        String systemPrompt = String.format("""
            Tu es un assistant de planification intelligent. Ton rôle est d'aider l'utilisateur à gérer son emploi du temps (événements et tâches).
            La date actuelle est : %s.

            Définitions des plages horaires :
            - Matin : 08h00 - 11h00
            - Midi : 12h00 - 14h00
            - Après-midi : 15h00 - 18h00
            - Soir : 19h00 - 23h00

            Tu dois impérativement utiliser les fonctions disponibles pour :
            - Lister les événements : entre deux dates (`list_events_between`), sur un créneau (`list_events_slot`).
            - Gérer les tâches : lister par priorité (`list_tasks_by_priority`), lister tout (`list_all_tasks`), ajouter (`add_task`), annuler par ID (`cancel_task`) ou par priorité (`cancel_tasks_by_priority`).
            - Modifier l'agenda : annuler un matin (`cancel_morning`),un midi ('cancel_noon'), un après-midi  (`cancel_afternoon`), ou un créneau précis (`cancel_events_slot`).
            - Organiser : déplacer une activité (`move_activity`) ou ajouter un événement (`add_event`).
            
            Règles pour l'ajout d'événements :
            1. Tu peux ajouter PLUSIEURS événements en une seule fois via `add_events_batch`.
            2. Si l'utilisateur demande une RÉCURRENCE (ex: "Sport tous les lundis ce mois-ci"), tu dois CALCULER toutes les dates précises et les envoyer via `add_events_batch`.
            3. Tu dois ASSIGNER une catégorie à chaque événement parmi : WORK, STUDY, SPORT, LEISURE, OTHER. Déduis la catégorie selon le titre (ex: "Réunion" -> WORK, "Foot" -> SPORT).

            Instructions :
            1. Formate toujours les dates et heures en ISO-8601 (YYYY-MM-DDTHH:mm:ss).
            2. Si une information est manquante (ex: l'heure pour un événement), demande-la précisément.
            3. Confirme toujours l'action réalisée à l'utilisateur de manière concise.
            """, currentDate);
        
        List<Map<String, Object>> contents = new ArrayList<>();

        contents.add(Map.of("role", "user", "parts", List.of(Map.of("text", systemPrompt))));
        contents.add(Map.of("role", "model", "parts", List.of(Map.of("text", "Compris. J'utiliserai les fonctions appropriées..."))));

        List<ChatMessage> chronologicalHistory = new ArrayList<>(history);
        Collections.reverse(chronologicalHistory); // Remet les messages dans l'ordre chronologique (ancien -> récent)

        for (ChatMessage msg : chronologicalHistory) {
            contents.add(Map.of(
                "role", msg.getRole(),
                "parts", List.of(Map.of("text", msg.getContent()))
            ));
        }

        contents.add(Map.of("role", "user", "parts", List.of(Map.of("text", userMessage))));

        // Définition des outils disponibles
        var functionDeclarations = List.of(
            defineTool("list_today_events", 
                "Liste tous les événements prévus aujourd'hui pour l'utilisateur.",
                Map.of(), // Pas de paramètres
                List.of()),
            
            defineTool("cancel_afternoon", 
                "Annule tous les événements de l'après-midi (15h-18h) d'une date donnée.",
                Map.of("date", Map.of(
                    "type", "STRING", 
                    "description", "Date au format YYYY-MM-DD (ex: 2025-01-25)"
                )), 
                List.of("date")),
            
            defineTool("cancel_morning", 
                "Annule tous les événements du matin (08h-11h) d'une date donnée.",
                Map.of("date", Map.of(
                    "type", "STRING", 
                    "description", "Date au format YYYY-MM-DD"
                )), 
                List.of("date")),

            defineTool("cancel_noon", 
            "Annule tous les événements du midi (12h-14h).",
                Map.of("date", Map.of("type", "STRING", "description", "Date YYYY-MM-DD")), 
                List.of("date")),
            
            defineTool("move_activity", 
                "Déplace une activité vers une nouvelle date et heure.",
                Map.of(
                    "activityName", Map.of(
                        "type", "STRING", 
                        "description", "Nom ou partie du nom de l'activité à déplacer"
                    ),
                    "targetDate", Map.of(
                        "type", "STRING", 
                        "description", "Nouvelle date au format YYYY-MM-DD"
                    ),
                    "targetTime", Map.of(
                        "type", "STRING", 
                        "description", "Nouvelle heure au format HH:mm (ex: 14:30)"
                    )
                ), 
                List.of("activityName", "targetDate", "targetTime")),
            
            defineTool("add_task", 
                "Ajoute une nouvelle tâche au backlog de l'utilisateur.",
                Map.of(
                    "name", Map.of(
                        "type", "STRING", 
                        "description", "Nom ou description de la tâche"
                    ),
                    "durationMinutes", Map.of(
                        "type", "INTEGER", 
                        "description", "Durée estimée en minutes (ex: 60 pour 1h)"
                    )
                ), 
                List.of("name", "durationMinutes")),
            defineTool("list_events_between", 
                "Liste les événements compris dans une plage horaire donnée.",
                Map.of(
                    "start", Map.of("type", "STRING", "description", "Début de la plage (ISO-8601 ex: 2025-01-25T14:00:00)"),
                    "end", Map.of("type", "STRING", "description", "Fin de la plage (ISO-8601)")
                ),
                List.of("start", "end")),
            
            defineTool("add_event", 
                "Ajoute un événement au calendrier.",
                Map.of(
                    "summary", Map.of("type", "STRING", "description", "Titre de l'événement"),
                    "start", Map.of("type", "STRING", "description", "Date de début (ISO-8601)"),
                    "end", Map.of("type", "STRING", "description", "Date de fin (ISO-8601)")
                ), 
                List.of("summary", "start", "end")),
            
            defineTool("list_tasks_by_priority", 
                "Liste les tâches ayant une priorité spécifique.",
                Map.of("priority", Map.of("type", "INTEGER", "description", "Niveau de priorité (1, 2 ou 3)")), 
                List.of("priority")),

            defineTool("find_task_by_name", 
                "Trouve une tâche en cherchant par son nom.",
                Map.of("name", Map.of("type", "STRING", "description", "Nom ou partie du nom de la tâche")), 
                List.of("name")),
                
            defineTool("cancel_evening", 
            "Annule tous les événements du soir (19h-23h).",
                Map.of("date", Map.of("type", "STRING", "description", "Date YYYY-MM-DD")), 
                List.of("date"))


        );
        
        functionDeclarations.add(Map.of(
        "name", "add_events_batch",
        "description", "Ajoute une liste d'événements en une seule fois. Utile pour les récurrences ou les ajouts multiples.",
        "parameters", Map.of(
            "type", "OBJECT",
            "properties", Map.of(
                "events", Map.of(
                    "type", "ARRAY",
                    "description", "Liste des événements à ajouter",
                    "items", Map.of(
                        "type", "OBJECT",
                        "properties", Map.of(
                            "summary", Map.of("type", "STRING", "description", "Titre de l'événement"),
                            "start", Map.of("type", "STRING", "description", "Date de début ISO-8601"),
                            "end", Map.of("type", "STRING", "description", "Date de fin ISO-8601"),
                            "category", Map.of("type", "STRING", "description", "Catégorie (WORK, STUDY, SPORT, LEISURE, OTHER)")
                        ),
                        "required", List.of("summary", "start", "end", "category")
                    )
                )
            ),
            "required", List.of("events")
        )
    ));

        // Payload final avec outils activés
        var requestBody = Map.of(
            "contents", contents,
            "tools", List.of(Map.of("function_declarations", functionDeclarations))
        );

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