package com.example.backend.service.impl;

import com.example.backend.model.Location;
import com.example.backend.model.TravelTime.TransportMode;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.util.ReflectionTestUtils;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests d'intégration pour GoogleMapsTravelTimeCalculator.
 * 
 * VERSION : Uniquement adresses complètes et coordonnées GPS
 * - address = adresse postale complète (pour calcul)
 * - name = nom d'affichage (optionnel)
 * - Coordonnées GPS : latitude, longitude précises
 * 
 * Configuration requise:
 * - Clé API Google Maps dans GOOGLE_MAPS_API_KEY ou application-external-api.properties
 * - Profil Spring "external-api" actif
 */
@SpringBootTest
@ActiveProfiles("external-api")
class GoogleMapsTravelTimeCalculatorIntegrationTest {

    private GoogleMapsTravelTimeCalculator calculator;

    @Value("${google.maps.api.key:}")
    private String apiKey;

    @BeforeEach
    void setUp() {
        calculator = new GoogleMapsTravelTimeCalculator();
        
        String key = System.getenv("GOOGLE_MAPS_API_KEY");

        if ((key == null || key.isEmpty()) && apiKey != null && !apiKey.isEmpty()) {
            key = apiKey;
        }

        if (key == null || key.isEmpty()) {
            String[] candidates = new String[] { ".env", "backend/.env", "../backend/.env", "../.env" };
            for (String cand : candidates) {
                Path p = Paths.get(cand);
                if (Files.exists(p)) {
                    try {
                        List<String> lines = Files.readAllLines(p);
                        for (String line : lines) {
                            String trimmed = line.trim();
                            if (trimmed.startsWith("GOOGLE_MAPS_API_KEY=")) {
                                String val = trimmed.substring("GOOGLE_MAPS_API_KEY=".length()).trim();
                                if ((val.startsWith("\"") && val.endsWith("\"")) || (val.startsWith("'") && val.endsWith("'"))) {
                                    val = val.substring(1, val.length() - 1);
                                }
                                if (!val.isEmpty()) {
                                    key = val;
                                    break;
                                }
                            }
                        }
                    } catch (IOException e) {
                        // ignore
                    }
                }
                if (key != null && !key.isEmpty()) break;
            }
        }

        if (key != null && !key.isEmpty()) {
            ReflectionTestUtils.setField(calculator, "apiKey", key);
        } else {
            fail("Aucune clé API Google Maps configurée.");
        }
    }

    // ========================================================================
    // TESTS AVEC ARCHITECTURE AMÉLIORÉE : GPS + ADRESSE + NOM
    // ========================================================================

    @Test
    @DisplayName("Paris-Lyon : Adresse postale + Nom")
    void testParisLyon_GPS_Complete() {
        // adresse postale + nom d'affichage
        Location paris = new Location(
            "1 Place de la Concorde, 75008 Paris, France" // Adresse postale
        );
        paris.setName("Paris Centre");
        Location lyon = new Location(
            "1 Place Bellecour, 69002 Lyon, France"
        );
        lyon.setName("Lyon Centre");

        int time = calculator.calculateTravelTime(paris, lyon, TransportMode.DRIVING);

        System.out.println("📍 " + paris.getDisplayName() + " → " + lyon.getDisplayName());
        System.out.println("   Temps: " + time + " min (" + (time / 60) + "h" + (time % 60) + "min)");
        
        assertTrue(time >= 240 && time <= 360);
    }

    @Test
    @DisplayName("Paris-Lyon : GPS seul avec noms")
    void testParisLyon_GPS_WithNames() {
        // GPS précis + noms d'affichage
        Location paris = new Location(48.8566, 2.3522);
        paris.setName("Paris");
        Location lyon = new Location(45.7640, 4.8357);
        lyon.setName("Lyon");

        int time = calculator.calculateTravelTime(paris, lyon, TransportMode.DRIVING);

        System.out.println("📍 " + paris.getDisplayName() + " → " + lyon.getDisplayName());
        System.out.println("   GPS: " + paris.getLatitude() + "," + paris.getLongitude() +
                          " → " + lyon.getLatitude() + "," + lyon.getLongitude());
        System.out.println("   Temps: " + time + " min");
        
        assertTrue(time >= 240 && time <= 360);
    }

    
    @Test
    @DisplayName("Louvre → Tour Eiffel : Courte distance")
    void testParis_ShortDistance() {
        Location louvre = new Location(
            "1 Rue de Rivoli, 75001 Paris, France",
            48.8606, 2.3376
        );
        louvre.setName("Musée du Louvre");

        Location eiffel = new Location(
            "5 Avenue Anatole France, 75007 Paris, France",
            48.8584, 2.2945
        );
        eiffel.setName("Tour Eiffel");

        int time = calculator.calculateTravelTime(louvre, eiffel, TransportMode.WALKING);

        System.out.println("📍 " + louvre.getDisplayName() + " → " + eiffel.getDisplayName() + " (à pied)");
        System.out.println("   Temps: " + time + " min");
        
        assertTrue(time >= 30 && time <= 70);
    }

    @Test
    @DisplayName("Bellecour → Fourvière : Lyon intra-muros")
    void testLyon_ShortDistance() {
        Location bellecour = new Location(
            "1 Place Bellecour, 69002 Lyon, France"
        );
        bellecour.setName("Place Bellecour");
        
        Location fourviere = new Location(
            "8 Place de Fourvière, 69005 Lyon, France"
        );
        fourviere.setName("Basilique de Fourvière");

        int time = calculator.calculateTravelTime(bellecour, fourviere, TransportMode.WALKING);

        System.out.println("📍 " + bellecour.getDisplayName() + " → " + fourviere.getDisplayName() + " (à pied)");
        System.out.println("   Temps: " + time + " min");
        
        assertTrue(time >= 15 && time <= 45);
    }

