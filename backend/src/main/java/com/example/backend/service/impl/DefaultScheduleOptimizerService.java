package com.example.backend.service.impl;

import com.example.backend.service.ScheduleOptimizerService;
import com.example.backend.service.strategy.TaskSelectionStrategy;
import com.example.backend.service.TravelTimeService;
import com.example.backend.model.Event;
import com.example.backend.model.Task;
import com.example.backend.model.TravelTime;
import com.example.backend.model.TravelTime.TransportMode;
import com.example.backend.repository.EventRepository;
import com.example.backend.repository.TaskRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Service d'optimisation de l'emploi du temps avec gestion des temps de trajet.
 */
@Service
public class DefaultScheduleOptimizerService implements ScheduleOptimizerService {

    private final EventRepository eventRepository;
    private final TaskRepository taskRepository;
    private final TaskSelectionStrategy taskSelectionStrategy;
    private final TravelTimeService travelTimeService;
    
    public DefaultScheduleOptimizerService(EventRepository eventRepository,
                                           TaskRepository taskRepository,
                                           TaskSelectionStrategy taskSelectionStrategy,
                                           TravelTimeService travelTimeService) {
        this.eventRepository = eventRepository;
        this.taskRepository = taskRepository;
        this.taskSelectionStrategy = taskSelectionStrategy;
        this.travelTimeService = travelTimeService;
    }

    @Override
    @Transactional
    public void reshuffle(Long eventId) {
        Event cancelledEvent = eventRepository.findById(eventId)
                .orElseThrow(() -> new IllegalArgumentException("Event not found"));

        LocalDateTime start = cancelledEvent.getStartTime();
        LocalDateTime end = cancelledEvent.getEndTime();
        long freeMinutes = Duration.between(start, end).toMinutes();

        // Trouve l'événement suivant pour calculer le temps de trajet si nécessaire
        Event nextEvent = findNextEvent(cancelledEvent);
        
        // Si l'événement suivant existe et a une localisation différente,
        // on doit réserver du temps pour le trajet
        int travelTimeMinutes = 0;
        if (nextEvent != null && 
            cancelledEvent.getLocation() != null && 
            nextEvent.getLocation() != null &&
            !cancelledEvent.getLocation().equals(nextEvent.getLocation())) {
            
            // On estime un temps de trajet (par défaut en voiture)
            travelTimeMinutes = estimateTravelTime(cancelledEvent, nextEvent, TransportMode.DRIVING);
        }

        // Le temps disponible réel est le temps libre moins le temps de trajet
        long availableMinutes = freeMinutes - travelTimeMinutes;

        if (availableMinutes > 0) {
            // Sélection de la meilleure tâche avec le temps disponible
            Task bestTask = taskSelectionStrategy.selectTask(
                cancelledEvent.getUserId(), 
                availableMinutes
            );

            if (bestTask != null) {
                // Création du nouvel événement pour la tâche
                Event newEvent = new Event(
                        bestTask.getTitle(),
                        start,
                        start.plusMinutes(bestTask.getEstimatedDuration()),
                        cancelledEvent.getUser()
                );
                
                // Utilise la localisation de l'événement annulé si elle existe
                if (cancelledEvent.getLocation() != null) {
                    newEvent.setLocation(cancelledEvent.getLocation());
                }
                
                eventRepository.save(newEvent);

                // Si nécessaire, crée un événement de trajet vers l'événement suivant
                if (travelTimeMinutes > 0 && nextEvent != null) {
                    TravelTime travelTime = travelTimeService.createTravelTime(
                        newEvent, 
                        nextEvent, 
                        TransportMode.DRIVING
                    );
                    // Le temps de trajet est automatiquement sauvegardé
                }
            }
        }

        // Marque l'événement comme annulé
        cancelledEvent.setStatus("CANCELLED");
        eventRepository.save(cancelledEvent);
    }

    /**
     * Trouve le prochain événement chronologiquement après l'événement donné.
     *
     * @param event l'événement de référence
     * @return le prochain événement ou null
     */
    private Event findNextEvent(Event event) {
        List<Event> futureEvents = eventRepository.findAll().stream()
            .filter(e -> e.getUser().equals(event.getUser()))
            .filter(e -> e.getStartTime().isAfter(event.getEndTime()))
            .filter(e -> !"CANCELLED".equals(e.getStatus()))
            .sorted((e1, e2) -> e1.getStartTime().compareTo(e2.getStartTime()))
            .toList();
        
        return futureEvents.isEmpty() ? null : futureEvents.get(0);
    }

    /**
     * Estime le temps de trajet entre deux événements.
     *
     * @param fromEvent l'événement de départ
     * @param toEvent l'événement d'arrivée
     * @param mode le mode de transport
     * @return le temps estimé en minutes
     */
    private int estimateTravelTime(Event fromEvent, Event toEvent, TransportMode mode) {
        // Pour l'estimation, on crée temporairement un TravelTime
        // (on pourrait aussi appeler directement le TravelTimeCalculator)
        TravelTime temp = travelTimeService.createTravelTime(fromEvent, toEvent, mode);
        int duration = temp.getDurationMinutes();
        
        // On supprime le TravelTime temporaire si on ne veut pas le garder
        // travelTimeService.deleteTravelTime(temp.getId());
        
        return duration;
    }

}