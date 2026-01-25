package com.example.backend.controller;

import com.example.backend.service.ChatbotService;
import com.example.backend.service.GeminiService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.ResponseEntity;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class ChatbotControllerTest {

    GeminiService geminiService;
    ChatbotService chatbotService;
    ChatbotController controller;

    @BeforeEach
    void setUp() {
        geminiService = mock(GeminiService.class);
        chatbotService = mock(ChatbotService.class);
        controller = new ChatbotController(geminiService, chatbotService);
    }

    @Test
    void handleMessage_simpleText_returnsText() {
        // build Gemini response with simple text
        var part = new GeminiService.Part("bonjour", null);
        var content = new GeminiService.Content(List.of(part));
        var candidate = new GeminiService.Candidate(content);
        var resp = new GeminiService.GeminiResponse(List.of(candidate));

        when(geminiService.chatWithGemini(anyString())).thenReturn(resp);

        Map<String,String> payload = new HashMap<>();
        payload.put("message","salut");
        payload.put("userId","1");

        ResponseEntity<String> r = controller.handleMessage(payload);
        assertEquals("bonjour", r.getBody());
    }

    @Test
    void handleMessage_functionCall_dispatchesToService() {
        var func = new GeminiService.FunctionCall("cancel_afternoon", Map.of("date","2026-01-25"));
        var part = new GeminiService.Part(null, func);
        var content = new GeminiService.Content(List.of(part));
        var candidate = new GeminiService.Candidate(content);
        var resp = new GeminiService.GeminiResponse(List.of(candidate));

        when(geminiService.chatWithGemini(anyString())).thenReturn(resp);
        when(chatbotService.cancelAfternoon(anyLong(), anyString())).thenReturn("OK CANCEL");

        Map<String,String> payload = new HashMap<>();
        payload.put("message","annule");
        payload.put("userId","2");

        ResponseEntity<String> r = controller.handleMessage(payload);
        assertEquals("OK CANCEL", r.getBody());
        verify(chatbotService).cancelAfternoon(2L, "2026-01-25");
    }
}
