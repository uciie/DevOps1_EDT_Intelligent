package com.example.backend.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;

/**
 * Représente un temps de déplacement entre deux événements.
 * Cet événement spécial est automatiquement créé pour gérer les trajets.
 */
@Entity
public class TravelTime {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // Événement de départ, pour avoir le point de départ du trajet
    @ManyToOne
    @JoinColumn(name = "from_event_id")
    private Event fromEvent;

    // Événement d'arrivée, pour avoir le point d'arrivée du trajet
    @ManyToOne
    @JoinColumn(name = "to_event_id")
    private Event toEvent;

    // Utilisateur auquel ce temps de trajet appartient
    @ManyToOne
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    private LocalDateTime startTime;
    private LocalDateTime endTime;

    // Durée en minutes
    private int durationMinutes;

    // Distance en kilomètres
    private Double distanceKm;

    // Mode de transport (WALKING, DRIVING, TRANSIT, CYCLING)
    @Enumerated(EnumType.STRING)
    private TransportMode mode = TransportMode.DRIVING;

    /**
     * Constructeur par défaut.
     */
    public TravelTime() {}

    /**
     * Construit un nouveau temps de trajet.
     *
     * @param fromEvent l'événement de départ
     * @param toEvent l'événement d'arrivée
     * @param user l'utilisateur
     * @param startTime l'heure de début du trajet
     * @param durationMinutes la durée en minutes
     */
    public TravelTime(Event fromEvent, Event toEvent, User user, 
                      LocalDateTime startTime, int durationMinutes) {
        this.fromEvent = fromEvent;
        this.toEvent = toEvent;
        this.user = user;
        this.startTime = startTime;
        this.durationMinutes = durationMinutes;
        this.endTime = startTime.plusMinutes(durationMinutes);
    }

    // Getters et Setters

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    
    public Event getFromEvent() { return fromEvent; }
    public void setFromEvent(Event fromEvent) { this.fromEvent = fromEvent; }

    public Event getToEvent() { return toEvent; }
    public void setToEvent(Event toEvent) { this.toEvent = toEvent; }

    public User getUser() { return user; }
    public void setUser(User user) { this.user = user; }

    public LocalDateTime getStartTime() { return startTime; }

    /**
     * Définit l'heure de début du trajet et met à jour l'heure de fin en fonction de la durée.
     *
     * @param startTime l'heure de début du trajet
     */
    public void setStartTime(LocalDateTime startTime) { 
        this.startTime = startTime;
        // Met à jour l'heure de fin si la durée est déjà définie
        if (durationMinutes > 0) {
            this.endTime = startTime.plusMinutes(durationMinutes);
        }
    }

    public LocalDateTime getEndTime() { return endTime; }
    public void setEndTime(LocalDateTime endTime) { this.endTime = endTime; }

    public int getDurationMinutes() { return durationMinutes; }

    /**
     * Définit la durée du trajet en minutes et met à jour l'heure de fin en fonction de l'heure de début.
     *
     * @param durationMinutes la durée du trajet en minutes
     */
    public void setDurationMinutes(int durationMinutes) { 
        this.durationMinutes = durationMinutes;
        // Met à jour l'heure de fin si l'heure de début est déjà définie
        if (startTime != null) {
            this.endTime = startTime.plusMinutes(durationMinutes);
        }
    }

    public Double getDistanceKm() { return distanceKm; }
    public void setDistanceKm(Double distanceKm) { this.distanceKm = distanceKm; }

    public TransportMode getMode() { return mode; }
    public void setMode(TransportMode mode) { this.mode = mode; }

    /**
     * Énumération des modes de transport disponibles.
     */
    public enum TransportMode {
        WALKING,// À pied
        DRIVING,// En voiture
        TRANSIT,// Transports en commun
        CYCLING// À vélo
    }
}