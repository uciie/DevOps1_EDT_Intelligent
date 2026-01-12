package com.example.backend.service.impl;

import com.example.backend.model.Event;
import com.example.backend.model.Task;
import com.example.backend.repository.EventRepository;
import com.example.backend.repository.TaskRepository;
import com.example.backend.service.ScheduleOptimizerService;
import com.example.backend.service.strategy.TaskSelectionStrategy;
import com.example.backend.service.TravelTimeService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;

@Service
public class DefaultScheduleOptimizerService implements ScheduleOptimizerService {

    private final EventRepository eventRepository;
    private final TaskRepository taskRepository;
    private final TaskSelectionStrategy taskSelectionStrategy;
    private final TravelTimeService travelTimeService;
    private final FocusService focusService; // AJOUT : Dépendance au service de Focus

    public DefaultScheduleOptimizerService(EventRepository eventRepository,
                                           TaskRepository taskRepository,
                                           TaskSelectionStrategy taskSelectionStrategy,
                                           TravelTimeService travelTimeService,
                                           FocusService focusService) {
        this.eventRepository = eventRepository;
        this.taskRepository = taskRepository;
        this.taskSelectionStrategy = taskSelectionStrategy;
        this.travelTimeService = travelTimeService;
        this.focusService = focusService;
    }

    @Override
    @Transactional
    public void reshuffle(Long userId) {
        // 1. Charger l'existant
        List<Event> events = eventRepository.findByUser_IdOrderByStartTime(userId);
        List<Task> tasks = taskRepository.findByUser_Id(userId);

    // 3️⃣ Trier les tâches par deadline (plus proche d'abord) 
    // puis par priorité (1 avant 5)
    tasks.sort(Comparator
        .comparing(Task::getDeadline, Comparator.nullsLast(Comparator.naturalOrder()))
        .thenComparing(Task::getPriority) 
    );
        // 3. Initialiser le curseur (8h00 aujourd'hui ou l'heure actuelle si plus tard)
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime cursor = now.withHour(8).withMinute(0).withSecond(0).withNano(0);
        if (cursor.isBefore(now)) {
            cursor = now;
        }

        for (Task task : tasks) {
            // Ignorer les tâches déjà faites ou déjà liées à un événement valide
            if (task.isDone() || task.getEvent() != null) {
                continue;
            }

            // Marquer comme en retard si la deadline est dépassée
            if (task.getDeadline() != null && task.getDeadline().isBefore(now)) {
                task.setLate(true);
                taskRepository.save(task);
                continue;
            }

            boolean placed = false;
            
            // On cherche un créneau en boucle jusqu'à ce que la tâche soit placée
            while (!placed) {
                LocalDateTime potentialStart = cursor;
                LocalDateTime potentialEnd = cursor.plusMinutes(task.getEstimatedDuration());

                // A. Vérifier les collisions avec les ÉVÉNEMENTS existants
                Event collisionEvent = findCollision(potentialStart, potentialEnd, events);
                
                if (collisionEvent != null) {
                    // Si collision avec une réunion, on déplace le curseur après celle-ci
                    cursor = collisionEvent.getEndTime();
                    continue;
                }

                // B. Vérifier si le créneau est bloqué par le MODE FOCUS
                if (focusService.estBloqueParLeFocus(userId, potentialStart, potentialEnd)) {
                    // Si bloqué par le focus, on avance par petits pas (ex: 15min) pour chercher le prochain trou
                    cursor = cursor.plusMinutes(15);
                    continue;
                }

                // C. Si on arrive ici, le créneau est libre de réunions ET de focus !
                Event taskEvent = new Event(
                        task.getTitle(),
                        potentialStart,
                        potentialEnd,
                        task.getUser()
                );

                eventRepository.save(taskEvent);
                task.setEvent(taskEvent);
                taskRepository.save(task);

                // On met à jour le curseur pour la tâche suivante
                cursor = taskEvent.getEndTime();
                placed = true;
                
                // On rafraîchit la liste des événements pour inclure celui qu'on vient de créer
                events.add(taskEvent);
                events.sort(Comparator.comparing(Event::getStartTime));
            }
        }
    }

    /**
     * Vérifie si un créneau temporel chevauche un événement existant.
     */
    private Event findCollision(LocalDateTime start, LocalDateTime end, List<Event> events) {
        return events.stream()
                .filter(e -> start.isBefore(e.getEndTime()) && end.isAfter(e.getStartTime()))
                .findFirst()
                .orElse(null);
    }


}