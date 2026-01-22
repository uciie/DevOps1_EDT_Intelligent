package com.example.backend.dto;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

import java.time.LocalDateTime;
import java.time.Duration;

class TimeSlotTest {

    @Test
    void testTimeSlotCreation() {
        // Arrange
        LocalDateTime start = LocalDateTime.of(2025, 1, 15, 10, 0);
        LocalDateTime end = LocalDateTime.of(2025, 1, 15, 11, 30);

        // Act
        TimeSlot timeSlot = new TimeSlot(start, end);

        // Assert
        assertEquals(start, timeSlot.start());
        assertEquals(end, timeSlot.end());
    }

    @Test
    void testGetDurationMinutes() {
        // Arrange
        LocalDateTime start = LocalDateTime.of(2025, 1, 15, 10, 0);
        LocalDateTime end = LocalDateTime.of(2025, 1, 15, 11, 30);
        TimeSlot timeSlot = new TimeSlot(start, end);

        // Act
        long durationMinutes = timeSlot.getDurationMinutes();

        // Assert
        assertEquals(90, durationMinutes); // 1h30 = 90 minutes
    }

    @Test
    void testGetDurationMinutes_OneHour() {
        // Arrange
        LocalDateTime start = LocalDateTime.of(2025, 1, 15, 14, 0);
        LocalDateTime end = LocalDateTime.of(2025, 1, 15, 15, 0);
        TimeSlot timeSlot = new TimeSlot(start, end);

        // Act
        long durationMinutes = timeSlot.getDurationMinutes();

        // Assert
        assertEquals(60, durationMinutes);
    }

    @Test
    void testGetDurationMinutes_ShortDuration() {
        // Arrange
        LocalDateTime start = LocalDateTime.of(2025, 1, 15, 10, 0);
        LocalDateTime end = LocalDateTime.of(2025, 1, 15, 10, 15);
        TimeSlot timeSlot = new TimeSlot(start, end);

        // Act
        long durationMinutes = timeSlot.getDurationMinutes();

        // Assert
        assertEquals(15, durationMinutes);
    }

    @Test
    void testGetDurationMinutes_LongDuration() {
        // Arrange
        LocalDateTime start = LocalDateTime.of(2025, 1, 15, 9, 0);
        LocalDateTime end = LocalDateTime.of(2025, 1, 15, 17, 0);
        TimeSlot timeSlot = new TimeSlot(start, end);

        // Act
        long durationMinutes = timeSlot.getDurationMinutes();

        // Assert
        assertEquals(480, durationMinutes); // 8 heures = 480 minutes
    }

    @Test
    void testGetDurationMinutes_CrossDay() {
        // Arrange
        LocalDateTime start = LocalDateTime.of(2025, 1, 15, 23, 0);
        LocalDateTime end = LocalDateTime.of(2025, 1, 16, 1, 0);
        TimeSlot timeSlot = new TimeSlot(start, end);

        // Act
        long durationMinutes = timeSlot.getDurationMinutes();

        // Assert
        assertEquals(120, durationMinutes); // 2 heures
    }

    @Test
    void testTimeSlotImmutability() {
        // Arrange
        LocalDateTime start = LocalDateTime.of(2025, 1, 15, 10, 0);
        LocalDateTime end = LocalDateTime.of(2025, 1, 15, 11, 0);
        TimeSlot timeSlot = new TimeSlot(start, end);

        // Act - Les records sont immutables, donc on ne peut pas modifier
        // On vérifie que les valeurs restent inchangées
        LocalDateTime newStart = LocalDateTime.of(2025, 1, 15, 12, 0);

        // Assert - Les valeurs originales doivent être préservées
        assertEquals(start, timeSlot.start());
        assertEquals(end, timeSlot.end());
        assertNotEquals(newStart, timeSlot.start());
    }

    @Test
    void testTimeSlotEquality() {
        // Arrange
        LocalDateTime start = LocalDateTime.of(2025, 1, 15, 10, 0);
        LocalDateTime end = LocalDateTime.of(2025, 1, 15, 11, 0);
        TimeSlot timeSlot1 = new TimeSlot(start, end);
        TimeSlot timeSlot2 = new TimeSlot(start, end);

        // Assert - Les records ont une égalité basée sur les valeurs
        assertEquals(timeSlot1, timeSlot2);
        assertEquals(timeSlot1.hashCode(), timeSlot2.hashCode());
    }
}
