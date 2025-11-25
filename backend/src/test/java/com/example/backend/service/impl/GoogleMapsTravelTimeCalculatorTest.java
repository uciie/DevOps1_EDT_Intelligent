package com.example.backend.service.impl;

import com.example.backend.model.Location;
import com.example.backend.model.TravelTime.TransportMode;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class GoogleMapsTravelTimeCalculatorTest {

    @InjectMocks
    private GoogleMapsTravelTimeCalculator googleCalculator;

    @Mock
    private RestTemplate restTemplate;

    @Mock
    private SimpleTravelTimeCalculator fallbackCalculator;

    private Location from;
    private Location to;

    @BeforeEach
    void setUp() {
        // Injection manuelle des mocks car ils sont instanciés avec 'new' dans la classe cible
        ReflectionTestUtils.setField(googleCalculator, "restTemplate", restTemplate);
        ReflectionTestUtils.setField(googleCalculator, "fallbackCalculator", fallbackCalculator);
        // On définit une clé API pour ne pas déclencher le fallback immédiat
        ReflectionTestUtils.setField(googleCalculator, "apiKey", "FAKE_API_KEY");

        from = new Location(48.85, 2.35); // Paris
        to = new Location(45.75, 4.85);   // Lyon
    }

    @Test
    void testCalculate_NoApiKey_ShouldUseFallback() {
        // Given: Pas de clé API
        ReflectionTestUtils.setField(googleCalculator, "apiKey", "");

        when(fallbackCalculator.calculateTravelTime(any(), any(), any())).thenReturn(50);

        // When
        int result = googleCalculator.calculateTravelTime(from, to, TransportMode.DRIVING);

        // Then
        assertEquals(50, result);
        verifyNoInteractions(restTemplate); // Google ne doit pas être appelé
    }

    @Test
    void testCalculate_GoogleApiSuccess() {
        // Given: Réponse JSON simulée de Google
        String jsonResponse = """
            {
                "status": "OK",
                "rows": [
                    {
                        "elements": [
                            {
                                "status": "OK",
                                "duration": {
                                    "value": 3600,
                                    "text": "1 hour"
                                }
                            }
                        ]
                    }
                ]
            }
        """;
        when(restTemplate.getForObject(anyString(), eq(String.class))).thenReturn(jsonResponse);

        // When
        int result = googleCalculator.calculateTravelTime(from, to, TransportMode.DRIVING);

        // Then: 3600 secondes = 60 minutes
        assertEquals(60, result);
    }

    @Test
    void testCalculate_GoogleApiGlobalError() {
        // Given: Erreur de quota
        String jsonResponse = "{ \"status\": \"OVER_DAILY_LIMIT\" }";
        when(restTemplate.getForObject(anyString(), eq(String.class))).thenReturn(jsonResponse);
        when(fallbackCalculator.calculateTravelTime(any(), any(), any())).thenReturn(10);

        // When
        int result = googleCalculator.calculateTravelTime(from, to, TransportMode.DRIVING);

        // Then
        assertEquals(10, result); // Fallback utilisé
    }

    @Test
    void testCalculate_NetworkException() {
        // Given: Le réseau échoue
        when(restTemplate.getForObject(anyString(), eq(String.class)))
            .thenThrow(new RestClientException("Connection refused"));
        when(fallbackCalculator.calculateTravelTime(any(), any(), any())).thenReturn(15);

        // When
        int result = googleCalculator.calculateTravelTime(from, to, TransportMode.DRIVING);

        // Then
        assertEquals(15, result);
    }

    @Test
    void testCalculate_RouteNotFound() {
        // Given: Adresse introuvable par Google
        String jsonResponse = """
            {
                "status": "OK",
                "rows": [
                    {
                        "elements": [
                            { "status": "ZERO_RESULTS" }
                        ]
                    }
                ]
            }
        """;
        when(restTemplate.getForObject(anyString(), eq(String.class))).thenReturn(jsonResponse);
        when(fallbackCalculator.calculateTravelTime(any(), any(), any())).thenReturn(20);

        // When
        int result = googleCalculator.calculateTravelTime(from, to, TransportMode.DRIVING);

        // Then
        assertEquals(20, result);
    }
}