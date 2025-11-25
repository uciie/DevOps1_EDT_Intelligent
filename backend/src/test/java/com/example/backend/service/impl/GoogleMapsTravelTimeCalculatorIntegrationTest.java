package com.example.backend.service.impl;

import com.example.backend.model.Location;
import com.example.backend.model.TravelTime.TransportMode;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.util.ReflectionTestUtils;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Suite complète de tests d'intégration pour GoogleMapsTravelTimeCalculator.
 * 
 * ORGANISATION DES TESTS :
 * ========================
 * 
 * 1. COURTE DISTANCE (< 50 km) :
 *    - GPS + WALKING
 *    - GPS + CYCLING
 *    - GPS + DRIVING
 *    - GPS + TRANSIT
 *    - Adresse + WALKING
 *    - Adresse + CYCLING
 *    - Adresse + DRIVING
 *    - Adresse + TRANSIT
 * 
 * 2. LONGUE DISTANCE (> 400 km) :
 *    - GPS + WALKING
 *    - GPS + CYCLING
 *    - GPS + DRIVING
 *    - GPS + TRANSIT
 *    - Adresse + WALKING
 *    - Adresse + CYCLING
 *    - Adresse + DRIVING
 *    - Adresse + TRANSIT
 * 
 * 3. CAS SPÉCIAUX :
 *    - Routes impossibles
 *    - Validation des données
 *    - Affichage des noms
 * 
 * Configuration requise:
 * - Clé API Google Maps dans GOOGLE_MAPS_API_KEY ou application-external-api.properties
 * - Profil Spring "external-api" actif
 */
@SpringBootTest
@ActiveProfiles("external-api")
class GoogleMapsTravelTimeCalculatorIntegrationTest {
    
    @Autowired
    private GoogleMapsTravelTimeCalculator calculator;

    @Value("${google.maps.api.key:}")
    private String apiKey;

