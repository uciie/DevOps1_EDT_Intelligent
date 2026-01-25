package com.example.backend.service;

import com.example.backend.http.GeminiHttpClient;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class GeminiServiceTest {

    private GeminiHttpClient client;
    private GeminiService service;

    @BeforeEach
    void setUp() throws Exception {
        client = mock(GeminiHttpClient.class);
        // Le constructeur attend (GeminiHttpClient)
        service = new GeminiService(client);
        
        // On injecte les valeurs @Value par réflexion pour le test
        var apiField = service.getClass().getDeclaredField("apiKey");
        apiField.setAccessible(true);
        apiField.set(service, "test-api-key");
        
        var modelField = service.getClass().getDeclaredField("model");
        modelField.setAccessible(true);
        modelField.set(service, "gemini-1.5-flash");
    }

    @Test
    void chatWithGemini_returnsValidResponse() {
        // Mock d'une réponse texte simple
        var part = new GeminiService.Part("Test OK", null);
        var content = new GeminiService.Content(List.of(part));
        var candidate = new GeminiService.Candidate(content);
        var expected = new GeminiService.GeminiResponse(List.of(candidate));

        when(client.generateContent(anyString(), anyString(), any(), eq(GeminiService.GeminiResponse.class)))
            .thenReturn(expected);

        var response = service.chatWithGemini("Hello");

        assertNotNull(response);
        assertEquals("Test OK", response.candidates().get(0).content().parts().get(0).text());
    }
}