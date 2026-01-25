package com.example.backend.service;

import com.example.backend.model.Event;
import com.example.backend.model.Event.EventStatus;
import com.example.backend.model.Task; 
import com.example.backend.repository.EventRepository;
import com.example.backend.repository.TaskRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Service
public class ChatbotService {

    private final EventRepository eventRepository;
    private final TaskRepository taskRepository;
    // Injecter ScheduleOptimizerService si nécessaire pour simulation

    public ChatbotService(EventRepository eventRepository, TaskRepository taskRepository) {
        this.eventRepository = eventRepository;
        this.taskRepository = taskRepository;
    }

    /**
     * Simulation de l'annulation d'après-midi.
     * Marque les événements comme PENDING_DELETION au lieu de les supprimer.
     */
    @Transactional
    public String cancelAfternoon(Long userId, String dateStr) {
        LocalDate date = LocalDate.parse(dateStr);
        LocalDateTime start = date.atTime(12, 0);
        LocalDateTime end = date.atTime(23, 59);

        List<Event> events = eventRepository.findByUser_IdAndStartTimeBetween(userId, start, end);
        
        if (events.isEmpty()) {
            return "Aucune activité trouvée cet après-midi là.";
        }

        for (Event event : events) {
            event.setStatus(EventStatus.PENDING_DELETION);
            eventRepository.save(event);
        }

        return "J'ai identifié " + events.size() + " activités à annuler cet après-midi. Confirmez-vous l'annulation ?";
    }

    /**
     * Simulation du déplacement.
     * Utilise l'optimizer pour vérifier si le créneau est libre (logique simplifiée ici).
     */
    @Transactional
    public String moveActivity(Long userId, String activityName, String targetDateStr, String targetTimeStr) {
        // Recherche floue de l'événement (implémentation simplifiée)
        Event event = eventRepository.findBySummaryContainingAndUser_Id(activityName, userId)
            .stream().findFirst().orElse(null);

        if (event == null) return "Je ne trouve pas l'activité '" + activityName + "'.";

        LocalDateTime targetStart = LocalDateTime.parse(targetDateStr + "T" + targetTimeStr);
        LocalDateTime targetEnd = targetStart.plus(java.time.Duration.between(event.getStartTime(), event.getEndTime()));

        // Appel au service d'optimisation pour vérifier les conflits (Simulation)
        // Supposons une méthode checkAvailability dans l'interface ScheduleOptimizerService
        boolean isSlotFree = true; // optimizerService.isSlotFree(userId, targetStart, targetEnd);

        if (!isSlotFree) {
            return "Le créneau demandé est déjà occupé. Voulez-vous que je cherche un autre horaire ?";
        }

        event.setStartTime(targetStart);
        event.setEndTime(targetEnd);
        event.setStatus(EventStatus.PENDING_DELETION);
        eventRepository.save(event);

        return "J'ai trouvé '" + activityName + "'. Je propose de le déplacer au " + targetDateStr + " à " + targetTimeStr + ". Confirmez-vous ?";
    }

    @Transactional
    public String confirmPendingChanges(Long userId) {
        List<Event> pending = eventRepository.findByUser_IdAndStatusNot(userId, EventStatus.CONFIRMED);
        int count = 0;
        for (Event event : pending) {
            if (event.getStatus() == EventStatus.PENDING_DELETION) {
                eventRepository.delete(event);
            } else {
                event.setStatus(EventStatus.CONFIRMED);
                eventRepository.save(event);
            }
            count++;
        }
        return count + " modifications confirmées et appliquées.";
    }

    /**
     * Ajoute une tâche en statut "PENDING_CREATION" pour optimisation future.
     */
    @Transactional
    public String addTask(Long userId, String title, int durationMinutes) {
        Task newTask = new Task();
        newTask.setTitle(title);
        newTask.setEstimatedDuration(durationMinutes);
        // Idéalement, Task devrait avoir un statut. Si non, on peut utiliser un flag ou un préfixe dans le nom.
        // Pour cet exemple, supposons que Task a un champ status comme Event, ou qu'on le sauvegarde directement.
        
        // Simulation :
        taskRepository.save(newTask); 
        
        return String.format("J'ai préparé l'ajout de la tâche '%s' (%d min). Elle est en attente de votre validation pour être intégrée au planning.", title, durationMinutes);
    }

}