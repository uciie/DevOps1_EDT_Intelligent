package com.example.backend.service;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
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


    public List<ActivityStatsDTO> getStats(Long userId, LocalDateTime start, LocalDateTime end) {
        List<ActivityLog> logs = activityLogRepository.findByUserIdAndStartTimeBetween(userId, start, end);

        Map<ActivityCategory, List<ActivityLog>> groupedLogs = logs.stream()
            .collect(Collectors.groupingBy(ActivityLog::getActivityType));

        List<ActivityStatsDTO> stats = new ArrayList<>();
        
        for (ActivityCategory category : ActivityCategory.values()) {
            if (category == ActivityCategory.AUTRE && !groupedLogs.containsKey(category)) continue;

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
