package com.example.backend.service.impl;

import com.example.backend.model.Location;
import com.example.backend.model.TravelTime.TransportMode; 
import com.example.backend.service.TravelTimeCalculator;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.net.URI;

import org.springframework.beans.factory.annotation.Autowired; 
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.context.annotation.Primary;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

@Component
@Profile("external-api")
@Primary
public class GoogleMapsTravelTimeCalculator implements TravelTimeCalculator {

    @Value("${google.maps.api.key:}")
    private String apiKey;

    private final RestTemplate restTemplate;
    private final SimpleTravelTimeCalculator fallbackCalculator;
    private final ObjectMapper mapper;

    private static final String API_URL = "https://maps.googleapis.com/maps/api/distancematrix/json";

    // ✅ AJOUTEZ @Autowired ICI pour dire à Spring : "Utilise ce constructeur !"
    @Autowired
    public GoogleMapsTravelTimeCalculator(RestTemplateBuilder restTemplateBuilder) {
        this.restTemplate = restTemplateBuilder.build();
        this.fallbackCalculator = new SimpleTravelTimeCalculator();
        this.mapper = new ObjectMapper();
    }

    // Constructeur secondaire pour les tests unitaires (Spring l'ignorera grâce à l'annotation au-dessus)
    public GoogleMapsTravelTimeCalculator(RestTemplate restTemplate, SimpleTravelTimeCalculator fallbackCalculator, ObjectMapper mapper) {
        this.restTemplate = restTemplate;
        this.fallbackCalculator = fallbackCalculator;
        this.mapper = mapper;
    }

    @Override
    public int calculateTravelTime(Location from, Location to, TransportMode mode) {

        if (apiKey == null || apiKey.isBlank()) {
            System.err.println("⚠️  No Google Maps API key — fallback calculator used.");
            return fallbackCalculator.calculateTravelTime(from, to, mode);
        }

        try {
            URI uri = UriComponentsBuilder.fromHttpUrl(API_URL)
                    .queryParam("origins", formatLocation(from))
                    .queryParam("destinations", formatLocation(to))
                    .queryParam("mode", mapTransportMode(mode))
                    .queryParam("region", "fr")
                    .queryParam("language", "fr")
                    .queryParam("key", apiKey)
                    .build()
                    .toUri();

            System.err.println("🌐 Calling Google API:");
            System.err.println("   From: " + formatLocation(from));
            System.err.println("   To:   " + formatLocation(to));
            System.err.println("   Mode: " + mapTransportMode(mode));
            System.err.println("   URL:  " + uri);

            String json = restTemplate.getForObject(uri, String.class);

            // --- AJOUT DEBUG ---
            System.err.println("🔍 JSON REÇU DE GOOGLE :");
            System.err.println(json); 
            // -------------------

            JsonNode root = mapper.readTree(json);

            // Vérification statut global
            String status = root.path("status").asText();
            if (!"OK".equals(status)) {
                logGlobalError(root); // Utilisation de la méthode helper définie plus bas
                return fallbackCalculator.calculateTravelTime(from, to, mode);
            }

            JsonNode rows = root.path("rows");
            // Sécurité : on vérifie que rows n'est pas vide
            if (rows.isEmpty() || rows.path(0).path("elements").isEmpty()) {
                System.err.println("❌ Empty rows or elements");
                return fallbackCalculator.calculateTravelTime(from, to, mode);
            }

            JsonNode elementNode = rows.path(0).path("elements").path(0);

            String elementStatus = elementNode.path("status").asText();
            if (!"OK".equals(elementStatus)) {
                logElementError(elementStatus, elementNode); // Utilisation de la méthode helper définie plus bas
                return fallbackCalculator.calculateTravelTime(from, to, mode);
            }

            // Récupération de la durée en SECONDES (champ 'value')
            int durationInSeconds = elementNode.path("duration").path("value").asInt(-1);

            if (durationInSeconds < 0) {
                System.err.println("❌ Invalid duration — fallback");
                return fallbackCalculator.calculateTravelTime(from, to, mode);
            }

            // Conversion en minutes
            int minutes = durationInSeconds / 60;

            System.err.println("✅ Duration: " + minutes + " min (" + durationInSeconds + " sec)");

            return minutes;

        } catch (RestClientException e) {
            System.err.println("💥 HTTP error: " + e.getMessage());
        } catch (Exception e) {
            System.err.println("💥 Unexpected error: " + e.getClass().getSimpleName());
            e.printStackTrace();
        }

        return fallbackCalculator.calculateTravelTime(from, to, mode);
    }

    private void logGlobalError(JsonNode root) {
        String status = root.path("status").asText();
        System.err.println("❌ API error: " + status);

        switch (status) {
            case "INVALID_REQUEST" -> System.err.println("   → Invalid parameters (origins/destinations).");
            case "OVER_DAILY_LIMIT", "OVER_QUERY_LIMIT" -> System.err.println("   → API quota exceeded.");
            case "REQUEST_DENIED" -> System.err.println("   → API key invalid or API disabled.");
            case "UNKNOWN_ERROR" -> System.err.println("   → Temporary server error.");
        }

        if (root.has("error_message")) {
            System.err.println("   Google says: " + root.get("error_message").asText());
        }
    }

    private void logElementError(String status, JsonNode elementNode) {
        System.err.println("❌ Element status: " + status);
        switch (status) {
            case "NOT_FOUND" -> System.err.println("   → Address could not be geocoded.");
            case "ZERO_RESULTS" -> System.err.println("   → No route found.");
            case "MAX_ROUTE_LENGTH_EXCEEDED" -> System.err.println("   → Route too long.");
        }
        System.err.println("   Details: " + elementNode.toString());
    }

    private String formatLocation(Location location) {
        if (location.hasCoordinates()) {
            return location.getLatitude() + "," + location.getLongitude();
        }
        if (location.getAddress() == null) return "";

        String address = location.getAddress()
                .replaceAll("[\\r\\n]+", ", ")
                .replaceAll("\\s{2,}", " ").trim()
                .replaceAll("\\s*,\\s*", ", ")
                .replaceAll("\\bPl\\.\\s", "Place ")
                .replaceAll("\\bAv\\.\\s", "Avenue ");

        return address;
    }

    private String mapTransportMode(TransportMode mode) {
        return switch (mode) {
            case WALKING -> "walking";
            case CYCLING -> "bicycling";
            case DRIVING -> "driving";
            case TRANSIT -> "transit";
        };
    }
}
