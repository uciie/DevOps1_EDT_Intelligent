package com.example.backend.service.impl;

import com.example.backend.model.Location;
import com.example.backend.model.TravelTime.TransportMode;
import com.example.backend.service.TravelTimeCalculator;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.util.Map;

/**
 * Implémentation avec Google Maps Distance Matrix API.
 * Nécessite une clé API Google Maps (gratuit jusqu'à 25000 requêtes/jour).
 * 
 * VERSION AMÉLIORÉE avec meilleur logging et gestion d'erreurs.
 */
@Component
@Profile("external-api")
public class GoogleMapsTravelTimeCalculator implements TravelTimeCalculator {

    @Value("${google.maps.api.key:}")
    private String apiKey;

    private final RestTemplate restTemplate = new RestTemplate();
    private final SimpleTravelTimeCalculator fallbackCalculator;

    public GoogleMapsTravelTimeCalculator() {
        this.fallbackCalculator = new SimpleTravelTimeCalculator();
    }

    private static final String API_URL = "https://maps.googleapis.com/maps/api/distancematrix/json";

    @Override
    public int calculateTravelTime(Location from, Location to, TransportMode mode) {
        // Si pas de clé API, utilise le calculateur simple
        if (apiKey == null || apiKey.isEmpty()) {
            System.err.println("⚠️  NO GOOGLE MAPS API KEY - Using fallback calculator");
            return fallbackCalculator.calculateTravelTime(from, to, mode);
        }
        
        try {
            // Construction de l'URL avec les paramètres
            String url = UriComponentsBuilder.fromHttpUrl(API_URL)
                .queryParam("origins", formatLocation(from))
                .queryParam("destinations", formatLocation(to))
                .queryParam("mode", mapTransportMode(mode))
                .queryParam("key", apiKey)
                .queryParam("language", "fr")
                .encode() // S'assurer de l'encodage correct
                .toUriString();

            // Appel à l'API
            System.err.println("🌐 Calling Google Maps API");
            System.err.println("   From: " + formatLocation(from));
            System.err.println("   To: " + formatLocation(to));
            System.err.println("   Mode: " + mapTransportMode(mode));
            System.err.println("   URL: " + url.replaceAll("key=[^&]+", "key=***"));
            
            @SuppressWarnings("unchecked")
            Map<String, Object> response = restTemplate.getForObject(url, Map.class);
            
            // Logging détaillé de la réponse
            System.err.println("📥 Google Maps API Response:");
            System.err.println("   Full response: " + response);

            // Vérification du statut global
            if (response == null) {
                System.err.println("❌ Response is NULL - falling back");
                return fallbackCalculator.calculateTravelTime(from, to, mode);
            }

            String status = (String) response.get("status");
            System.err.println("   Global status: " + status);

            if (!"OK".equals(status)) {
                System.err.println("❌ API returned status: " + status);
                if (response.containsKey("error_message")) {
                    System.err.println("   Error message: " + response.get("error_message"));
                }
                
                // Messages d'aide selon le statut
                switch (status) {
                    case "INVALID_REQUEST":
                        System.err.println("   → Check that origins/destinations are valid");
                        break;
                    case "MAX_ELEMENTS_EXCEEDED":
                        System.err.println("   → Too many origin/destination pairs");
                        break;
                    case "OVER_DAILY_LIMIT":
                    case "OVER_QUERY_LIMIT":
                        System.err.println("   → API quota exceeded");
                        break;
                    case "REQUEST_DENIED":
                        System.err.println("   → API key invalid or API not enabled");
                        break;
                    case "UNKNOWN_ERROR":
                        System.err.println("   → Server error, may work on retry");
                        break;
                }
                
                return fallbackCalculator.calculateTravelTime(from, to, mode);
            }

            // Extraction du temps de trajet depuis la réponse (avec validations)
            @SuppressWarnings("unchecked")
            java.util.List<Map<String, Object>> rows = 
                (java.util.List<Map<String, Object>>) response.get("rows");
            
            if (rows == null || rows.isEmpty()) {
                System.err.println("❌ No rows in response - falling back");
                return fallbackCalculator.calculateTravelTime(from, to, mode);
            }

            @SuppressWarnings("unchecked")
            Map<String, Object> firstRow = rows.get(0);
            if (firstRow == null) {
                System.err.println("❌ First row is null - falling back");
                return fallbackCalculator.calculateTravelTime(from, to, mode);
            }

            @SuppressWarnings("unchecked")
            java.util.List<Map<String, Object>> elements = 
                (java.util.List<Map<String, Object>>) firstRow.get("elements");
            
            if (elements == null || elements.isEmpty()) {
                System.err.println("❌ No elements in row - falling back");
                return fallbackCalculator.calculateTravelTime(from, to, mode);
            }

            Map<String, Object> element = elements.get(0);
            if (element == null) {
                System.err.println("❌ First element is null - falling back");
                return fallbackCalculator.calculateTravelTime(from, to, mode);
            }

            String elementStatus = (String) element.get("status");
            System.err.println("   Element status: " + elementStatus);

            if (!"OK".equals(elementStatus)) {
                System.err.println("❌ Element status not OK: " + elementStatus);
                
                // Messages d'aide selon le statut de l'élément
                switch (elementStatus) {
                    case "NOT_FOUND":
                        System.err.println("   → Origin or destination address could not be geocoded");
                        break;
                    case "ZERO_RESULTS":
                        System.err.println("   → No route found between origin and destination");
                        break;
                    case "MAX_ROUTE_LENGTH_EXCEEDED":
                        System.err.println("   → Route is too long");
                        break;
                }
                
                System.err.println("   Element details: " + element);
                return fallbackCalculator.calculateTravelTime(from, to, mode);
            }

            @SuppressWarnings("unchecked")
            Map<String, Object> duration = (Map<String, Object>) element.get("duration");
            
            if (duration == null) {
                System.err.println("❌ Duration is null - falling back");
                System.err.println("   Element: " + element);
                return fallbackCalculator.calculateTravelTime(from, to, mode);
            }

            if (!(duration.get("value") instanceof Number)) {
                System.err.println("❌ Duration value is not a number - falling back");
                System.err.println("   Duration: " + duration);
                return fallbackCalculator.calculateTravelTime(from, to, mode);
            }

            int seconds = ((Number) duration.get("value")).intValue();
            int minutes = (int) Math.ceil(seconds / 60.0);
            
            System.err.println("✅ SUCCESS!");
            System.err.println("   Duration: " + seconds + " seconds (" + minutes + " minutes)");
            System.err.println("   Text: " + duration.get("text"));
            
            // Vérification de cohérence
            if (minutes < 1) {
                System.err.println("⚠️  WARNING: Duration is less than 1 minute, seems suspicious");
            }
            
            return minutes;

        } catch (Exception e) {
            // En cas d'exception, utilise le calculateur simple
            System.err.println("💥 EXCEPTION calling Google Maps API: " + e.getClass().getSimpleName());
            System.err.println("   Message: " + e.getMessage());
            e.printStackTrace();
            return fallbackCalculator.calculateTravelTime(from, to, mode);
        }
    }

    /**
     * Formate une location pour l'API Google Maps.
     *
     * @param location la localisation
     * @return la chaîne formatée (coordonnées ou adresse normalisée)
     */
    private String formatLocation(Location location) {
        if (location.hasCoordinates()) {
            return location.getLatitude() + "," + location.getLongitude();
        }
        String address = location.getAddress();
        if (address == null) {
            return "";
        }
        // Normaliser l'adresse : supprimer retours à la ligne, espaces multiples,
        // et uniformiser les séparateurs par des virgules (Google préfère des components séparés par des virgules).
        address = address.replaceAll("[\\r\\n]+", ", ");
        address = address.replaceAll("\\s{2,}", " ").trim();
        address = address.replaceAll("\\s*,\\s*", ", ");
        return address;
    }

    /**
     * Mappe le mode de transport vers le format Google Maps.
     *
     * @param mode le mode de transport
     * @return la chaîne correspondante pour l'API
     */
    private String mapTransportMode(TransportMode mode) {
        return switch (mode) {
            case WALKING -> "walking";
            case CYCLING -> "bicycling";
            case DRIVING -> "driving";
            case TRANSIT -> "transit";
        };
    }
}