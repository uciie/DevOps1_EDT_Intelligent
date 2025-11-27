package com.example.backend.controller;

import com.example.backend.service.ScheduleOptimizerService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.http.ResponseEntity;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * Tests unitaires pour ScheduleController.
 */
class ScheduleControllerTest {

    @Mock
    private ScheduleOptimizerService optimizerService;

    private ScheduleController controller;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        controller = new ScheduleController(optimizerService);
    }

    @Test
    void testReshuffle_Success() {
        // Arrange
        Long eventId = 1L;
        doNothing().when(optimizerService).reshuffle(eventId);

        // Act
        ResponseEntity<String> response = controller.reshuffle(eventId);

        // Assert
        assertEquals(200, response.getStatusCode().value());
        assertEquals("Schedule updated successfully.", response.getBody());
        verify(optimizerService, times(1)).reshuffle(eventId);
    }

    @Test
    void testReshuffle_WithDifferentEventId() {
        // Arrange
        Long eventId = 999L;
        doNothing().when(optimizerService).reshuffle(eventId);

        // Act
        ResponseEntity<String> response = controller.reshuffle(eventId);

        // Assert
        assertEquals(200, response.getStatusCode().value());
        assertNotNull(response.getBody());
        verify(optimizerService, times(1)).reshuffle(eventId);
    }
}