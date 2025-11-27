package com.example.backend.service.impl;

import com.example.backend.controller.EventController.EventRequest;
import com.example.backend.model.Event;
import com.example.backend.model.Location;
import com.example.backend.model.User;
import com.example.backend.repository.EventRepository;
import com.example.backend.repository.UserRepository;
import com.example.backend.service.EventService; // Import de l'interface
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Implémentation du service pour la gestion des événements, incluant la recherche pour l'optimiseur.
 */
@Service
public class EventServiceImpl implements EventService { // Implémente l'interface

    private final EventRepository eventRepository;
    private final UserRepository userRepository; // Ajouté par la branche 5512fe3

    public EventServiceImpl(EventRepository eventRepository, UserRepository userRepository) {
        this.eventRepository = eventRepository;
        this.userRepository = userRepository;
    }

    // --- Implémentation des méthodes ---

    /**
     * Récupère tous les événements d'un utilisateur, triés par heure de début (logique de l'optimiseur).
     */
    @Override
    public List<Event> getEventsByUserId(Long userId) {
        // Utilisation de la méthode JpaRepository triée (méthode de votre branche HEAD)
        return eventRepository.findByUser_IdOrderByStartTime(userId);
    }

    /**
     * Récupère un événement par son ID (logique de 5512fe3).
     */
    @Override
    public Event getEventById(Long id) {
        return eventRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Event not found with id: " + id));
    }

    /**
     * Crée un nouvel événement (logique de 5512fe3).
     */
    @Override
    @Transactional
    public Event createEvent(EventRequest eventRequest) {
        User user = userRepository.findById(eventRequest.getUserId())
                .orElseThrow(() -> new IllegalArgumentException("User not found"));

        Event event = new Event(
                eventRequest.getSummary(),
                eventRequest.getStartTime(),
                eventRequest.getEndTime(),
                user
        );

        // Ajouter la localisation si fournie
        if (eventRequest.getLocation() != null) {
            Location location = eventRequest.getLocation().toLocation();
            if (location != null) {
                event.setLocation(location);
            }
        }

        return eventRepository.save(event);
    }

    /**
     * Met à jour un événement existant (logique de 5512fe3).
     */
    @Override
    @Transactional
    public Event updateEvent(Long id, EventRequest eventRequest) {
        Event event = getEventById(id);

        if (eventRequest.getSummary() != null) {
            event.setSummary(eventRequest.getSummary());
        }
        if (eventRequest.getStartTime() != null) {
            event.setStartTime(eventRequest.getStartTime());
        }
        if (eventRequest.getEndTime() != null) {
            event.setEndTime(eventRequest.getEndTime());
        }
        if (eventRequest.getLocation() != null) {
            Location location = eventRequest.getLocation().toLocation();
            if (location != null) {
                event.setLocation(location);
            }
        }

        return eventRepository.save(event);
    }

    /**
     * Supprime un événement (logique de 5512fe3).
     */
    @Override
    @Transactional
    public void deleteEvent(Long id) {
        if (!eventRepository.existsById(id)) {
            throw new IllegalArgumentException("Event not found with id: " + id);
        }
        eventRepository.deleteById(id);
    }

    /**
     * Récupère les événements d'un utilisateur dans une période donnée (logique de 5512fe3).
     */
    @Override
    public List<Event> getEventsByUserIdAndPeriod(Long userId, LocalDateTime start, LocalDateTime end) {
        // Utilisation de la méthode JpaRepository (méthode de 5512fe3)
        return eventRepository.findByUser_IdAndStartTimeBetween(userId, start, end);
    }
}