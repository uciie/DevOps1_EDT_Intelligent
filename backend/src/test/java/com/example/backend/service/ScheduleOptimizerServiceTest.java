package com.example.backend.service;

import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import static org.mockito.Mockito.*;

/**
 * Tests unitaires pour l'interface ScheduleOptimizerService.
 * Teste les implémentations mock de l'interface.
 */
class ScheduleOptimizerServiceTest {

    @Test
    void testReshuffleMethodExists() {
        // Arrange
        ScheduleOptimizerService mockService = Mockito.mock(ScheduleOptimizerService.class);
        Long eventId = 1L;

        // Act
        doNothing().when(mockService).reshuffle(eventId);
        mockService.reshuffle(eventId);

        // Assert
        verify(mockService, times(1)).reshuffle(eventId);
    }

    @Test
    void testReshuffleWithDifferentEventIds() {
        // Arrange
        ScheduleOptimizerService mockService = Mockito.mock(ScheduleOptimizerService.class);

        // Act
        mockService.reshuffle(1L);
        mockService.reshuffle(2L);
        mockService.reshuffle(100L);

        // Assert
        verify(mockService, times(1)).reshuffle(1L);
        verify(mockService, times(1)).reshuffle(2L);
        verify(mockService, times(1)).reshuffle(100L);
        verify(mockService, times(3)).reshuffle(anyLong());
    }

    @Test
    void testReshuffleNeverCalled() {
        // Arrange
        ScheduleOptimizerService mockService = Mockito.mock(ScheduleOptimizerService.class);

        // Assert
        verify(mockService, never()).reshuffle(anyLong());
    }

    @Test
    void testReshuffleMultipleTimes() {
        // Arrange
        ScheduleOptimizerService mockService = Mockito.mock(ScheduleOptimizerService.class);
        Long eventId = 5L;

        // Act
        mockService.reshuffle(eventId);
        mockService.reshuffle(eventId);
        mockService.reshuffle(eventId);

        // Assert
        verify(mockService, times(3)).reshuffle(eventId);
    }
}