    @Test
    @DisplayName("Paris-Marseille : Longue distance")
    void testLongDistance() {
        Location paris = new Location("Paris, France");
        Location marseille = new Location("Marseille, France");

        int time = calculator.calculateTravelTime(paris, marseille, TransportMode.DRIVING);

        System.out.println("📍 " + paris.getDisplayName() + " → " + marseille.getDisplayName());
        System.out.println("   Temps: " + (time / 60) + "h" + (time % 60) + "min");
        
        assertTrue(time >= 400 && time <= 600);
    }

    @Test
    @DisplayName("Transit : Gare à gare en train")
    void testTransit() {
        Location gareParis = new Location(
            "Place Louis Armand, 75012 Paris, France",
            48.8443, 2.3730
        );
        gareParis.setName("Gare de Lyon Paris");
        
        Location gareLyon = new Location(
            "5 Place Charles Béraudier, 69003 Lyon, France",
            45.7603, 4.8599
        );
        gareLyon.setName("Gare Part-Dieu Lyon");

        int time = calculator.calculateTravelTime(gareParis, gareLyon, TransportMode.TRANSIT);

        System.out.println("📍 " + gareParis.getDisplayName() + " → " + gareLyon.getDisplayName() + " (train)");
        System.out.println("   Temps: " + time + " min");
        
        assertTrue(time >= 90 && time <= 450);
    }

    @Test
    @DisplayName("Vélo : Paris intra-muros")
    void testCycling() {
        Location bastille = new Location(
            "Place de la Bastille, 75011 Paris, France",
            48.8530, 2.3690
        );
        bastille.setName("Bastille");
        
        Location montmartre = new Location(
            "Place du Tertre, 75018 Paris, France",
            48.8867, 2.3431
        );
        montmartre.setName("Montmartre");

        int time = calculator.calculateTravelTime(bastille, montmartre, TransportMode.CYCLING);

        System.out.println("📍 " + bastille.getDisplayName() + " → " + montmartre.getDisplayName() + " (vélo)");
        System.out.println("   Temps: " + time + " min");
        
        assertTrue(time >= 10 && time <= 50);
    }

    @Test
    @DisplayName("Comparaison multi-modes")
    void testMultiMode() {
        Location arcTriomphe = new Location(
            "Place Charles de Gaulle, 75008 Paris, France"
        );
        arcTriomphe.setName("Arc de Triomphe");
        
        Location notreDame = new Location(
            "6 Parvis Notre-Dame, 75004 Paris, France"
        );
        notreDame.setName("Notre-Dame");

        int walking = calculator.calculateTravelTime(arcTriomphe, notreDame, TransportMode.WALKING);
        int cycling = calculator.calculateTravelTime(arcTriomphe, notreDame, TransportMode.CYCLING);
        int driving = calculator.calculateTravelTime(arcTriomphe, notreDame, TransportMode.DRIVING);
        int transit = calculator.calculateTravelTime(arcTriomphe, notreDame, TransportMode.TRANSIT);

        System.out.println("\n=== 📊 Comparaison: " + 
                          arcTriomphe.getDisplayName() + " → " + notreDame.getDisplayName() + " ===");
        System.out.println("🚶 À pied:      " + walking + " min");
        System.out.println("🚴 Vélo:        " + cycling + " min");
        System.out.println("🚗 Voiture:     " + driving + " min");
        System.out.println("🚇 Transports:  " + transit + " min");

        assertTrue(walking >= cycling);
        assertTrue(walking > 0 && cycling > 0 && driving > 0 && transit > 0);
    }

    @Test
    @DisplayName("Route impossible - Paris-Corse")
    void testImpossibleRoute() {
        Location paris = new Location("Paris, France");
        Location ajaccio = new Location("Ajaccio, Corse, France");

        int time = calculator.calculateTravelTime(paris, ajaccio, TransportMode.DRIVING);

        System.out.println("📍 Paris → Ajaccio (route impossible sans ferry)");
        System.out.println("   Temps: " + time + " min (fallback)");
        
        assertTrue(time > 0);
    }

    // ========================================================================
    // TEST DE VALIDATION DE LA NOUVELLE ARCHITECTURE
    // ========================================================================

    @Test
    @DisplayName("Validation : Adresse incomplète rejetée")
    void testValidation_ShortAddress() {
        assertThrows(IllegalArgumentException.class, () -> {
            new Location("Paris");  // Trop court
        });
    }

    @Test
    @DisplayName("Validation : GPS invalide rejeté")
    void testValidation_InvalidGPS() {
        assertThrows(IllegalArgumentException.class, () -> {
            new Location(100.0, 2.0);  // Latitude > 90 , "Paris, France"
        });
    }

    @Test
    @DisplayName("Affichage : getDisplayName() retourne le nom si défini")
    void testDisplay_WithName() {
        Location loc = new Location(
            "5 Avenue Anatole France, 75007 Paris, France"
        );
        loc.setName("Tour Eiffel");
        
        assertEquals("Tour Eiffel", loc.getDisplayName());
    }

    @Test
    @DisplayName("Affichage : getDisplayName() retourne adresse courte si pas de nom")
    void testDisplay_WithoutName() {
        Location loc = new Location("Place Bellecour, 69002 Lyon, France");
        
        assertEquals("Place Bellecour", loc.getDisplayName());
    }
}
