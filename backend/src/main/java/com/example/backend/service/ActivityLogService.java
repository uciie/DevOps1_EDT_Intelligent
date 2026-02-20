package com.example.backend.service;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
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

    @Autowired
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
        // 1. Initialiser les compteurs à 0
        Map<ActivityCategory, Long> durationMap = new HashMap<>();
        Map<ActivityCategory, Long> countMap = new HashMap<>();
        
        for (ActivityCategory cat : ActivityCategory.values()) {
            durationMap.put(cat, 0L);
            countMap.put(cat, 0L);
        }

        // 2. Récupérer et traiter les ActivityLogs (Chronomètre)
        List<ActivityLog> logs = activityLogRepository.findByUserIdAndStartTimeBetween(userId, start, end);
        if (logs != null) {
            for (ActivityLog log : logs) {
                if (log.getStartTime() != null && log.getEndTime() != null && log.getActivityType() != null) {
                    long minutes = Duration.between(log.getStartTime(), log.getEndTime()).toMinutes();
                    durationMap.put(log.getActivityType(), durationMap.get(log.getActivityType()) + minutes);
                    countMap.put(log.getActivityType(), countMap.get(log.getActivityType()) + 1);
                }
            }
        }

        // 3. Récupérer et traiter les Events (Calendrier)
        List<Event> events = eventRepository.findByUser_IdAndStartTimeBetween(userId, start, end);
        if (events != null) {
            for (Event event : events) {
                // Protection contre les valeurs nulles (Cause fréquente d'erreur 500)
                if (event.getStartTime() != null && event.getEndTime() != null) {
                    // Si catégorie null -> AUTRE
                    ActivityCategory cat = event.getCategory() != null ? event.getCategory() : ActivityCategory.AUTRE;
                    
                    long minutes = Duration.between(event.getStartTime(), event.getEndTime()).toMinutes();
                    durationMap.put(cat, durationMap.get(cat) + minutes);
                    countMap.put(cat, countMap.get(cat) + 1);
                }
            }
        }

        // 4. Construire la liste de résultats
        List<ActivityStatsDTO> result = new ArrayList<>();
        for (ActivityCategory cat : ActivityCategory.values()) {
            long totalMinutes = durationMap.get(cat);
            long count = countMap.get(cat);
            
            // On renvoie tout, même à 0, pour que le frontend puisse filtrer
            result.add(new ActivityStatsDTO(cat, count, totalMinutes));
        }
        
        return result;
    }
}
