package com.example.backend.service.impl;

import com.example.backend.model.Location;
import com.example.backend.model.TravelTime.TransportMode;
import com.example.backend.service.TravelTimeCalculator;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

/**
 * Implémentation simple du calcul de temps de trajet.
 * Utilise des vitesses moyennes pour estimer le temps.
 */
@Component
@Profile("!test & !external-api") // Ne pas créer ce bean pendant les tests (utiliser TestConfig)
public class SimpleTravelTimeCalculator implements TravelTimeCalculator {

    // Vitesses moyennes en km/h
    private static final double WALKING_SPEED = 5.0;
    private static final double CYCLING_SPEED = 15.0;
    private static final double DRIVING_SPEED = 40.0; // En ville
    private static final double TRANSIT_SPEED = 25.0;

    @Override
    public int calculateTravelTime(Location from, Location to, TransportMode mode) {
        if (!from.hasCoordinates() || !to.hasCoordinates()) {
            // Par défaut, on estime 15 minutes si pas de coordonnées
            return 15;
        }

        // Calcul de la distance avec la formule de Haversine
        double distanceKm = calculateDistance(from, to);

        // Choix de la vitesse selon le mode
        double speed = switch (mode) {
            case WALKING -> WALKING_SPEED;
            case CYCLING -> CYCLING_SPEED;
            case DRIVING -> DRIVING_SPEED;
            case TRANSIT -> TRANSIT_SPEED;
        };

        // Calcul du temps en minutes (distance / vitesse * 60)
        int travelTimeMinutes = (int) Math.ceil((distanceKm / speed) * 60);

        // Ajout d'un buffer minimum de 5 minutes
        return Math.max(travelTimeMinutes, 5);
    }

    /**
     * Calcule la distance entre deux lieux avec la formule de Haversine.
     *
     * @param from la localisation de départ
     * @param to la localisation d'arrivée
     * @return la distance en kilomètres
     */
    private double calculateDistance(Location from, Location to) {
        final int EARTH_RADIUS = 6371;

        double latDistance = Math.toRadians(to.getLatitude() - from.getLatitude());
        double lonDistance = Math.toRadians(to.getLongitude() - from.getLongitude());

        double a = Math.sin(latDistance / 2) * Math.sin(latDistance / 2)
                + Math.cos(Math.toRadians(from.getLatitude())) 
                * Math.cos(Math.toRadians(to.getLatitude()))
                * Math.sin(lonDistance / 2) * Math.sin(lonDistance / 2);

        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));

        return EARTH_RADIUS * c;
    }
}