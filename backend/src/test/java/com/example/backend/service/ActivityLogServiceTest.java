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

import com.example.backend.model.ActivityCategory;
import com.example.backend.model.ActivityLog; // Import de l'Enum
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
        // Arrange
        ActivityLog log = new ActivityLog();
        log.setUserId(1L);
        log.setActivityType(ActivityCategory.TRAVAIL); // Utilisation de l'Enum
        log.setStartTime(LocalDateTime.now().minusHours(1));
        log.setEndTime(LocalDateTime.now());
        
        when(activityLogRepository.save(any(ActivityLog.class))).thenReturn(log);

        // Act
        // On passe l'Enum ActivityCategory.TRAVAIL au lieu de "work"
        ActivityLog saved = activityLogService.recordActivity(
            1L, 
            ActivityCategory.TRAVAIL, 
            null, 
            log.getStartTime(), 
            log.getEndTime()
        );

        // Assert
        assertEquals(ActivityCategory.TRAVAIL, saved.getActivityType());
        assertEquals(1L, saved.getUserId());
    }

    @Test
    void testGetTotalTimeByActivityType() {
        // Arrange
        ActivityLog log1 = new ActivityLog();
        log1.setUserId(1L);
        log1.setActivityType(ActivityCategory.TRAVAIL); // "work" -> TRAVAIL
        log1.setStartTime(LocalDateTime.now().minusHours(2));
        log1.setEndTime(LocalDateTime.now().minusHours(1)); // Durée : 1h (60 min)

        ActivityLog log2 = new ActivityLog();
        log2.setUserId(1L);
        log2.setActivityType(ActivityCategory.ETUDE); // "course" -> ETUDE
        log2.setStartTime(LocalDateTime.now().minusHours(1));
        log2.setEndTime(LocalDateTime.now()); // Durée : 1h (60 min)

        List<ActivityLog> logs = Arrays.asList(log1, log2);
        when(activityLogRepository.findByUserId(1L)).thenReturn(logs);

        // Act
        // La map retournée utilise maintenant ActivityCategory comme clé
        Map<ActivityCategory, Long> stats = activityLogService.getTotalTimeByActivityType(1L);

        // Assert
        assertEquals(60L, stats.get(ActivityCategory.TRAVAIL));
        assertEquals(60L, stats.get(ActivityCategory.ETUDE));
    }
}