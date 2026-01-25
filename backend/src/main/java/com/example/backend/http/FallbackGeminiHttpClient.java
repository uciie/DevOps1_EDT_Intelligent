package com.example.backend.http;

import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.stereotype.Service;

@Service
@ConditionalOnMissingBean(GeminiHttpClient.class)
public class FallbackGeminiHttpClient implements GeminiHttpClient {

    @Override
    public <T> T generateContent(String model, String apiKey, Object body, Class<T> responseType) {
        // Fallback for test contexts: return null so the application context can start.
        return null;
    }
}
