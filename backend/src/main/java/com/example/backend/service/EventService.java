package com.example.backend.service;

import com.example.backend.controller.EventController.EventRequest;
import com.example.backend.model.Event;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Interface de service pour la gestion des événements et la planification.
 */
public interface EventService {
    
    // Méthode nécessaire pour l'optimiseur et le front-end simple
    List<Event> getEventsByUserId(Long userId);

    // Méthode CRUD (Ajoutée par 5512fe3)
    Event getEventById(Long id);
    
    // Méthode CRUD (Ajoutée par 5512fe3)
    Event createEvent(EventRequest eventRequest);

    // Méthode CRUD (Ajoutée par 5512fe3)
    Event updateEvent(Long id, EventRequest eventRequest);

    // Méthode CRUD (Ajoutée par 5512fe3)
    void deleteEvent(Long id);

    // Méthode de recherche par période (Ajoutée par 5512fe3)
    List<Event> getEventsByUserIdAndPeriod(Long userId, LocalDateTime start, LocalDateTime end);

    /**
     * Recalcule les temps de trajet pour tous les événements d'un utilisateur
     * selon le mode de calcul spécifié.
     * @param userId ID de l'utilisateur
     * @param useGoogleMaps true pour Google Maps, false pour calcul simple
     */
    void recalculateAllTravelTimes(Long userId, Boolean useGoogleMaps);

    // Lire le calendrier d'un coéquipier
    // requesterId = celui qui veut voir, targetUserId = celui qu'on regarde
    List<Event> getTeamMemberEvents(Long requesterId, Long memberId);
}