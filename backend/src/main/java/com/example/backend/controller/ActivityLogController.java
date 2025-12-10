package com.example.backend.controller;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.example.backend.dto.ActivityStatsDTO;
import com.example.backend.model.ActivityCategory;
import com.example.backend.model.ActivityLog;
import com.example.backend.service.ActivityLogService;
@RestController
@RequestMapping("/api/activity")
public class ActivityLogController {
    @Autowired
    private ActivityLogService activityLogService;

    @PostMapping("/record")
    public ActivityLog recordActivity(@RequestBody ActivityRecordRequest request) {
        return activityLogService.recordActivity(
            request.getUserId(),
            request.getActivityType(),
            request.getEventId(),
            request.getStartTime(),
            request.getEndTime()
        );
    }

    @GetMapping("/stats/{userId}")
    public List<ActivityStatsDTO> getStats(
            @PathVariable Long userId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime start,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime end) {
        
        // Par défaut : les 30 derniers jours si pas de dates fournies
        if (start == null) start = LocalDateTime.now().minusDays(30);
        if (end == null) end = LocalDateTime.now();

        return activityLogService.getStats(userId, start, end);
    }

    public static class ActivityRecordRequest {
        private Long userId;
        private ActivityCategory activityType;
        private Long eventId;
        private LocalDateTime startTime;
        private LocalDateTime endTime;
        // Getters and setters
        public Long getUserId() { return userId; }
        public void setUserId(Long userId) { this.userId = userId; }
        public ActivityCategory getActivityType() { return activityType; }
        public void setActivityType(ActivityCategory activityType) { this.activityType = activityType; }
        public Long getEventId() { return eventId; }
        public void setEventId(Long eventId) { this.eventId = eventId; }
        public LocalDateTime getStartTime() { return startTime; }
        public void setStartTime(LocalDateTime startTime) { this.startTime = startTime; }
        public LocalDateTime getEndTime() { return endTime; }
        public void setEndTime(LocalDateTime endTime) { this.endTime = endTime; }
    }
}
