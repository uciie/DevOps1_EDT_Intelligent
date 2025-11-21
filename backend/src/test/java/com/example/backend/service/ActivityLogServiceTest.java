package com.example.backend.service;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import static org.mockito.ArgumentMatchers.any;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import static org.mockito.Mockito.when;
import org.mockito.MockitoAnnotations;

import com.example.backend.model.ActivityLog;
import com.example.backend.repository.ActivityLogRepository;

class ActivityLogServiceTest {
    @Mock
    private ActivityLogRepository activityLogRepository;

    @InjectMocks
    private ActivityLogService activityLogService;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
    }

    @Test
    void testRecordActivity() {
        ActivityLog log = new ActivityLog();
        log.setUserId(1L);
        log.setActivityType("work");
        log.setStartTime(LocalDateTime.now().minusHours(1));
        log.setEndTime(LocalDateTime.now());
        when(activityLogRepository.save(any(ActivityLog.class))).thenReturn(log);

        ActivityLog saved = activityLogService.recordActivity(1L, "work", null, log.getStartTime(), log.getEndTime());
        assertEquals("work", saved.getActivityType());
        assertEquals(1L, saved.getUserId());
    }

    @Test
    void testGetTotalTimeByActivityType() {
        ActivityLog log1 = new ActivityLog();
        log1.setUserId(1L);
        log1.setActivityType("work");
        log1.setStartTime(LocalDateTime.now().minusHours(2));
        log1.setEndTime(LocalDateTime.now().minusHours(1));

        ActivityLog log2 = new ActivityLog();
        log2.setUserId(1L);
        log2.setActivityType("course");
        log2.setStartTime(LocalDateTime.now().minusHours(1));
        log2.setEndTime(LocalDateTime.now());

        List<ActivityLog> logs = Arrays.asList(log1, log2);
        when(activityLogRepository.findByUserId(1L)).thenReturn(logs);

        Map<String, Long> stats = activityLogService.getTotalTimeByActivityType(1L);
        assertEquals(60, stats.get("work"));
        assertEquals(60, stats.get("course"));
    }
}
