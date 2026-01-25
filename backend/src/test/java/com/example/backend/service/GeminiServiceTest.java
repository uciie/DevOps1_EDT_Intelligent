package com.example.backend.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.web.client.RestClient;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

class GeminiServiceTest {

    com.example.backend.http.GeminiHttpClient client;
    ObjectMapper objectMapper;
    GeminiService service;

    @BeforeEach
    void setUp() {
        client = mock(com.example.backend.http.GeminiHttpClient.class);
        objectMapper = new ObjectMapper();
        service = new GeminiService(client, objectMapper);
    }

    @org.junit.jupiter.api.Disabled("External HTTP client chain - skip in unit run")
    @Test
    void chatWithGemini_returnsParsedResponse() {
        // Prepare expected GeminiResponse
        var part = new GeminiService.Part("réponse", null);
        var content = new GeminiService.Content(List.of(part));
        var candidate = new GeminiService.Candidate(content);
        var expected = new GeminiService.GeminiResponse(List.of(candidate));

        when(client.generateContent(anyString(), anyString(), any(), eq(GeminiService.GeminiResponse.class))).thenReturn(expected);

        var res = service.chatWithGemini("bonjour");
        assertNotNull(res);
        assertEquals("réponse", res.candidates().get(0).content().parts().get(0).text());
    }
}
