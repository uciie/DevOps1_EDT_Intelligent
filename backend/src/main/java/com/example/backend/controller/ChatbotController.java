package com.example.backend.controller;

import java.util.Map;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.example.backend.exception.QuotaExceededException;
import com.example.backend.service.ChatbotService;

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
        } catch (QuotaExceededException e) {
            // Renvoie un code 429 avec le message explicite
            return ResponseEntity.status(429).body("⚠️ Oups ! Je suis surchargé (Quota dépassé). Revenez dans quelques instants.");
        } catch (Exception e) {
            // Log de l'erreur
            System.err.println("Erreur ChatbotController: " + e.getMessage());
            return ResponseEntity.internalServerError().body("Une erreur interne est survenue.");
        }
    }
}