package com.example.backend.http;

public interface GeminiHttpClient {
    <T> T generateContent(String model, String apiKey, Object body, Class<T> responseType);
}
