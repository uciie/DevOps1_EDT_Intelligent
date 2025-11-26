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
}