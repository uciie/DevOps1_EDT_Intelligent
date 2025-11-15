package com.example.backend.service.impl;

import com.example.backend.model.Location;
import com.example.backend.model.TravelTime.TransportMode;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

/**
 * Tests unitaires pour GoogleMapsTravelTimeCalculator.
 * Mock l'API Google Maps pour éviter les appels réseau réels.
 */
class GoogleMapsTravelTimeCalculatorTest {

    @Mock
    private RestTemplate restTemplate;

    private GoogleMapsTravelTimeCalculator calculator;
    private Location parisLocation;
    private Location lyonLocation;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        calculator = new GoogleMapsTravelTimeCalculator();
        
        // Injection de la RestTemplate mockée
        ReflectionTestUtils.setField(calculator, "restTemplate", restTemplate);
        
        // Injection d'une clé API factice
        ReflectionTestUtils.setField(calculator, "apiKey", "test-api-key");

        // Locations de test
        parisLocation = new Location("Paris, France", 48.8566, 2.3522);
        lyonLocation = new Location("Lyon, France", 45.7640, 4.8357);
    }

    @Test
    @DisplayName("Calcul avec réponse API réussie")
    void testCalculateTravelTime_SuccessfulApiResponse() {
        // Arrange
        Map<String, Object> mockResponse = createMockApiResponse(14400); // 240 minutes (14400 secondes)

        when(restTemplate.getForObject(anyString(), eq(Map.class)))
            .thenReturn(mockResponse);

        // Act
        int travelTime = calculator.calculateTravelTime(
            parisLocation, lyonLocation, TransportMode.DRIVING
        );

        // Assert
        assertEquals(240, travelTime);
        verify(restTemplate, times(1)).getForObject(anyString(), eq(Map.class));
    }

    @Test
    @DisplayName("Calcul avec arrondissement supérieur des secondes")
    void testCalculateTravelTime_RoundingUp() {
        // Arrange - 3661 secondes = 61.016... minutes -> arrondi à 62
        Map<String, Object> mockResponse = createMockApiResponse(3661);

        when(restTemplate.getForObject(anyString(), eq(Map.class)))
            .thenReturn(mockResponse);

        // Act
        int travelTime = calculator.calculateTravelTime(
            parisLocation, lyonLocation, TransportMode.DRIVING
        );

        // Assert
        assertEquals(62, travelTime); // ceil(3661/60) = 62
        verify(restTemplate, times(1)).getForObject(anyString(), eq(Map.class));
    }

    @Test
    @DisplayName("Fallback vers SimpleTravelTimeCalculator si pas de clé API")
    void testCalculateTravelTime_NoApiKey_UsesFallback() {
        // Arrange
        ReflectionTestUtils.setField(calculator, "apiKey", "");

        // Act
        int travelTime = calculator.calculateTravelTime(
            parisLocation, lyonLocation, TransportMode.DRIVING
        );

        // Assert
        assertTrue(travelTime > 0);
        verify(restTemplate, never()).getForObject(anyString(), eq(Map.class));
    }

    @Test
    @DisplayName("Fallback si l'API retourne un statut d'erreur")
    void testCalculateTravelTime_ApiErrorStatus_UsesFallback() {
        // Arrange
        Map<String, Object> mockResponse = new HashMap<>();
        mockResponse.put("status", "INVALID_REQUEST");

        when(restTemplate.getForObject(anyString(), eq(Map.class)))
            .thenReturn(mockResponse);

        // Act
        int travelTime = calculator.calculateTravelTime(
            parisLocation, lyonLocation, TransportMode.DRIVING
        );

        // Assert
        assertTrue(travelTime > 0);
        verify(restTemplate, times(1)).getForObject(anyString(), eq(Map.class));
    }

    @Test
    @DisplayName("Fallback si l'élément retourné a un statut NOT_FOUND")
    void testCalculateTravelTime_ElementNotFound_UsesFallback() {
        // Arrange
        Map<String, Object> mockResponse = createMockApiResponseWithElementStatus("NOT_FOUND");

        when(restTemplate.getForObject(anyString(), eq(Map.class)))
            .thenReturn(mockResponse);

        // Act
        int travelTime = calculator.calculateTravelTime(
            parisLocation, lyonLocation, TransportMode.DRIVING
        );

        // Assert
        assertTrue(travelTime > 0);
        verify(restTemplate, times(1)).getForObject(anyString(), eq(Map.class));
    }

    @Test
    @DisplayName("Fallback si l'API lance une exception")
    void testCalculateTravelTime_ApiException_UsesFallback() {
        // Arrange
        when(restTemplate.getForObject(anyString(), eq(Map.class)))
            .thenThrow(new RestClientException("Connection timeout"));

        // Act
        int travelTime = calculator.calculateTravelTime(
            parisLocation, lyonLocation, TransportMode.DRIVING
        );

        // Assert
        assertTrue(travelTime > 0);
        verify(restTemplate, times(1)).getForObject(anyString(), eq(Map.class));
    }

    @Test
    @DisplayName("Calcul avec différents modes de transport")
    void testCalculateTravelTime_DifferentTransportModes() {
        // Arrange
        Map<String, Object> mockResponse = createMockApiResponse(7200); // 120 minutes

        when(restTemplate.getForObject(anyString(), eq(Map.class)))
            .thenReturn(mockResponse);

        // Act & Assert pour chaque mode
        for (TransportMode mode : TransportMode.values()) {
            int travelTime = calculator.calculateTravelTime(
                parisLocation, lyonLocation, mode
            );
            assertEquals(120, travelTime);
        }

        // Vérifie que l'API a été appelée pour chaque mode
        verify(restTemplate, times(TransportMode.values().length))
            .getForObject(anyString(), eq(Map.class));
    }

    @Test
    @DisplayName("Utilise les coordonnées GPS si disponibles")
    void testCalculateTravelTime_UsesCoordinatesWhenAvailable() {
        // Arrange
        Map<String, Object> mockResponse = createMockApiResponse(3600);

        when(restTemplate.getForObject(anyString(), eq(Map.class)))
            .thenReturn(mockResponse);

        // Act
        calculator.calculateTravelTime(parisLocation, lyonLocation, TransportMode.DRIVING);

        // Assert — Capture l’URL réelle utilisée
        ArgumentCaptor<String> urlCaptor = ArgumentCaptor.forClass(String.class);

        verify(restTemplate).getForObject(urlCaptor.capture(), eq(Map.class));

        String calledUrl = urlCaptor.getValue();
        assertNotNull(calledUrl);

        // Vérifie que l'URL contient les coordonnées GPS attendues
        assertTrue(
            calledUrl.contains("48.8566,2.3522"),
            "L'URL ne contient pas les coordonnées GPS attendues : " + calledUrl
        );
    }

    @Test
    @DisplayName("Utilise l'adresse si pas de coordonnées")
    void testCalculateTravelTime_UsesAddressWhenNoCoordinates() {
        // Arrange
        Location locationNoCoords = new Location("10 Rue de Rivoli, Paris");
        Map<String, Object> mockResponse = createMockApiResponse(3600);

        when(restTemplate.getForObject(anyString(), eq(Map.class)))
            .thenReturn(mockResponse);

        // Act
        calculator.calculateTravelTime(locationNoCoords, lyonLocation, TransportMode.DRIVING);

        // Assert - Vérifie que l'URL contient l'adresse
        ArgumentCaptor<String> urlCaptor = ArgumentCaptor.forClass(String.class);
        verify(restTemplate).getForObject(urlCaptor.capture(), eq(Map.class));
        String calledUrl = urlCaptor.getValue();
        
        // Vérifie que l'URL n'est pas nulle
        assertNotNull(calledUrl);
        
        // Vérifie que l'URL contient l'adresse formatée correctement
        assertTrue(
            calledUrl.contains("10+Rue+de+Rivoli,+Paris") ||
            calledUrl.contains("10%20Rue%20de%20Rivoli,%20Paris"),
            "L'URL ne contient pas l'adresse attendue : " + calledUrl
        );
    }

    @Test
    @DisplayName("Gère correctement une réponse API null")
    void testCalculateTravelTime_NullApiResponse_UsesFallback() {
        // Arrange
        when(restTemplate.getForObject(anyString(), eq(Map.class)))
            .thenReturn(null);

        // Act
        int travelTime = calculator.calculateTravelTime(
            parisLocation, lyonLocation, TransportMode.DRIVING
        );

        // Assert
        assertTrue(travelTime > 0);
        verify(restTemplate, times(1)).getForObject(anyString(), eq(Map.class));
    }

    @Test
    @DisplayName("Temps de trajet minimum pour très courtes distances")
    void testCalculateTravelTime_VeryShortDistance() {
        // Arrange - 30 secondes
        Map<String, Object> mockResponse = createMockApiResponse(30);

        when(restTemplate.getForObject(anyString(), eq(Map.class)))
            .thenReturn(mockResponse);

        // Act
        int travelTime = calculator.calculateTravelTime(
            parisLocation, lyonLocation, TransportMode.WALKING
        );

        // Assert - Doit retourner 1 minute (arrondi supérieur de 30/60)
        assertEquals(1, travelTime);
    }

    // ==================== Méthodes utilitaires ====================

    /**
     * Crée une réponse mockée de l'API Google Maps avec un temps de trajet donné.
     */
    private Map<String, Object> createMockApiResponse(int durationSeconds) {
        Map<String, Object> duration = new HashMap<>();
        duration.put("value", durationSeconds);
        duration.put("text", durationSeconds / 60 + " mins");

        Map<String, Object> element = new HashMap<>();
        element.put("status", "OK");
        element.put("duration", duration);

        Map<String, Object> row = new HashMap<>();
        row.put("elements", List.of(element));

        Map<String, Object> response = new HashMap<>();
        response.put("status", "OK");
        response.put("rows", List.of(row));

        return response;
    }

    /**
     * Crée une réponse mockée avec un statut d'erreur au niveau de l'élément.
     */
    private Map<String, Object> createMockApiResponseWithElementStatus(String elementStatus) {
        Map<String, Object> element = new HashMap<>();
        element.put("status", elementStatus);

        Map<String, Object> row = new HashMap<>();
        row.put("elements", List.of(element));

        Map<String, Object> response = new HashMap<>();
        response.put("status", "OK");
        response.put("rows", List.of(row));

        return response;
    }
}