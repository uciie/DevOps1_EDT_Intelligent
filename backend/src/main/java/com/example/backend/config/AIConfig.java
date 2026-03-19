package com.example.backend.config;

import com.example.backend.service.strategy.SmartScheduler;
import dev.langchain4j.model.openai.OpenAiChatModel; // On utilise le protocole OpenAI pour Groq
import dev.langchain4j.service.AiServices;
import dev.langchain4j.memory.chat.MessageWindowChatMemory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.beans.factory.annotation.Value;

@Configuration
public class AIConfig {

    // On récupère les nouvelles clés Groq
    @Value("${groq.api.key}")
    private String apiKey;

    @Value("${groq.model}")
    private String modelName;

    @Bean
    public SmartScheduler smartScheduler() {
        // 1. On configure le modèle via Groq (en mode compatible OpenAI)
        OpenAiChatModel model = OpenAiChatModel.builder()
                .apiKey(apiKey)
                .baseUrl("https://api.groq.com/openai/v1") // C'est l'adresse pour parler à Groq
                .modelName(modelName)
                .logRequests(true) 
                .logResponses(true)
                .build();

        // 2. On crée le service qui lie l'interface au modèle avec une mémoire
        // Le reste du code ne change pas, LangChain4j s'occupe de la transition !
        return AiServices.builder(SmartScheduler.class)
                .chatLanguageModel(model)
                .chatMemoryProvider(sessionId -> MessageWindowChatMemory.builder()
                        .id(sessionId)
                        .maxMessages(20) 
                        .build())
                .build();
    }
}