package com.example.backend.service.strategy;



import dev.langchain4j.service.SystemMessage;
import dev.langchain4j.service.MemoryId;
import dev.langchain4j.service.UserMessage;
import com.example.backend.record.AIPlanningResponse;

public interface SmartScheduler {

    @SystemMessage("""
        Tu es un expert en planification. 
        Analyse le document, estime les durées et trouve les dépendances.
        Utilise le 'Chain of Thought' pour expliquer tes choix.
        Réponds TOUJOURS au format JSON.
        """)
    AIPlanningResponse chat(@MemoryId String sessionId, @UserMessage String content);
}