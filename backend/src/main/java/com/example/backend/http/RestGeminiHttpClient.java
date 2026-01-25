package com.example.backend.http;

import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

@Service
public class RestGeminiHttpClient implements GeminiHttpClient {

    private final RestClient restClient;

    public RestGeminiHttpClient(RestClient.Builder restClientBuilder) {
        this.restClient = restClientBuilder.baseUrl("https://generativelanguage.googleapis.com/v1beta/models").build();
    }

    @Override
    public <T> T generateContent(String model, String apiKey, Object body, Class<T> responseType) {
        return restClient.post()
                .uri(uriBuilder -> uriBuilder.path("/" + model + ":generateContent").queryParam("key", apiKey).build())
                .contentType(MediaType.APPLICATION_JSON)
                .body(body)
                .retrieve()
                .body(responseType);
    }
}
