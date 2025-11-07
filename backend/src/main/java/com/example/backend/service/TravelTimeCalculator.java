package com.example.backend.service;

import com.example.backend.model.Location;
import com.example.backend.model.TravelTime.TransportMode;

/**
 * Interface pour le calcul du temps de trajet.
 * Permet d'avoir plusieurs implémentations (simple, API externe, etc.)
 */
public interface TravelTimeCalculator {
    
    /**
     * Calcule le temps de trajet entre deux lieux.
     *
     * @param from la localisation de départ
     * @param to la localisation d'arrivée
     * @param mode le mode de transport
     * @return le temps de trajet en minutes
     */
    int calculateTravelTime(Location from, Location to, TransportMode mode);
}