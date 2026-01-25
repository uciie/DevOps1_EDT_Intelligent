package com.example.backend.service;

import com.example.backend.service.GeminiService.GeminiResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Assumptions;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.reactive.function.client.WebClient;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.JsonNode;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
class GeminiServiceIntegrationTest {

    @Autowired
    private GeminiService geminiService;

    // Récupère la clé depuis application.properties
    @Value("${google.ai.api-key:}")
    private String configuredKey;

    @BeforeEach
    void beforeEach() {
        // On essaie de récupérer la clé (Priorité variable d'env, sinon config)
        String envKey = System.getenv("CHATBOT_API_KEY");
        String key = (envKey != null && !envKey.isEmpty()) ? envKey : configuredKey;
        // Si aucune clé n'est trouvée, on skip le test
        Assumptions.assumeTrue(key != null && !key.isEmpty(), "Clé API manquante (CHATBOT_API_KEY ou google.ai.api-key)");

        // On injecte la clé dans le service pour le test
        ReflectionTestUtils.setField(geminiService, "apiKey", key);
    }

    @Test
    void testChatWithGemini_realCall() {
        String prompt = "Bonjour, réponds simplement 'OK'.";
        String key = (String) ReflectionTestUtils.getField(geminiService, "apiKey");

        // 1) Vérification large des modèles pour ne pas être ignoré
        WebClient client = WebClient.builder().build();
        String listUrl = "https://generativelanguage.googleapis.com/v1beta/models?key=" + key;
        
        try {
            String listJson = client.get().uri(listUrl).retrieve().bodyToMono(String.class).block();
            // On vérifie simplement si le mot "gemini" existe dans la liste renvoyée par Google
            boolean modelFound = listJson.toLowerCase().contains("gemini");
            
            Assumptions.assumeTrue(modelFound, "Aucun modèle Gemini disponible pour cette clé.");
        } catch (Exception e) {
            fail("Erreur de connexion à l'API Google : " + e.getMessage());
        }

        // 2) Appel réel au service
        try {
            GeminiResponse resp = geminiService.chatWithGemini(prompt);
            
            assertNotNull(resp, "La réponse ne doit pas être nulle");
            
            // CORRECTION ICI : Pour un record, on utilise .candidates() et non .getCandidates()
            assertNotNull(resp.candidates(), "La liste des candidats ne doit pas être nulle");
            assertFalse(resp.candidates().isEmpty(), "La réponse doit contenir au moins un candidat");

            // Affichage du texte pour confirmer le fonctionnement dans la console
            String reply = resp.candidates().get(0).content().parts().get(0).text();
            System.out.println("Gemini a répondu : " + reply);
            
        } catch (RuntimeException e) {
            // Si on dépasse le quota gratuit (429), on skip le test au lieu de le faire échouer
            if (e.getMessage().contains("429") || e.getMessage().contains("RESOURCE_EXHAUSTED")) {
                Assumptions.assumeTrue(false, "Quota API dépassé, test ignoré.");
            } else {
                throw e;
            }
        }
    }
}