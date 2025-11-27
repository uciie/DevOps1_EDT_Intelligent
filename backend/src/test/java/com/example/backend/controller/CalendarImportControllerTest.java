package com.example.backend.controller;

import com.example.backend.model.Event;
import com.example.backend.model.User;
import com.example.backend.service.CalendarImportService;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.web.MockMultipartFile;

import java.util.List;

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

    // User test commun à tous les tests
    private User testUser;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        controller = new CalendarImportController(importService);

        testUser = new User("testUser", "pwd");
        testUser.setId(1L); // optionnel mais souvent utile
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

        when(importService.importCalendar(any(), any()))
                .thenReturn(List.of(new Event(), new Event(), new Event(), new Event(), new Event()));

        // Act
        ResponseEntity<String> response = controller.importCalendar(file, testUser);

        // Assert
        assertEquals(200, response.getStatusCode().value());
        assertEquals("5 événements importés avec succès.", response.getBody());
        verify(importService).importCalendar(file, testUser);
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

        when(importService.importCalendar(any(), any()))
                .thenThrow(new RuntimeException("Invalid file format"));

        // Act
        ResponseEntity<String> response = controller.importCalendar(file, testUser);

        // Assert
        assertEquals(400, response.getStatusCode().value());
        assertTrue(response.getBody().contains("Erreur"));
        assertTrue(response.getBody().contains("Invalid file format"));
        verify(importService).importCalendar(file, testUser);
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

        when(importService.importCalendar(any(), any()))
                .thenReturn(List.of()); // zéro événement

        // Act
        ResponseEntity<String> response = controller.importCalendar(file, testUser);

        // Assert
        assertEquals(200, response.getStatusCode().value());
        assertEquals("0 événements importés avec succès.", response.getBody());
        verify(importService).importCalendar(file, testUser);
    }
}