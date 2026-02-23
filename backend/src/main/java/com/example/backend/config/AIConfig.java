package com.example.backend.config;



import  com.example.backend.service.strategy.SmartScheduler;
import dev.langchain4j.model.anthropic.AnthropicChatModel;
import dev.langchain4j.service.AiServices;
import dev.langchain4j.store.memory.chat.ChatMemoryStore;
import dev.langchain4j.store.memory.chat.InMemoryChatMemoryStore;
import dev.langchain4j.memory.chat.MessageWindowChatMemory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.beans.factory.annotation.Value;

@Configuration
public class AIConfig {

    // On récupère la clé API depuis ton fichier .env ou variables d'environnement
    @Value("${anthropic.api.key}")
    private String apiKey;

    @Bean
    public SmartScheduler smartScheduler() {
        // 1. On configure le modèle Claude
        AnthropicChatModel model = AnthropicChatModel.builder()
                .apiKey(apiKey)
                .modelName("claude-3-5-sonnet-20240620")
                .logRequests(true) // Utile pour voir ce qui est envoyé à Claude dans la console
                .logResponses(true)
                .build();

        // 2. On crée le service qui lie l'interface au modèle avec une mémoire
        return AiServices.builder(SmartScheduler.class)
                .chatLanguageModel(model)
                .chatMemoryProvider(sessionId -> MessageWindowChatMemory.builder()
                        .id(sessionId)
                        .maxMessages(20) // Se souvient des 20 derniers messages du chat
                        .build())
                .build();
    }
}