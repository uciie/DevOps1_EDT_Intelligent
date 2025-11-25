package com.example.backend.service;

import com.example.backend.controller.EventController.EventRequest;
import com.example.backend.model.Event;
import com.example.backend.model.Location;
import com.example.backend.model.User;
import com.example.backend.repository.EventRepository;
import com.example.backend.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Service pour la gestion des événements.
 */
@Service
public class EventService {

    private final EventRepository eventRepository;
    private final UserRepository userRepository;

    public EventService(EventRepository eventRepository, UserRepository userRepository) {
        this.eventRepository = eventRepository;
        this.userRepository = userRepository;
    }

    /**
     * Récupère tous les événements d'un utilisateur.
     *
     * @param userId l'ID de l'utilisateur
     * @return liste des événements
     */
    public List<Event> getEventsByUserId(Long userId) {
        return eventRepository.findAll().stream()
                .filter(event -> event.getUser() != null && event.getUser().getId().equals(userId))
                .sorted((e1, e2) -> e1.getStartTime().compareTo(e2.getStartTime()))
                .collect(Collectors.toList());
    }

    /**
     * Récupère un événement par son ID.
     *
     * @param id l'ID de l'événement
     * @return l'événement
     */
    public Event getEventById(Long id) {
        return eventRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Event not found with id: " + id));
    }

    /**
     * Crée un nouvel événement.
     *
     * @param eventRequest les données de l'événement
     * @return l'événement créé
     */
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
     * Met à jour un événement existant.
     *
     * @param id l'ID de l'événement
     * @param eventRequest les nouvelles données
     * @return l'événement mis à jour
     */
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
     * Supprime un événement.
     *
     * @param id l'ID de l'événement
     */
    @Transactional
    public void deleteEvent(Long id) {
        if (!eventRepository.existsById(id)) {
            throw new IllegalArgumentException("Event not found with id: " + id);
        }
        eventRepository.deleteById(id);
    }

    /**
     * Récupère les événements d'un utilisateur dans une période donnée.
     *
     * @param userId l'ID de l'utilisateur
     * @param start date de début
     * @param end date de fin
     * @return liste des événements dans la période
     */
    public List<Event> getEventsByUserIdAndPeriod(Long userId, LocalDateTime start, LocalDateTime end) {
        return eventRepository.findAll().stream()
                // 1. Filtrer par User
                .filter(event -> event.getUser() != null && event.getUser().getId().equals(userId))
                // 2. Filtrer par Date (Start time is within the range)
                .filter(event -> {
                    LocalDateTime eventStart = event.getStartTime();
                    return (eventStart.isEqual(start) || eventStart.isAfter(start)) &&
                        (eventStart.isBefore(end) || eventStart.isEqual(end));
                })
                // 3. Trier par heure de début
                .sorted((e1, e2) -> e1.getStartTime().compareTo(e2.getStartTime()))
                .collect(Collectors.toList());
    }
}