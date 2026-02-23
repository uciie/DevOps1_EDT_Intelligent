package com.example.backend.service.impl;

import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.ArrayList;

// Si tu utilises LangChain4j pour ChatMessage :
import dev.langchain4j.data.message.ChatMessage;
import dev.langchain4j.data.message.UserMessage;
import dev.langchain4j.data.message.AiMessage;

@Service
public class AIChatHistoryService {
    private final Map<String, List<ChatMessage>> histories = new HashMap<>();

    public List<ChatMessage> getHistory(String sessionId) {
        return histories.computeIfAbsent(sessionId, k -> new ArrayList<>());
    }

    public void addMessage(String sessionId, String role, String content) {
        List<ChatMessage> history = getHistory(sessionId);
        if ("user".equalsIgnoreCase(role)) {
            history.add(UserMessage.from(content));
        } else if ("assistant".equalsIgnoreCase(role)) {
            history.add(AiMessage.from(content));
        }
    }
}
