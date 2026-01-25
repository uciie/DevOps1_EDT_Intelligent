package com.example.backend.controller;

import com.example.backend.service.ChatbotService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/chat")
public class ChatbotController {

    private final ChatbotService chatbotService;

    // On n'a plus besoin de GeminiService ici, car ChatbotService s'en occupe
    public ChatbotController(ChatbotService chatbotService) {
        this.chatbotService = chatbotService;
    }

    @PostMapping("/message")
    public ResponseEntity<String> handleMessage(@RequestBody Map<String, String> payload) {
        try {
            // Extraction des données du payload
            String userMessage = payload.get("message");
            String userIdStr = payload.get("userId");

            if (userMessage == null || userIdStr == null) {
                return ResponseEntity.badRequest().body("Message ou UserId manquant.");
            }

            Long userId = Long.parseLong(userIdStr);

            // 1. Délégation totale au ChatbotService
            // handleUserRequest s'occupe d'appeler Gemini, d'analyser l'intention 
            // et d'exécuter l'action (SQL/Repository).
            String response = chatbotService.handleUserRequest(userId, userMessage);

            return ResponseEntity.ok(response);

        } catch (NumberFormatException e) {
            return ResponseEntity.badRequest().body("ID utilisateur invalide.");
        } catch (Exception e) {
            // Log de l'erreur
            System.err.println("Erreur ChatbotController: " + e.getMessage());
            return ResponseEntity.internalServerError().body("Une erreur interne est survenue.");
        }
    }
}