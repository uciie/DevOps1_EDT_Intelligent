package com.example.backend.service;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.example.backend.dto.ActivityStatsDTO;
import com.example.backend.model.ActivityCategory;
import com.example.backend.model.ActivityLog;
import com.example.backend.model.Event;
import com.example.backend.repository.ActivityLogRepository;
import com.example.backend.repository.EventRepository;
@Service
public class ActivityLogService {
    @Autowired
    private ActivityLogRepository activityLogRepository;

    private EventRepository eventRepository;

    public ActivityLog recordActivity(Long userId, ActivityCategory activityType, Long eventId, java.time.LocalDateTime startTime, java.time.LocalDateTime endTime) {
        ActivityLog log = new ActivityLog();
        log.setUserId(userId);
        log.setActivityType(activityType);
        log.setEventId(eventId);
        log.setStartTime(startTime);
        log.setEndTime(endTime);    
        return activityLogRepository.save(log);
    }


    public List<ActivityStatsDTO> getStats(Long userId, LocalDateTime start, LocalDateTime end) {
        // 1. Récupérer les logs dédiés (si vous utilisez le chronomètre manuel)
        List<ActivityLog> logs = activityLogRepository.findByUserIdAndStartTimeBetween(userId, start, end);

        // 2. Récupérer les événements du calendrier
        List<Event> events = eventRepository.findByUser_IdAndStartTimeBetween(userId, start, end);

        // 3. Fusionner les deux sources de données pour le calcul
        // On crée une map pour sommer les durées par catégorie
        Map<ActivityCategory, Long> statsMap = new java.util.HashMap<>();
        Map<ActivityCategory, Long> countMap = new java.util.HashMap<>();

        // Initialiser la map avec toutes les catégories à 0
        for (ActivityCategory cat : ActivityCategory.values()) {
            statsMap.put(cat, 0L);
            countMap.put(cat, 0L);
        }

        // Traiter les ActivityLogs
        for (ActivityLog log : logs) {
            long minutes = Duration.between(log.getStartTime(), log.getEndTime()).toMinutes();
            ActivityCategory cat = log.getActivityType();
            statsMap.put(cat, statsMap.get(cat) + minutes);
            countMap.put(cat, countMap.get(cat) + 1);
        }

        // Traiter les Events (Ceux de votre calendrier !)
        for (Event event : events) {
            // On utilise la catégorie de l'event (ou AUTRE si null)
            ActivityCategory cat = event.getCategory() != null ? event.getCategory() : ActivityCategory.AUTRE;
            
            long minutes = Duration.between(event.getStartTime(), event.getEndTime()).toMinutes();
            statsMap.put(cat, statsMap.get(cat) + minutes);
            countMap.put(cat, countMap.get(cat) + 1);
        }

        // 4. Convertir en DTO pour le frontend
        List<ActivityStatsDTO> result = new ArrayList<>();
        for (ActivityCategory cat : ActivityCategory.values()) {
            long totalMinutes = statsMap.get(cat);
            long count = countMap.get(cat);
            
            // On n'ajoute à la liste que s'il y a des données (sauf si vous voulez afficher les 0)
            if (count > 0) {
                result.add(new ActivityStatsDTO(cat, count, totalMinutes));
            }
        }
        
        return result;
    }
}
