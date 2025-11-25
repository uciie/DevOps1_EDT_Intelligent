package com.example.backend.service.impl;

import com.example.backend.model.Event;
import com.example.backend.model.Task;
import com.example.backend.repository.EventRepository;
import com.example.backend.repository.TaskRepository;
import com.example.backend.service.ScheduleOptimizerService;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;

@Service
public class DefaultScheduleOptimizerService implements ScheduleOptimizerService {

    private final EventRepository eventRepository;
    private final TaskRepository taskRepository;

    public DefaultScheduleOptimizerService(EventRepository eventRepository, TaskRepository taskRepository) {
        this.eventRepository = eventRepository;
        this.taskRepository = taskRepository;
    }

    // Dans DefaultScheduleOptimizerService.java

    @Override
    public void reshuffle(Long userId) {

        // 1️⃣ Charger tous les événements existants (emploi du temps)
        // NOTE : Assurez-vous que cette liste est triée par startTime pour que l'itération fonctionne.
        List<Event> events = eventRepository.findByUser_IdOrderByStartTime(userId);

        // 2️⃣ Charger toutes les tâches non planifiées (vous devriez filtrer celles SANS événement pour être strict)
        // Pour l'instant, on garde votre implémentation qui charge toutes les tâches
        List<Task> tasks = taskRepository.findByUser_Id(userId);

        // 3️⃣ Trier les tâches par deadline puis priorité
        tasks.sort(Comparator
                .comparing(Task::getDeadline, Comparator.nullsLast(Comparator.naturalOrder()))
                .thenComparing(Task::getPriority).reversed()
        );

        // 4️⃣ Positionner les tâches dans les créneaux libres
        // Le curseur doit commencer après le dernier événement existant s'il y en a.
        // Pour simplifier, on commence à 8h aujourd'hui.
        LocalDateTime cursor = LocalDateTime.now().withHour(8).withMinute(0); 
        
        // Si la liste des événements n'est pas vide, le curseur devrait commencer après le dernier événement actuel
        if (!events.isEmpty()) {
            cursor = events.get(events.size() - 1).getEndTime();
        }


        for (Task task : tasks) {
            
            // Si la tâche est déjà associée à un événement, on l'ignore (sinon on la replanifie)
            if (task.getEvent() != null) {
                continue; 
            }

            // si deadline dépassée → skip
            if (task.getDeadline() != null && task.getDeadline().isBefore(LocalDateTime.now())) {
                task.setLate(true);
                taskRepository.save(task);
                continue;
            }

            boolean placed = false;

            // I. PLACEMENT DANS LES TROUS ENTRE ÉVÉNEMENTS EXISTANTS
            for (Event event : events) {

                // Créneau libre entre "cursor" et "event.start" est suffisant pour la tâche
                if (cursor.plusMinutes(task.getEstimatedDuration()).isBefore(event.getStartTime())) {

                    Event newEvent = new Event(
                            task.getTitle(),
                            cursor,
                            cursor.plusMinutes(task.getEstimatedDuration()),
                            event.getUser()
                    );

                    eventRepository.save(newEvent);

                    task.setEvent(newEvent);
                    taskRepository.save(task);
                    
                    // AJOUT CRUCIAL : Mettre à jour le curseur au même endroit que le nouvel événement
                    cursor = newEvent.getEndTime(); 

                    placed = true;
                    break;
                }

                // Si pas de place, le curseur avance à la fin de l'événement actuel
                cursor = event.getEndTime();
            }

            // II. PLACEMENT APRÈS TOUS LES ÉVÉNEMENTS EXISTANTS
            if (!placed) {
                
                // On peut ajouter ici une logique pour passer au jour suivant (ex: 8h00 demain) si le cursor est après 18h00.
                
                // Création de l'événement à partir du curseur actuel
                Event newEvent = new Event(
                    task.getTitle(),
                    cursor,
                    cursor.plusMinutes(task.getEstimatedDuration()),
                    task.getUser() // <-- Nous utilisons toujours l'utilisateur de la tâche pour garantir qu'il n'est pas null.
                );

                eventRepository.save(newEvent);

                task.setEvent(newEvent);
                taskRepository.save(task);
                
                // AJOUT CRUCIAL : Mettre à jour le curseur à la fin du nouvel événement
                cursor = newEvent.getEndTime(); 
            }
        }
    }
}
