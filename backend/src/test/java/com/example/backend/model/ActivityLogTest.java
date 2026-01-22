package com.example.backend.model;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

import java.time.LocalDateTime;

class ActivityLogTest {

    @Test
    void testDefaultConstructor() {
        // Act
        ActivityLog log = new ActivityLog();

        // Assert
        assertNull(log.getId());
        assertNull(log.getUserId());
        assertNull(log.getActivityType());
        assertNull(log.getStartTime());
        assertNull(log.getEndTime());
        assertNull(log.getEventId());
    }

    @Test
    void testGettersAndSetters() {
        // Arrange
        ActivityLog log = new ActivityLog();
        Long id = 1L;
        Long userId = 100L;
        ActivityCategory category = ActivityCategory.TRAVAIL;
        LocalDateTime start = LocalDateTime.of(2025, 1, 15, 10, 0);
        LocalDateTime end = LocalDateTime.of(2025, 1, 15, 11, 0);
        Long eventId = 50L;

        // Act
        log.setId(id);
        log.setUserId(userId);
        log.setActivityType(category);
        log.setStartTime(start);
        log.setEndTime(end);
        log.setEventId(eventId);

        // Assert
        assertEquals(id, log.getId());
        assertEquals(userId, log.getUserId());
        assertEquals(category, log.getActivityType());
        assertEquals(start, log.getStartTime());
        assertEquals(end, log.getEndTime());
        assertEquals(eventId, log.getEventId());
    }

    @Test
    void testAllActivityCategories() {
        // Test avec toutes les catégories
        ActivityLog log = new ActivityLog();
        
        for (ActivityCategory category : ActivityCategory.values()) {
            log.setActivityType(category);
            assertEquals(category, log.getActivityType());
        }
    }

    @Test
    void testEventIdCanBeNull() {
        // Arrange
        ActivityLog log = new ActivityLog();
        log.setUserId(1L);
        log.setActivityType(ActivityCategory.SPORT);
        log.setStartTime(LocalDateTime.now());
        log.setEndTime(LocalDateTime.now().plusHours(1));

        // Act
        log.setEventId(null);

        // Assert
        assertNull(log.getEventId());
    }

    @Test
    void testTimeRange() {
        // Arrange
        ActivityLog log = new ActivityLog();
        LocalDateTime start = LocalDateTime.of(2025, 1, 15, 9, 0);
        LocalDateTime end = LocalDateTime.of(2025, 1, 15, 17, 0);

        // Act
        log.setStartTime(start);
        log.setEndTime(end);

        // Assert
        assertEquals(start, log.getStartTime());
        assertEquals(end, log.getEndTime());
        assertTrue(end.isAfter(start));
    }

    @Test
    void testActivityLogWithEvent() {
        // Arrange
        ActivityLog log = new ActivityLog();
        log.setId(1L);
        log.setUserId(1L);
        log.setActivityType(ActivityCategory.ETUDE);
        log.setEventId(100L);
        log.setStartTime(LocalDateTime.of(2025, 1, 15, 14, 0));
        log.setEndTime(LocalDateTime.of(2025, 1, 15, 16, 0));

        // Assert
        assertNotNull(log.getEventId());
        assertEquals(100L, log.getEventId());
    }

    @Test
    void testActivityLogWithoutEvent() {
        // Arrange
        ActivityLog log = new ActivityLog();
        log.setUserId(1L);
        log.setActivityType(ActivityCategory.LOISIR);
        log.setEventId(null);
        log.setStartTime(LocalDateTime.now());
        log.setEndTime(LocalDateTime.now().plusMinutes(30));

        // Assert
        assertNull(log.getEventId());
    }

    @Test
    void testSetId() {
        // Arrange
        ActivityLog log = new ActivityLog();

        // Act
        log.setId(999L);

        // Assert
        assertEquals(999L, log.getId());
    }

    @Test
    void testSetUserId() {
        // Arrange
        ActivityLog log = new ActivityLog();

        // Act
        log.setUserId(123L);

        // Assert
        assertEquals(123L, log.getUserId());
    }
}
