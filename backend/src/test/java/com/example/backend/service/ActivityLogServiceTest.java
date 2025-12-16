package com.example.backend.service;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import static org.mockito.Mockito.when;
import org.mockito.MockitoAnnotations;

import com.example.backend.dto.ActivityStatsDTO;
import com.example.backend.model.ActivityCategory;
import com.example.backend.model.ActivityLog;
import com.example.backend.model.Event;
import com.example.backend.model.User;
import com.example.backend.repository.ActivityLogRepository;
import com.example.backend.repository.EventRepository;

class ActivityLogServiceTest {

    @Mock
    private ActivityLogRepository activityLogRepository;
    
    @Mock
    private EventRepository eventRepository;

    @InjectMocks
    private ActivityLogService activityLogService;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
    }

    @Test
    void testGetStats_IncludeCalendarEvents() {
        // ARRANGE
        LocalDateTime start = LocalDateTime.now().minusDays(7);
        LocalDateTime end = LocalDateTime.now();
        Long userId = 1L;

        // 1. Simuler un Event du calendrier (ex: 2h de Travail)
        Event workEvent = new Event();
        workEvent.setStartTime(LocalDateTime.now().minusHours(2));
        workEvent.setEndTime(LocalDateTime.now());
        workEvent.setCategory(ActivityCategory.TRAVAIL);
        workEvent.setUser(new User()); 
        
        // 2. Simuler un Log manuel (ex: 1h de Sport)
        ActivityLog sportLog = new ActivityLog();
        sportLog.setStartTime(LocalDateTime.now().minusHours(1));
        sportLog.setEndTime(LocalDateTime.now());
        sportLog.setActivityType(ActivityCategory.SPORT);
        sportLog.setUserId(userId);

        // Configuration des Mocks
        when(eventRepository.findByUser_IdAndStartTimeBetween(eq(userId), any(), any()))
            .thenReturn(Arrays.asList(workEvent));
            
        when(activityLogRepository.findByUserIdAndStartTimeBetween(eq(userId), any(), any()))
            .thenReturn(Arrays.asList(sportLog));

        // ACT
        List<ActivityStatsDTO> stats = activityLogService.getStats(userId, start, end);

        // ASSERT
        assertNotNull(stats);
        assertFalse(stats.isEmpty());

        // Vérifier le Travail (doit être ~120 min)
        ActivityStatsDTO workStat = stats.stream()
            .filter(s -> s.getCategory() == ActivityCategory.TRAVAIL)
            .findFirst().orElse(null);
        
        assertNotNull(workStat, "La catégorie TRAVAIL doit être présente");
        assertEquals(120, workStat.getTotalMinutes(), "Le temps de travail doit être de 120 min (venant du calendrier)");

        // Vérifier le Sport (doit être ~60 min)
        ActivityStatsDTO sportStat = stats.stream()
            .filter(s -> s.getCategory() == ActivityCategory.SPORT)
            .findFirst().orElse(null);
            
        assertEquals(60, sportStat.getTotalMinutes(), "Le temps de sport doit être de 60 min (venant des logs)");
    }

    @Test
    void testGetStats_HandleNullCategorySafely() {
        // Test pour vérifier que l'erreur 500 ne se produit pas si la catégorie est null
        
        Event eventWithoutCat = new Event();
        eventWithoutCat.setStartTime(LocalDateTime.now().minusMinutes(30));
        eventWithoutCat.setEndTime(LocalDateTime.now());
        eventWithoutCat.setCategory(null); // Catégorie manquante !

        when(eventRepository.findByUser_IdAndStartTimeBetween(any(), any(), any()))
            .thenReturn(Arrays.asList(eventWithoutCat));
            
        when(activityLogRepository.findByUserIdAndStartTimeBetween(any(), any(), any()))
            .thenReturn(Collections.emptyList());

        // ACT
        List<ActivityStatsDTO> stats = activityLogService.getStats(1L, LocalDateTime.now(), LocalDateTime.now());

        // ASSERT
        // Cela ne doit pas lancer d'exception
        // Et l'événement doit être compté dans "AUTRE"
        ActivityStatsDTO autreStat = stats.stream()
            .filter(s -> s.getCategory() == ActivityCategory.AUTRE)
            .findFirst().orElseThrow();
            
        assertEquals(30, autreStat.getTotalMinutes());
    }
}