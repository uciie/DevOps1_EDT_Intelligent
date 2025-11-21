package com.example.backend.service;

import java.time.Duration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.example.backend.model.ActivityLog;
import com.example.backend.repository.ActivityLogRepository;

@Service
public class ActivityLogService {
    @Autowired
    private ActivityLogRepository activityLogRepository;

    public ActivityLog recordActivity(Long userId, String activityType, Long eventId, java.time.LocalDateTime startTime, java.time.LocalDateTime endTime) {
        ActivityLog log = new ActivityLog();
        log.setUserId(userId);
        log.setActivityType(activityType);
        log.setEventId(eventId);
        log.setStartTime(startTime);
        log.setEndTime(endTime);
        return activityLogRepository.save(log);
    }

    public Map<String, Long> getTotalTimeByActivityType(Long userId) {
        List<ActivityLog> logs = activityLogRepository.findByUserId(userId);
        Map<String, Long> timeByType = new HashMap<>();
        for (ActivityLog log : logs) {
            long minutes = Duration.between(log.getStartTime(), log.getEndTime()).toMinutes();
            timeByType.put(log.getActivityType(), timeByType.getOrDefault(log.getActivityType(), 0L) + minutes);
        }
        return timeByType;
    }
}
