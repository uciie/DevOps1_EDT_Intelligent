package com.example.backend.controller;

import com.example.backend.service.CalendarImportService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.web.MockMultipartFile;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Tests unitaires pour CalendarImportController.
 */
class CalendarImportControllerTest {

    @Mock
    private CalendarImportService importService;

    private CalendarImportController controller;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        controller = new CalendarImportController(importService);
    }

    @Test
    void testImportCalendar_Success() throws Exception {
        // Arrange
        MockMultipartFile file = new MockMultipartFile(
            "file",
            "calendar.ics",
            "text/calendar",
            "test content".getBytes()
        );
        
        when(importService.importCalendar(any())).thenReturn(5);

        // Act
        ResponseEntity<String> response = controller.importCalendar(file);

        // Assert
        assertEquals(200, response.getStatusCode().value());
        assertEquals("5 événements importés avec succès.", response.getBody());
        verify(importService, times(1)).importCalendar(file);
    }

    @Test
    void testImportCalendar_Failure() throws Exception {
        // Arrange
        MockMultipartFile file = new MockMultipartFile(
            "file",
            "calendar.ics",
            "text/calendar",
            "test content".getBytes()
        );
        
        when(importService.importCalendar(any()))
            .thenThrow(new RuntimeException("Invalid file format"));

        // Act
        ResponseEntity<String> response = controller.importCalendar(file);

        // Assert
        assertEquals(400, response.getStatusCode().value());
        assertTrue(response.getBody().contains("Erreur"));
        assertTrue(response.getBody().contains("Invalid file format"));
        verify(importService, times(1)).importCalendar(file);
    }

    @Test
    void testImportCalendar_NoEvents() throws Exception {
        // Arrange
        MockMultipartFile file = new MockMultipartFile(
            "file",
            "empty.ics",
            "text/calendar",
            "empty".getBytes()
        );
        
        when(importService.importCalendar(any())).thenReturn(0);

        // Act
        ResponseEntity<String> response = controller.importCalendar(file);

        // Assert
        assertEquals(200, response.getStatusCode().value());
        assertEquals("0 événements importés avec succès.", response.getBody());
    }
}