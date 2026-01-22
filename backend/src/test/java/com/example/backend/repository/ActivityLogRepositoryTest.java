package com.example.backend.repository;

import com.example.backend.model.ActivityCategory;
import com.example.backend.model.ActivityLog;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;

import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@DataJpaTest
class ActivityLogRepositoryTest {

    @Autowired
    private TestEntityManager entityManager;

    @Autowired
    private ActivityLogRepository activityLogRepository;

    @Test
    void testSaveActivityLog() {
        // Arrange
        ActivityLog log = new ActivityLog();
        log.setUserId(1L);
        log.setActivityType(ActivityCategory.TRAVAIL);
        log.setStartTime(LocalDateTime.of(2025, 1, 15, 10, 0));
        log.setEndTime(LocalDateTime.of(2025, 1, 15, 11, 0));

        // Act
        ActivityLog saved = activityLogRepository.save(log);

        // Assert
        assertNotNull(saved.getId());
        assertEquals(1L, saved.getUserId());
        assertEquals(ActivityCategory.TRAVAIL, saved.getActivityType());
    }

    @Test
    void testFindByUserIdAndStartTimeBetween() {
        // Arrange
        LocalDateTime start = LocalDateTime.of(2025, 1, 15, 0, 0);
        LocalDateTime end = LocalDateTime.of(2025, 1, 15, 23, 59);

        ActivityLog log1 = new ActivityLog();
        log1.setUserId(1L);
        log1.setActivityType(ActivityCategory.TRAVAIL);
        log1.setStartTime(LocalDateTime.of(2025, 1, 15, 10, 0));
        log1.setEndTime(LocalDateTime.of(2025, 1, 15, 11, 0));
        entityManager.persistAndFlush(log1);

        ActivityLog log2 = new ActivityLog();
        log2.setUserId(1L);
        log2.setActivityType(ActivityCategory.SPORT);
        log2.setStartTime(LocalDateTime.of(2025, 1, 15, 14, 0));
        log2.setEndTime(LocalDateTime.of(2025, 1, 15, 15, 0));
        entityManager.persistAndFlush(log2);

        // Log en dehors de la période
        ActivityLog log3 = new ActivityLog();
        log3.setUserId(1L);
        log3.setActivityType(ActivityCategory.ETUDE);
        log3.setStartTime(LocalDateTime.of(2025, 1, 16, 10, 0));
        log3.setEndTime(LocalDateTime.of(2025, 1, 16, 11, 0));
        entityManager.persistAndFlush(log3);

        // Act
        List<ActivityLog> results = activityLogRepository.findByUserIdAndStartTimeBetween(1L, start, end);

        // Assert
        assertEquals(2, results.size());
        assertTrue(results.stream().anyMatch(l -> l.getActivityType() == ActivityCategory.TRAVAIL));
        assertTrue(results.stream().anyMatch(l -> l.getActivityType() == ActivityCategory.SPORT));
        assertFalse(results.stream().anyMatch(l -> l.getActivityType() == ActivityCategory.ETUDE));
    }

    @Test
    void testFindByUserIdAndStartTimeBetween_EmptyResult() {
        // Arrange
        LocalDateTime start = LocalDateTime.of(2025, 1, 15, 0, 0);
        LocalDateTime end = LocalDateTime.of(2025, 1, 15, 23, 59);

        // Act
        List<ActivityLog> results = activityLogRepository.findByUserIdAndStartTimeBetween(999L, start, end);

        // Assert
        assertTrue(results.isEmpty());
    }

    @Test
    void testFindByUserIdAndStartTimeBetween_DifferentUsers() {
        // Arrange
        LocalDateTime start = LocalDateTime.of(2025, 1, 15, 0, 0);
        LocalDateTime end = LocalDateTime.of(2025, 1, 15, 23, 59);

        ActivityLog log1 = new ActivityLog();
        log1.setUserId(1L);
        log1.setActivityType(ActivityCategory.TRAVAIL);
        log1.setStartTime(LocalDateTime.of(2025, 1, 15, 10, 0));
        log1.setEndTime(LocalDateTime.of(2025, 1, 15, 11, 0));
        entityManager.persistAndFlush(log1);

        ActivityLog log2 = new ActivityLog();
        log2.setUserId(2L);
        log2.setActivityType(ActivityCategory.SPORT);
        log2.setStartTime(LocalDateTime.of(2025, 1, 15, 14, 0));
        log2.setEndTime(LocalDateTime.of(2025, 1, 15, 15, 0));
        entityManager.persistAndFlush(log2);

        // Act
        List<ActivityLog> results = activityLogRepository.findByUserIdAndStartTimeBetween(1L, start, end);

        // Assert
        assertEquals(1, results.size());
        assertEquals(1L, results.get(0).getUserId());
    }

    @Test
    void testActivityLogWithEventId() {
        // Arrange
        ActivityLog log = new ActivityLog();
        log.setUserId(1L);
        log.setActivityType(ActivityCategory.ETUDE);
        log.setEventId(100L);
        log.setStartTime(LocalDateTime.now());
        log.setEndTime(LocalDateTime.now().plusHours(1));

        // Act
        ActivityLog saved = activityLogRepository.save(log);

        // Assert
        assertNotNull(saved.getId());
        assertEquals(100L, saved.getEventId());
    }

    @Test
    void testDeleteActivityLog() {
        // Arrange
        ActivityLog log = new ActivityLog();
        log.setUserId(1L);
        log.setActivityType(ActivityCategory.LOISIR);
        log.setStartTime(LocalDateTime.now());
        log.setEndTime(LocalDateTime.now().plusMinutes(30));
        ActivityLog saved = activityLogRepository.save(log);
        Long id = saved.getId();

        // Act
        activityLogRepository.deleteById(id);

        // Assert
        assertFalse(activityLogRepository.existsById(id));
    }
}
