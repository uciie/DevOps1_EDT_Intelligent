package com.example.backend.service.impl;

import com.example.backend.model.Location;
import com.example.backend.model.TravelTime.TransportMode;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class SimpleTravelTimeCalculatorTest {

    private final SimpleTravelTimeCalculator calculator = new SimpleTravelTimeCalculator();

    @Test
    void testCalculateTravelTime_Walking() {
        // Given: Deux points distants d'environ 1 km
        // Point A
        Location from = new Location(48.8566, 2.3522); 
        // Point B (environ 1km plus loin)
        Location to = new Location(48.8656, 2.3522);   

        // When: Vitesse marche = 5 km/h. 1km = 12 min.
        int result = calculator.calculateTravelTime(from, to, TransportMode.WALKING);

        // Then
        assertTrue(result >= 10 && result <= 14, "Devrait être environ 12 minutes");
    }

    @Test
    void testCalculateTravelTime_Driving_MinimumBuffer() {
        // Given: Deux points très proches (100 mètres)
        Location from = new Location(48.8566, 2.3522); 
        Location to = new Location(48.8570, 2.3522);   

        // When: En voiture, cela prend quelques secondes
        int result = calculator.calculateTravelTime(from, to, TransportMode.DRIVING);

        // Then: Le buffer minimum de 5 minutes doit s'appliquer
        assertEquals(5, result);
    }
    
    @Test
    void testCalculateTravelTime_DifferentModes() {
        // Given: Distance ~10km
        Location from = new Location(48.8566, 2.3522); 
        Location to = new Location(48.9466, 2.3522); 
        
        // When
        int timeWalking = calculator.calculateTravelTime(from, to, TransportMode.WALKING);
        int timeDriving = calculator.calculateTravelTime(from, to, TransportMode.DRIVING);
        
        // Then: Marcher doit être plus long que conduire
        assertTrue(timeWalking > timeDriving);
    }
}