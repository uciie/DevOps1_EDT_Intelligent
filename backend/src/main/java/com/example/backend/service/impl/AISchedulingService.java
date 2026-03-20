package com.example.backend.service.impl;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.example.backend.record.AIPlanningResponse;
import com.example.backend.record.AIHistoryRequest;
import com.example.backend.service.strategy.SmartScheduler;

@Service
public class AISchedulingService {

    @Autowired
    private SmartScheduler aiClient; // Ton interface LangChain4j

    // Cas 1 : Création initiale (Endpoint /test-ai)
    public AIPlanningResponse processRequest(String userContent) {
        // Simple appel sans mémoire côté serveur
        return aiClient.chat(userContent);
    }

    // Cas 2 : La boucle d'échange (Endpoint /refine-ai)
    public AIPlanningResponse refineRequest(AIHistoryRequest request) {
        // C'est ici que l'échange de flux a lieu :
        // On envoie à l'IA ce qu'on a reçu dans le corps de la requête
        return aiClient.refine(request.currentTasks(), request.userFeedback());
    }
}