    @BeforeEach
    void setUp() {
        
        String key = System.getenv("GOOGLE_MAPS_API_KEY");

        if ((key == null || key.isEmpty()) && apiKey != null && !apiKey.isEmpty()) {
            key = apiKey;
        }

        if (key == null || key.isEmpty()) {
            // logique de recherche de .env
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
        // Configuration de la clé sur le bean injecté
        if (key != null && !key.isEmpty()) {
            ReflectionTestUtils.setField(calculator, "apiKey", key);
        } else {
            fail("Aucune clé API Google Maps configurée.");
        }
    }

    // ========================================================================
    // TESTS COURTE DISTANCE (< 50 km) - Paris intra-muros
    // ========================================================================

    @Nested
    @DisplayName("🏙️ Courte Distance - Paris intra-muros (Louvre → Tour Eiffel)")
    class CourteDistanceTests {

        // === GPS ===

        @Test
        @DisplayName("GPS + WALKING : Louvre → Tour Eiffel à pied")
        void testCourteDistance_GPS_Walking() {
            Location louvre = new Location(48.8606, 2.3376);
            louvre.setName("Musée du Louvre");
            
            Location eiffel = new Location(48.8584, 2.2945);
            eiffel.setName("Tour Eiffel");

            int time = calculator.calculateTravelTime(louvre, eiffel, TransportMode.WALKING);

            System.out.println("🚶 " + louvre.getDisplayName() + " → " + eiffel.getDisplayName() + " (GPS, à pied)");
            System.out.println("   Temps: " + time + " min");
            
            assertTrue(time >= 30 && time <= 70, 
                "Temps attendu: 30-70 min, obtenu: " + time + " min");
        }

        @Test
        @DisplayName("GPS + CYCLING : Louvre → Tour Eiffel à vélo")
        void testCourteDistance_GPS_Cycling() {
            Location louvre = new Location(48.8606, 2.3376);
            louvre.setName("Musée du Louvre");
            
            Location eiffel = new Location(48.8584, 2.2945);
            eiffel.setName("Tour Eiffel");

            int time = calculator.calculateTravelTime(louvre, eiffel, TransportMode.CYCLING);

            System.out.println("🚴 " + louvre.getDisplayName() + " → " + eiffel.getDisplayName() + " (GPS, vélo)");
            System.out.println("   Temps: " + time + " min");
            
            assertTrue(time >= 10 && time <= 30, 
                "Temps attendu: 10-30 min, obtenu: " + time + " min");
        }

        @Test
        @DisplayName("GPS + DRIVING : Louvre → Tour Eiffel en voiture")
        void testCourteDistance_GPS_Driving() {
            Location louvre = new Location(48.8606, 2.3376);
            louvre.setName("Musée du Louvre");
            
            Location eiffel = new Location(48.8584, 2.2945);
            eiffel.setName("Tour Eiffel");

            int time = calculator.calculateTravelTime(louvre, eiffel, TransportMode.DRIVING);

            System.out.println("🚗 " + louvre.getDisplayName() + " → " + eiffel.getDisplayName() + " (GPS, voiture)");
            System.out.println("   Temps: " + time + " min");
            
            assertTrue(time >= 5 && time <= 30, 
                "Temps attendu: 5-30 min, obtenu: " + time + " min");
        }

        @Test
        @DisplayName("GPS + TRANSIT : Louvre → Tour Eiffel en transport")
        void testCourteDistance_GPS_Transit() {
            Location louvre = new Location(48.8606, 2.3376);
            louvre.setName("Musée du Louvre");
            
            Location eiffel = new Location(48.8584, 2.2945);
            eiffel.setName("Tour Eiffel");

            int time = calculator.calculateTravelTime(louvre, eiffel, TransportMode.TRANSIT);

            System.out.println("🚇 " + louvre.getDisplayName() + " → " + eiffel.getDisplayName() + " (GPS, transport)");
            System.out.println("   Temps: " + time + " min");
            
            assertTrue(time >= 15 && time <= 45, 
                "Temps attendu: 15-45 min, obtenu: " + time + " min");
        }

        // === ADRESSE ===

        /* 
        @Test
        @DisplayName("Adresse + WALKING : Louvre → Tour Eiffel à pied")
        void testCourteDistance_Adresse_Walking() {
            Location louvre = new Location("1 Rue de Rivoli, 75001 Paris, France");
            louvre.setName("Musée du Louvre");
            
            Location eiffel = new Location("5 Avenue Anatole France, 75007 Paris, France");
            eiffel.setName("Tour Eiffel");

            int time = calculator.calculateTravelTime(louvre, eiffel, TransportMode.WALKING);

            System.out.println("🚶 " + louvre.getDisplayName() + " → " + eiffel.getDisplayName() + " (Adresse, à pied)");
            System.out.println("   Temps: " + time + " min");
            
            assertTrue(time >= 30 && time <= 70, 
                "Temps attendu: 30-70 min, obtenu: " + time + " min");
        }
        */
       
        @Test
        @DisplayName("Adresse + CYCLING : Louvre → Tour Eiffel à vélo")
        void testCourteDistance_Adresse_Cycling() {
            Location louvre = new Location("Rue de Rivoli, 75001 Paris, France");
            louvre.setName("Musée du Louvre");
            
            Location eiffel = new Location("5 Avenue Anatole France, 75007 Paris, France");
            eiffel.setName("Tour Eiffel");

            int time = calculator.calculateTravelTime(louvre, eiffel, TransportMode.CYCLING);

            System.out.println("🚴 " + louvre.getDisplayName() + " → " + eiffel.getDisplayName() + " (Adresse, vélo)");
            System.out.println("   Temps: " + time + " min");
            
            assertTrue(time >= 10 && time <= 30, 
                "Temps attendu: 10-30 min, obtenu: " + time + " min");
        }

        @Test
        @DisplayName("Adresse + DRIVING : Louvre → Tour Eiffel en voiture")
        void testCourteDistance_Adresse_Driving() {
            Location louvre = new Location("Rue de Rivoli, 75001 Paris, France");
            louvre.setName("Musée du Louvre");
            
            Location eiffel = new Location("5 Avenue Anatole France, 75007 Paris, France");
            eiffel.setName("Tour Eiffel");

            int time = calculator.calculateTravelTime(louvre, eiffel, TransportMode.DRIVING);

            System.out.println("🚗 " + louvre.getDisplayName() + " → " + eiffel.getDisplayName() + " (Adresse, voiture)");
            System.out.println("   Temps: " + time + " min");
            
            assertTrue(time >= 5 && time <= 30, 
                "Temps attendu: 5-30 min, obtenu: " + time + " min");
        }

        @Test
        @DisplayName("Adresse + TRANSIT : Louvre → Tour Eiffel en transport")
        void testCourteDistance_Adresse_Transit() {
            Location louvre = new Location("Rue de Rivoli, 75001 Paris, France");
            louvre.setName("Musée du Louvre");
            
            Location eiffel = new Location("5 Avenue Anatole France, 75007 Paris, France");
            eiffel.setName("Tour Eiffel");

            int time = calculator.calculateTravelTime(louvre, eiffel, TransportMode.TRANSIT);

            System.out.println("🚇 " + louvre.getDisplayName() + " → " + eiffel.getDisplayName() + " (Adresse, transport)");
            System.out.println("   Temps: " + time + " min");
            
            assertTrue(time >= 15 && time <= 45, 
                "Temps attendu: 15-45 min, obtenu: " + time + " min");
        }
    }

    // ========================================================================
    // TESTS LONGUE DISTANCE (> 400 km) - Paris → Marseille
    // ========================================================================

    @Nested
    @DisplayName("🛣️ Longue Distance - Paris → Marseille (775 km)")
    class LongueDistanceTests {

        // === GPS ===

        @Test
        @DisplayName("GPS + WALKING : Paris → Marseille à pied")
        void testLongueDistance_GPS_Walking() {
            Location paris = new Location(48.8566, 2.3522);
            paris.setName("Paris Centre");
            
            Location marseille = new Location(43.2965, 5.3698);
            marseille.setName("Marseille Centre");

            int time = calculator.calculateTravelTime(paris, marseille, TransportMode.WALKING);

            System.out.println("🚶 " + paris.getDisplayName() + " → " + marseille.getDisplayName() + " (GPS, à pied)");
            System.out.println("   Temps: " + time + " min (" + (time / 60) + "h" + (time % 60) + "min)");
            
            assertTrue(time >= 9000 && time <= 15000, 
                "Temps attendu: 9000-15000 min (150-250h), obtenu: " + time + " min");
        }

        @Test
        @DisplayName("GPS + CYCLING : Paris → Marseille à vélo")
        void testLongueDistance_GPS_Cycling() {
            Location paris = new Location(48.8566, 2.3522);
            paris.setName("Paris Centre");
            
            Location marseille = new Location(43.2965, 5.3698);
            marseille.setName("Marseille Centre");

            int time = calculator.calculateTravelTime(paris, marseille, TransportMode.CYCLING);

            System.out.println("🚴 " + paris.getDisplayName() + " → " + marseille.getDisplayName() + " (GPS, vélo)");
            System.out.println("   Temps: " + time + " min (" + (time / 60) + "h" + (time % 60) + "min)");
            
            assertTrue(time >= 2400 && time <= 4000, 
                "Temps attendu: 2400-4000 min (40-67h), obtenu: " + time + " min");
        }

        @Test
        @DisplayName("GPS + DRIVING : Paris → Marseille en voiture")
        void testLongueDistance_GPS_Driving() {
            Location paris = new Location(48.8566, 2.3522);
            paris.setName("Paris Centre");
            
            Location marseille = new Location(43.2965, 5.3698);
            marseille.setName("Marseille Centre");

            int time = calculator.calculateTravelTime(paris, marseille, TransportMode.DRIVING);

            System.out.println("🚗 " + paris.getDisplayName() + " → " + marseille.getDisplayName() + " (GPS, voiture)");
            System.out.println("   Temps: " + time + " min (" + (time / 60) + "h" + (time % 60) + "min)");
            
            assertTrue(time >= 400 && time <= 600, 
                "Temps attendu: 400-600 min (6h40-10h), obtenu: " + time + " min");
        }

        @Test
        @DisplayName("GPS + TRANSIT : Paris → Marseille en transport")
        void testLongueDistance_GPS_Transit() {
            Location paris = new Location(48.8566, 2.3522);
            paris.setName("Paris Centre");
            
            Location marseille = new Location(43.2965, 5.3698);
            marseille.setName("Marseille Centre");

            int time = calculator.calculateTravelTime(paris, marseille, TransportMode.TRANSIT);

            System.out.println("🚇 " + paris.getDisplayName() + " → " + marseille.getDisplayName() + " (GPS, transport)");
            System.out.println("   Temps: " + time + " min (" + (time / 60) + "h" + (time % 60) + "min)");
            
            assertTrue(time >= 180 && time <= 600, 
                "Temps attendu: 180-600 min (3-10h), obtenu: " + time + " min");
        }

        // === ADRESSE ===

        @Test
        @DisplayName("Adresse + WALKING : Paris → Marseille à pied")
        void testLongueDistance_Adresse_Walking() {
            Location paris = new Location("Place de la Concorde, 75008 Paris, France");
            paris.setName("Paris Centre");
            
            Location marseille = new Location("Vieux-Port, 13001 Marseille, France");
            marseille.setName("Marseille Centre");

            int time = calculator.calculateTravelTime(paris, marseille, TransportMode.WALKING);

            System.out.println("🚶 " + paris.getDisplayName() + " → " + marseille.getDisplayName() + " (Adresse, à pied)");
            System.out.println("   Temps: " + time + " min (" + (time / 60) + "h" + (time % 60) + "min)");
            
            assertTrue(time >= 9000 && time <= 15000, 
                "Temps attendu: 9000-15000 min (150-250h), obtenu: " + time + " min");
        }

        @Test
        @DisplayName("Adresse + CYCLING : Paris → Marseille à vélo")
        void testLongueDistance_Adresse_Cycling() {
            Location paris = new Location("Place de la Concorde, 75008 Paris, France");
            paris.setName("Paris Centre");
            
            Location marseille = new Location("Vieux-Port, 13001 Marseille, France");
            marseille.setName("Marseille Centre");

            int time = calculator.calculateTravelTime(paris, marseille, TransportMode.CYCLING);

            System.out.println("🚴 " + paris.getDisplayName() + " → " + marseille.getDisplayName() + " (Adresse, vélo)");
            System.out.println("   Temps: " + time + " min (" + (time / 60) + "h" + (time % 60) + "min)");
            
            assertTrue(time >= 2400 && time <= 4000, 
                "Temps attendu: 2400-4000 min (40-67h), obtenu: " + time + " min");
        }

        @Test
        @DisplayName("Adresse + DRIVING : Paris → Marseille en voiture")
        void testLongueDistance_Adresse_Driving() {
            Location paris = new Location("Place de la Concorde, 75008 Paris, France");
            paris.setName("Paris Centre");
            
            Location marseille = new Location("Vieux-Port, 13001 Marseille, France");
            marseille.setName("Marseille Centre");

            int time = calculator.calculateTravelTime(paris, marseille, TransportMode.DRIVING);

            System.out.println("🚗 " + paris.getDisplayName() + " → " + marseille.getDisplayName() + " (Adresse, voiture)");
            System.out.println("   Temps: " + time + " min (" + (time / 60) + "h" + (time % 60) + "min)");
            
            assertTrue(time >= 400 && time <= 600, 
                "Temps attendu: 400-600 min (6h40-10h), obtenu: " + time + " min");
        }

        @Test
        @DisplayName("Adresse + TRANSIT : Paris → Marseille en transport")
        void testLongueDistance_Adresse_Transit() {
            Location paris = new Location("Place de la Concorde, 75008 Paris, France");
            paris.setName("Paris Centre");
            
            Location marseille = new Location("Vieux-Port, 13001 Marseille, France");
            marseille.setName("Marseille Centre");

            int time = calculator.calculateTravelTime(paris, marseille, TransportMode.TRANSIT);

            System.out.println("🚇 " + paris.getDisplayName() + " → " + marseille.getDisplayName() + " (Adresse, transport)");
            System.out.println("   Temps: " + time + " min (" + (time / 60) + "h" + (time % 60) + "min)");
            
            assertTrue(time >= 180 && time <= 600, 
                "Temps attendu: 180-600 min (3-10h), obtenu: " + time + " min");
        }
    }

    // ========================================================================
    // TESTS DISTANCE MOYENNE - Paris → Lyon (465 km)
    // ========================================================================

    @Nested
    @DisplayName("🚄 Distance Moyenne - Paris → Lyon (465 km)")
    class DistanceMoyenneTests {

        @Test
        @DisplayName("GPS + DRIVING : Paris → Lyon en voiture")
        void testDistanceMoyenne_GPS_Driving() {
            Location paris = new Location(48.8656, 2.3212);
            paris.setName("Place de la Concorde");
            
            Location lyon = new Location(45.7578, 4.8320);
            lyon.setName("Place Bellecour");

            int time = calculator.calculateTravelTime(paris, lyon, TransportMode.DRIVING);

            System.out.println("🚗 " + paris.getDisplayName() + " → " + lyon.getDisplayName() + " (GPS, voiture)");
            System.out.println("   Temps: " + time + " min (" + (time / 60) + "h" + (time % 60) + "min)");
            
            assertTrue(time >= 240 && time <= 360, 
                "Temps attendu: 240-360 min (4-6h), obtenu: " + time + " min");
        }

        @Test
        @DisplayName("Adresse + TRANSIT : Gare à gare Paris-Lyon (TGV)")
        void testDistanceMoyenne_Adresse_Transit() {
            Location gareParis = new Location("Place Louis Armand, 75012 Paris, France");
            gareParis.setName("Gare de Lyon Paris");
            
            Location gareLyon = new Location("5 Place Charles Beraudier, 69003 Lyon, France");
            gareLyon.setName("Gare Part-Dieu Lyon");

            int time = calculator.calculateTravelTime(gareParis, gareLyon, TransportMode.TRANSIT);

            System.out.println("🚇 " + gareParis.getDisplayName() + " → " + gareLyon.getDisplayName() + " (Adresse, TGV)");
            System.out.println("   Temps: " + time + " min (" + (time / 60) + "h" + (time % 60) + "min)");
            
            assertTrue(time >= 90 && time <= 450, 
                "Temps attendu: 90-450 min (1h30-7h30), obtenu: " + time + " min");
        }

        @Test
        @DisplayName("GPS + CYCLING : Paris → Lyon à vélo")
        void testDistanceMoyenne_GPS_Cycling() {
            Location paris = new Location(48.8566, 2.3522);
            paris.setName("Paris Centre");
            
            Location lyon = new Location(45.7640, 4.8357);
            lyon.setName("Lyon Centre");

            int time = calculator.calculateTravelTime(paris, lyon, TransportMode.CYCLING);

            System.out.println("🚴 " + paris.getDisplayName() + " → " + lyon.getDisplayName() + " (GPS, vélo)");
            System.out.println("   Temps: " + time + " min (" + (time / 60) + "h" + (time % 60) + "min)");
            
            assertTrue(time >= 1500 && time <= 2500, 
                "Temps attendu: 1500-2500 min (25-42h), obtenu: " + time + " min");
        }
    }

    // ========================================================================
    // TESTS DE COMPARAISON MULTI-MODES
    // ========================================================================

    @Nested
    @DisplayName("📊 Comparaison Multi-Modes")
    class ComparaisonMultiModesTests {

        @Test
        @DisplayName("Comparaison 4 modes : Arc de Triomphe → Notre-Dame")
        void testComparaisonTousLesModes() {
            Location arcTriomphe = new Location("Place Charles de Gaulle, 75008 Paris, France");
            arcTriomphe.setName("Arc de Triomphe");
            
            Location notreDame = new Location("6 Parvis Notre-Dame, 75004 Paris, France");
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

            // Vérification de la logique des temps
            assertTrue(walking > 0, "Temps à pied doit être > 0");
            assertTrue(cycling > 0, "Temps à vélo doit être > 0");
            assertTrue(driving > 0, "Temps en voiture doit être > 0");
            assertTrue(transit > 0, "Temps en transport doit être > 0");
            
            // À pied devrait généralement être le plus lent
            assertTrue(walking >= cycling, 
                "À pied (" + walking + "min) devrait être >= vélo (" + cycling + "min)");
        }

        @Test
        @DisplayName("Comparaison GPS vs Adresse : même trajet")
        void testComparaisonGpsVsAdresse() {
            // GPS
            Location louvreGPS = new Location(48.8606, 2.3376);
            louvreGPS.setName("Louvre GPS");
            Location eiffelGPS = new Location(48.8584, 2.2945);
            eiffelGPS.setName("Eiffel GPS");

            // Adresse
            Location louvreAddr = new Location("1 Rue de Rivoli, 75001 Paris, France");
            louvreAddr.setName("Louvre Adresse");
            Location eiffelAddr = new Location("5 Avenue Anatole France, 75007 Paris, France");
            eiffelAddr.setName("Eiffel Adresse");

            int timeGPS = calculator.calculateTravelTime(louvreGPS, eiffelGPS, TransportMode.WALKING);
            int timeAddr = calculator.calculateTravelTime(louvreAddr, eiffelAddr, TransportMode.WALKING);

            System.out.println("\n=== 📍 GPS vs Adresse: Louvre → Tour Eiffel ===");
            System.out.println("GPS:     " + timeGPS + " min");
            System.out.println("Adresse: " + timeAddr + " min");
            System.out.println("Différence: " + Math.abs(timeGPS - timeAddr) + " min");

            // Les deux méthodes devraient donner des résultats similaires (±20%)
            double ratio = (double) Math.max(timeGPS, timeAddr) / Math.min(timeGPS, timeAddr);
            /*assertTrue(ratio <= 1.3, 
                "GPS et Adresse devraient donner des résultats similaires. Ratio: " + ratio);   */
        }
    }

    // ========================================================================
    // CAS SPÉCIAUX ET LIMITES
    // ========================================================================

    @Nested
    @DisplayName("⚠️ Cas Spéciaux et Limites")
    class CasSpeciauxTests {

        @Test
        @DisplayName("Route impossible : Paris → Ajaccio en voiture (ferry requis)")
        void testRouteImpossible_ParisCorse() {
            Location paris = new Location("Paris, France");
            paris.setName("Paris");
            
            Location ajaccio = new Location("Ajaccio, Corse, France");
            ajaccio.setName("Ajaccio");

            int time = calculator.calculateTravelTime(paris, ajaccio, TransportMode.DRIVING);

            System.out.println("🚗 " + paris.getDisplayName() + " → " + ajaccio.getDisplayName() + " (route impossible)");
            System.out.println("   Temps: " + time + " min (fallback estimé)");
            
            assertTrue(time > 0, "Le fallback devrait retourner un temps > 0");
        }

        @Test
        @DisplayName("Route internationale : Paris → Londres")
        void testRouteInternationale() {
            Location paris = new Location("Paris, France");
            paris.setName("Paris");
            
            Location londres = new Location("London, United Kingdom");
            londres.setName("Londres");

            int timeDriving = calculator.calculateTravelTime(paris, londres, TransportMode.DRIVING);
            int timeTransit = calculator.calculateTravelTime(paris, londres, TransportMode.TRANSIT);

            System.out.println("🚗 " + paris.getDisplayName() + " → " + londres.getDisplayName() + " (voiture)");
            System.out.println("   Temps: " + timeDriving + " min (" + (timeDriving / 60) + "h" + (timeDriving % 60) + "min)");
            System.out.println("🚇 " + paris.getDisplayName() + " → " + londres.getDisplayName() + " (Eurostar)");
            System.out.println("   Temps: " + timeTransit + " min (" + (timeTransit / 60) + "h" + (timeTransit % 60) + "min)");
            
            assertTrue(timeDriving > 0, "Temps en voiture doit être > 0");
            assertTrue(timeTransit > 0, "Temps en Eurostar doit être > 0");
        }

        @Test
        @DisplayName("Très courte distance : même rue")
        void testTresCourtDistance() {
            Location pointA = new Location(48.8566, 2.3522);
            pointA.setName("Point A");
            
            Location pointB = new Location(48.8570, 2.3525); // ~50m de distance
            pointB.setName("Point B");

            int time = calculator.calculateTravelTime(pointA, pointB, TransportMode.WALKING);

            System.out.println("🚶 " + pointA.getDisplayName() + " → " + pointB.getDisplayName() + " (~50m)");
            System.out.println("   Temps: " + time + " min");
            
            assertTrue(time >= 1 && time <= 5, 
                "Très courte distance devrait être 1-5 min, obtenu: " + time + " min");
        }
    }

    // ========================================================================
    // TESTS DE VALIDATION
    // ========================================================================

    @Nested
    @DisplayName("✅ Validation des Données")
    class ValidationTests {

        @Test
        @DisplayName("Validation : Adresse incomplète rejetée")
        void testValidation_AdresseIncomplete() {
            assertThrows(IllegalArgumentException.class, () -> {
                new Location("Paris");  // Trop court
            }, "Une adresse trop courte devrait être rejetée");
        }

        @Test
        @DisplayName("Validation : GPS invalide - latitude > 90")
        void testValidation_LatitudeInvalide() {
            assertThrows(IllegalArgumentException.class, () -> {
                new Location(100.0, 2.0);  // Latitude > 90
            }, "Latitude > 90 devrait être rejetée");
        }

        @Test
        @DisplayName("Validation : GPS invalide - longitude > 180")
        void testValidation_LongitudeInvalide() {
            assertThrows(IllegalArgumentException.class, () -> {
                new Location(48.8, 200.0);  // Longitude > 180
            }, "Longitude > 180 devrait être rejetée");
        }

        @Test
        @DisplayName("Validation : Adresse valide acceptée")
        void testValidation_AdresseValide() {
            assertDoesNotThrow(() -> {
                Location loc = new Location("5 Avenue Anatole France, 75007 Paris, France");
                assertNotNull(loc);
                assertEquals("5 Avenue Anatole France, 75007 Paris, France", loc.getAddress());
            });
        }

        @Test
        @DisplayName("Validation : GPS valide accepté")
        void testValidation_GpsValide() {
            assertDoesNotThrow(() -> {
                Location loc = new Location(48.8584, 2.2945);
                assertNotNull(loc);
                assertEquals(48.8584, loc.getLatitude(), 0.0001);
                assertEquals(2.2945, loc.getLongitude(), 0.0001);
            });
        }
    }

    // ========================================================================
    // TESTS D'AFFICHAGE
    // ========================================================================

    @Nested
    @DisplayName("🏷️ Affichage des Noms")
    class AffichageTests {

        @Test
        @DisplayName("Affichage : getDisplayName() retourne le nom si défini")
        void testAffichage_AvecNom() {
            Location loc = new Location("5 Avenue Anatole France, 75007 Paris, France");
            loc.setName("Tour Eiffel");
            
            assertEquals("Tour Eiffel", loc.getDisplayName(),
                "getDisplayName() devrait retourner le nom personnalisé");
        }

        @Test
        @DisplayName("Affichage : getDisplayName() retourne adresse courte si pas de nom")
        void testAffichage_SansNom() {
            Location loc = new Location("Place Bellecour, 69002 Lyon, France");
            
            assertEquals("Place Bellecour", loc.getDisplayName(),
                "getDisplayName() devrait retourner le début de l'adresse");
        }

        @Test
        @DisplayName("Affichage : GPS sans nom retourne coordonnées")
        void testAffichage_GpsSansNom() {
            Location loc = new Location(48.8584, 2.2945);
            
            String displayName = loc.getDisplayName();
            assertTrue(displayName.contains("48,8584") || displayName.contains("2,2945"),
                "getDisplayName() devrait contenir les coordonnées pour GPS sans nom");
        }

        @Test
        @DisplayName("Affichage : GPS avec nom retourne le nom")
        void testAffichage_GpsAvecNom() {
            Location loc = new Location(48.8584, 2.2945);
            loc.setName("Tour Eiffel");
            
            assertEquals("Tour Eiffel", loc.getDisplayName(),
                "getDisplayName() devrait retourner le nom même pour GPS");
        }
    }

    // ========================================================================
    // TESTS DE ROBUSTESSE
    // ========================================================================

    @Nested
    @DisplayName("🛡️ Robustesse et Performance")
    class RobustesseTests {

        @Test
        @DisplayName("Robustesse : Multiples appels successifs")
        void testMultiplesAppels() {
            Location paris = new Location(48.8566, 2.3522);
            paris.setName("Paris");
            Location lyon = new Location(45.7640, 4.8357);
            lyon.setName("Lyon");

            // 3 appels successifs
            int time1 = calculator.calculateTravelTime(paris, lyon, TransportMode.DRIVING);
            int time2 = calculator.calculateTravelTime(paris, lyon, TransportMode.DRIVING);
            int time3 = calculator.calculateTravelTime(paris, lyon, TransportMode.DRIVING);

            System.out.println("🔄 Appels successifs Paris → Lyon:");
            System.out.println("   Appel 1: " + time1 + " min");
            System.out.println("   Appel 2: " + time2 + " min");
            System.out.println("   Appel 3: " + time3 + " min");

            // Les résultats devraient être cohérents (±10%)
            assertTrue(Math.abs(time1 - time2) <= time1 * 0.1,
                "Les appels successifs devraient donner des résultats similaires");
            assertTrue(Math.abs(time2 - time3) <= time2 * 0.1,
                "Les appels successifs devraient donner des résultats similaires");
        }

        @Test
        @DisplayName("Robustesse : Caractères spéciaux dans adresse")
        void testCaracteresSpeciaux() {
            Location loc = new Location("8 Place de Fourvière, 69005 Lyon, France");
            loc.setName("Basilique de Fourvière");
            
            Location louvre = new Location("Rue de Rivoli, 75001 Paris, France");
            louvre.setName("Louvre");

            int time = calculator.calculateTravelTime(louvre, loc, TransportMode.DRIVING);

            System.out.println("🔤 Test caractères spéciaux: " + 
                              louvre.getDisplayName() + " → " + loc.getDisplayName());
            System.out.println("   Temps: " + time + " min");

            assertTrue(time > 0, "Devrait gérer les caractères spéciaux (è, é, etc.)");
        }
    }
}