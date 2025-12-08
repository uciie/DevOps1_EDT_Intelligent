package com.example.backend.service.impl;

import com.example.backend.controller.EventController.EventRequest;
import com.example.backend.model.Event;
import com.example.backend.model.Location;
import com.example.backend.model.TravelTime;
import com.example.backend.model.User;
import com.example.backend.repository.EventRepository;
import com.example.backend.repository.UserRepository;
import com.example.backend.service.EventService;
import com.example.backend.service.TravelTimeCalculator; // Import du calculateur
import com.example.backend.service.TravelTimeService; // Import du service de persistance

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;

/**
 * Implémentation du service pour la gestion des événements.
 */
@Service
public class EventServiceImpl implements EventService {

    private final EventRepository eventRepository;
    private final UserRepository userRepository;
    private final TravelTimeService travelTimeService; 
    private final TravelTimeCalculator travelTimeCalculator;

    // Injection des dépendances nécessaires
    public EventServiceImpl(EventRepository eventRepository, 
                            UserRepository userRepository,
                            TravelTimeService travelTimeService,
                            TravelTimeCalculator travelTimeCalculator) {
        this.eventRepository = eventRepository;
        this.userRepository = userRepository;
        this.travelTimeService = travelTimeService;
        this.travelTimeCalculator = travelTimeCalculator;
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
     * Crée un nouvel événement avec vérification de faisabilité de trajet.
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

        // --- VERIFICATION DE FAISABILITE AVANT SAUVEGARDE ---
        // Si l'utilisateur a spécifié un mode de transport et que l'événement a une localisation
        if (eventRequest.getTransportMode() != null && event.getLocation() != null) {
            
            // 1. Récupérer les événements de l'utilisateur pour trouver le précédent
            List<Event> userEvents = eventRepository.findByUser_IdOrderByStartTime(user.getId());
            
            // 2. Trouver l'événement qui termine avant ou exactement au début du nouvel événement
            Event previousEvent = userEvents.stream()
                .filter(e -> e.getEndTime().isBefore(event.getStartTime()) || 
                             e.getEndTime().isEqual(event.getStartTime()))
                .max(Comparator.comparing(Event::getEndTime))
                .orElse(null);

            // 3. Si un événement précédent existe et a une localisation
            if (previousEvent != null && previousEvent.getLocation() != null) {
                try {
                    // Conversion du String en Enum
                    TravelTime.TransportMode mode = TravelTime.TransportMode.valueOf(eventRequest.getTransportMode());
                    
                    // A. Calculer le temps de trajet SANS le créer tout de suite
                    int durationMinutes = travelTimeCalculator.calculateTravelTime(
                        previousEvent.getLocation(),
                        event.getLocation(),
                        mode
                    );
                    
                    // B. Vérifier si on arrive à temps
                    // Heure de départ = fin de l'événement A
                    LocalDateTime departureTime = previousEvent.getEndTime();
                    // Heure d'arrivée estimée = départ + trajet
                    LocalDateTime estimatedArrival = departureTime.plusMinutes(durationMinutes);
                    
                    // Si l'arrivée estimée est STRICTEMENT APRES le début de l'événement B
                    if (estimatedArrival.isAfter(event.getStartTime())) {
                        throw new IllegalArgumentException(
                            String.format("Impossible d'arriver à l'heure ! Fin de l'événement précédent : %s. " +
                                          "Temps de trajet (%s) : %d min. Arrivée estimée : %s. " +
                                          "Début de l'événement prévu : %s.",
                                          departureTime.toLocalTime(),
                                          mode,
                                          durationMinutes,
                                          estimatedArrival.toLocalTime(),
                                          event.getStartTime().toLocalTime())
                        );
                    }
                    
                    // C. Si c'est faisable, on sauvegarde l'événement
                    Event savedEvent = eventRepository.save(event);
                    
                    // D. Et on crée le TravelTime en base pour la persistance
                    travelTimeService.createTravelTime(previousEvent, savedEvent, mode);
                    
                    return savedEvent;

                } catch (java.lang.IllegalArgumentException e) {
                    // Relance l'exception pour qu'elle soit gérée par le contrôleur (retour 400)
                    throw e; 
                }
            }
        }

        // Si pas de conflit ou pas de vérification nécessaire, sauvegarde simple
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

        // --- VERIFICATION DE FAISABILITE AVANT SAUVEGARDE (AJOUT POUR UPDATE) ---
        // Si l'utilisateur a spécifié un mode de transport et que l'événement a une localisation
        if (eventRequest.getTransportMode() != null && event.getLocation() != null) {
            
            // 1. Récupérer les événements de l'utilisateur pour trouver le précédent
            List<Event> userEvents = eventRepository.findByUser_IdOrderByStartTime(event.getUser().getId());
            
            // 2. Trouver l'événement qui termine avant ou exactement au début de l'événement modifié
            // IMPORTANT : On exclut l'événement lui-même (id) pour ne pas le comparer avec sa propre ancienne version en mémoire
            Event previousEvent = userEvents.stream()
                .filter(e -> !e.getId().equals(event.getId())) 
                .filter(e -> e.getEndTime().isBefore(event.getStartTime()) || 
                             e.getEndTime().isEqual(event.getStartTime()))
                .max(Comparator.comparing(Event::getEndTime))
                .orElse(null);

            // 3. Si un événement précédent existe et a une localisation
            if (previousEvent != null && previousEvent.getLocation() != null) {
                try {
                    // Conversion du String en Enum
                    TravelTime.TransportMode mode = TravelTime.TransportMode.valueOf(eventRequest.getTransportMode());
                    
                    // A. Calculer le temps de trajet
                    int durationMinutes = travelTimeCalculator.calculateTravelTime(
                        previousEvent.getLocation(),
                        event.getLocation(),
                        mode
                    );
                    
                    // B. Vérifier si on arrive à temps
                    LocalDateTime departureTime = previousEvent.getEndTime();
                    LocalDateTime estimatedArrival = departureTime.plusMinutes(durationMinutes);
                    
                    if (estimatedArrival.isAfter(event.getStartTime())) {
                        throw new IllegalArgumentException(
                            String.format("Impossible d'arriver à l'heure ! Fin de l'événement précédent : %s. " +
                                          "Temps de trajet (%s) : %d min. Arrivée estimée : %s. " +
                                          "Début de l'événement prévu : %s.",
                                          departureTime.toLocalTime(),
                                          mode,
                                          durationMinutes,
                                          estimatedArrival.toLocalTime(),
                                          event.getStartTime().toLocalTime())
                        );
                    }
                    
                    // C. Si c'est faisable, on sauvegarde d'abord l'événement mis à jour
                    Event savedEvent = eventRepository.save(event);
                    
                    // D. On met à jour (ou recrée) le lien de TravelTime
                    // Note: Idéalement, on devrait supprimer l'ancien TravelTime s'il existe, 
                    // mais ici createTravelTime en créera un nouveau valide.
                    travelTimeService.createTravelTime(previousEvent, savedEvent, mode);
                    
                    return savedEvent;

                } catch (java.lang.IllegalArgumentException e) {
                    throw e; 
                }
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
        // Grâce aux cascades ajoutées dans Event.java, ceci supprimera aussi la Location liée
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