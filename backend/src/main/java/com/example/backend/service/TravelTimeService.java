package com.example.backend.service;

import com.example.backend.model.Event;
import com.example.backend.model.Location;
import com.example.backend.model.TravelTime;
import com.example.backend.model.TravelTime.TransportMode;
import com.example.backend.repository.TravelTimeRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

/**
 * Service pour calculer et gérer les temps de trajet entre événements
 */
@Service
public class TravelTimeService {

    private final TravelTimeRepository travelTimeRepository;
    private final TravelTimeCalculator travelTimeCalculator;

    public TravelTimeService(TravelTimeRepository travelTimeRepository,
                            TravelTimeCalculator travelTimeCalculator) {
        this.travelTimeRepository = travelTimeRepository;
        this.travelTimeCalculator = travelTimeCalculator;
    }

    /**
     * Calcule et crée un temps de trajet entre deux événements.
     *
     * @param fromEvent l'événement de départ
     * @param toEvent l'événement d'arrivée
     * @param mode le mode de transport
     * @pre fromEvent et toEvent doivent avoir une localisation définie
     * @return le temps de trajet créé
     */
    @Transactional
    public TravelTime createTravelTime(Event fromEvent, Event toEvent, TransportMode mode) {
        // Vérifie que les deux événements ont une localisation
        if (fromEvent.getLocation() == null || toEvent.getLocation() == null) {
            throw new IllegalArgumentException("Les événements doivent avoir une localisation");
        }

        // Calcule le temps de trajet
        int durationMinutes = travelTimeCalculator.calculateTravelTime(
            fromEvent.getLocation(), 
            toEvent.getLocation(), 
            mode
        );

        // Le trajet commence à la fin de l'événement précédent
        LocalDateTime travelStartTime = fromEvent.getEndTime();

        // Crée l'objet TravelTime
        TravelTime travelTime = new TravelTime(
            fromEvent, 
            toEvent, 
            fromEvent.getUser(),
            travelStartTime, 
            durationMinutes
        );
        travelTime.setMode(mode);

        // Calcule aussi la distance si possible
        if (fromEvent.getLocation().hasCoordinates() && 
            toEvent.getLocation().hasCoordinates()) {
            double distance = calculateDistance(
                fromEvent.getLocation(), 
                toEvent.getLocation()
            );
            travelTime.setDistanceKm(distance);
        }

        return travelTimeRepository.save(travelTime);
    }

    /**
     * Met à jour le temps de trajet en fonction des modifications d'événements.
     *
     * @param travelTimeId l'ID du temps de trajet
     * @param newStartTime la nouvelle heure de début
     */
    @Transactional
    public void updateTravelTime(Long travelTimeId, LocalDateTime newStartTime) {
        TravelTime travelTime = travelTimeRepository.findById(travelTimeId)
            .orElseThrow(() -> new IllegalArgumentException("TravelTime not found"));
        
        travelTime.setStartTime(newStartTime);
        travelTimeRepository.save(travelTime);
    }

    /**
     * Supprime un temps de trajet.
     *
     * @param travelTimeId l'ID du temps de trajet
     */
    @Transactional
    public void deleteTravelTime(Long travelTimeId) {
        travelTimeRepository.deleteById(travelTimeId);
    }

    /**
     * Calcule la distance entre deux lieux en kilomètres (formule de Haversine).
     *
     * @param from la localisation de départ
     * @param to la localisation d'arrivée
     * @return la distance en kilomètres
     */
    private double calculateDistance(Location from, Location to) {
        final int EARTH_RADIUS = 6371; // Rayon de la Terre en km

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