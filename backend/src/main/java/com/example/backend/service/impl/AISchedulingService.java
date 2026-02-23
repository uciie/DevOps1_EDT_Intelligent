package com.example.backend.service.impl;

import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.example.backend.record.AIPlanningResponse;
import com.example.backend.service.strategy.SmartScheduler;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.util.HashMap;
import java.util.ArrayList;

// Si tu utilises LangChain4j pour ChatMessage :
import dev.langchain4j.data.message.ChatMessage;
import dev.langchain4j.data.message.UserMessage;
import dev.langchain4j.data.message.AiMessage;

@Service
public class AISchedulingService {

    @Autowired
    private AIChatHistoryService historyService;

    @Autowired
    private SmartScheduler aiClient; // Ton interface LangChain4j

    public AIPlanningResponse processRequest(String sessionId, String userContent) {
        // 1. Ajouter le message de l'utilisateur à l'historique
        historyService.addMessage(sessionId, "user", userContent);

        // 2. Appel à l'IA via l'interface intelligente
        // Note : LangChain4j gère le SystemPrompt, l'historique et le JSON automatiquement 
        // si ton interface SmartScheduler est bien annotée.
        AIPlanningResponse response = aiClient.chat(sessionId, userContent);

        // 3. L'historique est normalement géré automatiquement par le ChatMemory de LangChain4j
        // mais si tu veux le faire manuellement :
        historyService.addMessage(sessionId, "assistant", response.globalExplanation());

        return response;
    }
}