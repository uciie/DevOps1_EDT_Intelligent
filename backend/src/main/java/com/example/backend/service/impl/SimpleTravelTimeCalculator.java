package com.example.backend.service.impl;

import org.springframework.stereotype.Component;

import com.example.backend.model.Location;
import com.example.backend.model.TravelTime.TransportMode;
import com.example.backend.service.TravelTimeCalculator;

/**
 * Implémentation simple du calcul de temps de trajet.
 * Utilise des vitesses moyennes pour estimer le temps.
 */
@Component
//@Profile("!test & !external-api") // Ne pas créer ce bean pendant les tests unitaires purs (utiliser TestConfig)
public class SimpleTravelTimeCalculator implements TravelTimeCalculator {

    // Vitesses moyennes en km/h (Ville)
    private static final double WALKING_SPEED = 5.0;
    private static final double CYCLING_SPEED = 15.0;
    private static final double DRIVING_SPEED_CITY = 30.0;
    private static final double DRIVING_SPEED_HIGHWAY = 90.0; // Vitesse moyenne sur longue distance
    private static final double TRANSIT_SPEED_CITY = 20.0;
    private static final double TRANSIT_SPEED_HIGHWAY = 100.0; // TGV / Train moyenne

    @Override
    public int calculateTravelTime(Location from, Location to, TransportMode mode) {
        System.out.println("  Using SimpleTravelTimeCalculator (fallback).");
        if (!from.hasCoordinates() || !to.hasCoordinates()) {
            // Par défaut, on estime 15 minutes si pas de coordonnées
            return 15;
        }

        // Calcul de la distance avec la formule de Haversine
        double distanceKm = calculateDistance(from, to);

        // Choix de la vitesse selon le mode et la distance
        double speed = getSpeed(mode, distanceKm);

        // Calcul du temps en minutes (distance / vitesse * 60)
        int travelTimeMinutes = (int) Math.ceil((distanceKm / speed) * 60);

        // Ajout d'un buffer minimum de 5 minutes
        return Math.max(travelTimeMinutes, 5);
    }

    private double getSpeed(TransportMode mode, double distanceKm) {
        // Si longue distance (> 50km), on suppose autoroute ou train
        boolean isLongDistance = distanceKm > 50.0;

        return switch (mode) {
            case WALKING -> WALKING_SPEED;
            case CYCLING -> CYCLING_SPEED;
            case DRIVING -> isLongDistance ? DRIVING_SPEED_HIGHWAY : DRIVING_SPEED_CITY;
            case TRANSIT -> isLongDistance ? TRANSIT_SPEED_HIGHWAY : TRANSIT_SPEED_CITY;
        };
    }

    /**
     * Calcule la distance entre deux lieux avec la formule de Haversine.
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