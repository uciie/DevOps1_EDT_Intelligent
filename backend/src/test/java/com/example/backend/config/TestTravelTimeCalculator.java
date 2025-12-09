package com.example.backend.config;

import com.example.backend.model.Location;
import com.example.backend.model.TravelTime.TransportMode;
import com.example.backend.service.TravelTimeCalculator;

import org.springframework.context.annotation.Primary;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

/**
 * Implémentation simple du TravelTimeCalculator pour les tests.
 * Retourne des durées fixes pour ne pas dépendre d'APIs externes.
 */
@Component
@Profile("test")
@Primary
public class TestTravelTimeCalculator implements TravelTimeCalculator {

    @Override
    public int calculateTravelTime(Location from, Location to, TransportMode mode) {
        // Retourne une durée fixe pour les tests
        return switch (mode) {
            case WALKING -> 30;
            case CYCLING -> 20;
            case DRIVING -> 15;
            case TRANSIT -> 25;
        };
    }
}