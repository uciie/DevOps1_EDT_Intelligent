package com.example.backend.service.impl;

import com.example.backend.model.Location;
import com.example.backend.model.TravelTime.TransportMode;
import com.example.backend.service.TravelTimeCalculator;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

@Component
@Profile("external-api")
public class GoogleMapsTravelTimeCalculator implements TravelTimeCalculator {

    @Value("${google.maps.api.key:}")
    private String apiKey;

    private final RestTemplate restTemplate = new RestTemplate();
    private final SimpleTravelTimeCalculator fallbackCalculator = new SimpleTravelTimeCalculator();
    private final ObjectMapper mapper = new ObjectMapper();

    private static final String API_URL = "https://maps.googleapis.com/maps/api/distancematrix/json";

    @Override
    public int calculateTravelTime(Location from, Location to, TransportMode mode) {

        if (apiKey == null || apiKey.isBlank()) {
            System.err.println("⚠️  No Google Maps API key — fallback calculator used.");
            return fallbackCalculator.calculateTravelTime(from, to, mode);
        }

        try {
            String url = UriComponentsBuilder.fromHttpUrl(API_URL)
                    .queryParam("origins", formatLocation(from))
                    .queryParam("destinations", formatLocation(to))
                    .queryParam("mode", mapTransportMode(mode))
                    .queryParam("region", "fr")
                    .queryParam("language", "fr")
                    .queryParam("key", apiKey)
                    .encode()
                    .toUriString();

            System.err.println("🌐 Calling Google API:");
            System.err.println("   From: " + formatLocation(from));
            System.err.println("   To:   " + formatLocation(to));
            System.err.println("   Mode: " + mapTransportMode(mode));

            String json = restTemplate.getForObject(url, String.class);
            JsonNode root = mapper.readTree(json);

            // Vérification statut global
            String status = root.path("status").asText();
            if (!"OK".equals(status)) {
                logGlobalError(root);
                return fallbackCalculator.calculateTravelTime(from, to, mode);
            }

            JsonNode elementNode = root
                    .path("rows").path(0)
                    .path("elements").path(0);

            String elementStatus = elementNode.path("status").asText();
            if (!"OK".equals(elementStatus)) {
                logElementError(elementStatus, elementNode);
                return fallbackCalculator.calculateTravelTime(from, to, mode);
            }

            // Durée
            int seconds = elementNode.path("duration").path("value").asInt(-1);
            if (seconds <= 0) {
                System.err.println("❌ Invalid duration — fallback");
                return fallbackCalculator.calculateTravelTime(from, to, mode);
            }

            int minutes = (int) Math.ceil(seconds / 60.0);
            System.err.println("✅ Duration: " + seconds + "s ≈ " + minutes + " min");

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
