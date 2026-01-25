package com.example.backend.controller;

import com.example.backend.service.ChatbotService;
import com.example.backend.service.GeminiService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/chat")
public class ChatbotController {

    private final GeminiService geminiService;
    private final ChatbotService chatbotService;

    public ChatbotController(GeminiService geminiService, ChatbotService chatbotService) {
        this.geminiService = geminiService;
        this.chatbotService = chatbotService;
    }

    @PostMapping("/message")
    public ResponseEntity<String> handleMessage(@RequestBody Map<String, String> payload) {
        String userMessage = payload.get("message");
        Long userId = Long.parseLong(payload.get("userId"));

        // 1. Appel à Gemini pour comprendre l'intention
        GeminiService.GeminiResponse response = geminiService.chatWithGemini(userMessage);

        // Sécurité : Vérifier si une réponse valide existe
        if (response.candidates() == null || response.candidates().isEmpty()) {
            return ResponseEntity.ok("Désolé, je n'arrive pas à joindre mon cerveau (API Error).");
        }

        GeminiService.Part firstPart = response.candidates().get(0).content().parts().get(0);

        // 2. Cas : Function Calling détecté (Action concrète)
        if (firstPart.functionCall() != null) {
            String functionName = firstPart.functionCall().name();
            Map<String, Object> args = firstPart.functionCall().args();

            // Dispatching dynamique vers le service métier
            return switch (functionName) {
                case "cancel_afternoon" -> ResponseEntity.ok(
                        chatbotService.cancelAfternoon(userId, (String) args.get("date"))
                );
                case "move_activity" -> ResponseEntity.ok(
                        chatbotService.moveActivity(userId, 
                                (String) args.get("activityName"), 
                                (String) args.get("targetDate"), 
                                (String) args.get("targetTime"))
                );
                case "add_task" -> ResponseEntity.ok(
                        chatbotService.addTask(userId, 
                                (String) args.get("name"), 
                                ((Number) args.get("durationMinutes")).intValue()) // Cast safe pour JSON number
                );
                default -> ResponseEntity.ok("J'ai identifié une action inconnue : " + functionName);
            };
        }

        // 3. Cas : Conversation simple (Pas d'action)
        return ResponseEntity.ok(firstPart.text());
    }
}