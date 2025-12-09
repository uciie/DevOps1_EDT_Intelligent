package com.example.backend.service;

import com.example.backend.model.Event;
import com.example.backend.model.Location;
import com.example.backend.model.TravelTime;
import com.example.backend.model.TravelTime.TransportMode;
import com.example.backend.repository.EventRepository;
import com.example.backend.repository.TravelTimeRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Service pour calculer et gérer les temps de trajet entre événements
 */
@Service
public class TravelTimeService {

    private final TravelTimeRepository travelTimeRepository;
    private final TravelTimeCalculator travelTimeCalculator;
    private final EventRepository eventRepository;

    public TravelTimeService(TravelTimeRepository travelTimeRepository,
                            TravelTimeCalculator travelTimeCalculator,
                            EventRepository eventRepository) {
        this.travelTimeRepository = travelTimeRepository;
        this.travelTimeCalculator = travelTimeCalculator;
        this.eventRepository = eventRepository;
    }

    /**
     * Récupère tous les temps de trajet d'un utilisateur.
     *
     * @param userId l'ID de l'utilisateur
     * @return liste des temps de trajet
     */
    public List<TravelTime> getTravelTimesByUserId(Long userId) {
        return travelTimeRepository.findByUser_Id(userId);
    }

    /**
     * Récupère les temps de trajet d'un utilisateur dans une période donnée.
     *
     * @param userId l'ID de l'utilisateur
     * @param start date de début
     * @param end date de fin
     * @return liste des temps de trajet
     */
    public List<TravelTime> getTravelTimesByUserIdAndDateRange(Long userId, LocalDateTime start, LocalDateTime end) {
        return travelTimeRepository.findByUser_IdAndStartTimeBetween(userId, start, end);
    }

    /**
     * Calcule et crée un temps de trajet entre deux événements à partir de leurs IDs.
     *
     * @param fromEventId l'ID de l'événement de départ
     * @param toEventId l'ID de l'événement d'arrivée
     * @param mode le mode de transport
     * @return le temps de trajet créé
     */
    @Transactional
    public TravelTime calculateAndCreateTravelTime(Long fromEventId, Long toEventId, TransportMode mode) {
        Event fromEvent = eventRepository.findById(fromEventId)
                .orElseThrow(() -> new IllegalArgumentException("FromEvent not found"));
        Event toEvent = eventRepository.findById(toEventId)
                .orElseThrow(() -> new IllegalArgumentException("ToEvent not found"));
        
        return createTravelTime(fromEvent, toEvent, mode);
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

        // Appel de la méthode interne pour la sauvegarde
        return saveTravelTimeInternal(fromEvent, toEvent, mode, durationMinutes);
    }

    /**
     * Crée un temps de trajet avec une durée DÉJÀ connue (évite le recalcul).
     * C'est cette méthode que EventService doit appeler.
     */
    @Transactional
    public TravelTime createTravelTimeWithDuration(Event fromEvent, Event toEvent, TransportMode mode, int durationMinutes) {
        return saveTravelTimeInternal(fromEvent, toEvent, mode, durationMinutes);
    }

    /**
     * Méthode interne pour factoriser la création et la sauvegarde du TravelTime.
     */
    private TravelTime saveTravelTimeInternal(Event fromEvent, Event toEvent, TransportMode mode, int durationMinutes) {
        // Vérifie que les deux événements ont une localisation (sécurité supplémentaire)
        if (fromEvent.getLocation() == null || toEvent.getLocation() == null) {
            throw new IllegalArgumentException("Les événements doivent avoir une localisation");
        }

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

    /**
     * Estime la durée du trajet entre deux localisations selon un mode de transport, sans sauvegarder en BDD.
     *
     * @param from la localisation de départ
     * @param to la localisation d'arrivée
     * @param mode le mode de transport
     * @return la durée estimée en minutes
     */
    public int estimateDuration(Location from, Location to, TransportMode mode) {
        if (from == null || to == null) return 0;
        return travelTimeCalculator.calculateTravelTime(from, to, mode);
    }
}