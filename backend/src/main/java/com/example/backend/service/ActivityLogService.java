package com.example.backend.service;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.example.backend.dto.ActivityStatsDTO;
import com.example.backend.model.ActivityCategory;
import com.example.backend.model.ActivityLog;
import com.example.backend.repository.ActivityLogRepository;
@Service
public class ActivityLogService {
    @Autowired
    private ActivityLogRepository activityLogRepository;

    public ActivityLog recordActivity(Long userId, ActivityCategory activityType, Long eventId, java.time.LocalDateTime startTime, java.time.LocalDateTime endTime) {
        ActivityLog log = new ActivityLog();
        log.setUserId(userId);
        log.setActivityType(activityType);
        log.setEventId(eventId);
        log.setStartTime(startTime);
        log.setEndTime(endTime);
        return activityLogRepository.save(log);
    }

    public Map<ActivityCategory, Long> getTotalTimeByActivityType(Long userId) {
        List<ActivityLog> logs = activityLogRepository.findByUserId(userId);
        Map<ActivityCategory, Long> timeByType = new HashMap<>();
        for (ActivityLog log : logs) {
            long minutes = Duration.between(log.getStartTime(), log.getEndTime()).toMinutes();
            timeByType.put(log.getActivityType(), timeByType.getOrDefault(log.getActivityType(), 0L) + minutes);
        }
        return timeByType;
    }
    public List<ActivityStatsDTO> getStats(Long userId, LocalDateTime start, LocalDateTime end) {
        // 1. Récupérer les logs filtrés
        List<ActivityLog> logs = activityLogRepository.findByUserIdAndStartTimeBetween(userId, start, end);

        // 2. Grouper par catégorie
        Map<ActivityCategory, List<ActivityLog>> groupedLogs = logs.stream()
            .collect(Collectors.groupingBy(ActivityLog::getActivityType));

        // 3. Calculer les stats pour chaque catégorie
        List<ActivityStatsDTO> stats = new ArrayList<>();
        
        // On parcourt toutes les catégories possibles pour avoir des zéros si aucune activité
        for (ActivityCategory category : ActivityCategory.values()) {
            List<ActivityLog> catLogs = groupedLogs.getOrDefault(category, Collections.emptyList());
            
            long count = catLogs.size();
            long totalMinutes = catLogs.stream()
                .mapToLong(log -> Duration.between(log.getStartTime(), log.getEndTime()).toMinutes())
                .sum();

            stats.add(new ActivityStatsDTO(category, count, totalMinutes));
        }
        
        return stats;
    }
}
