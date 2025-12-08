package com.example.backend.controller;

import com.example.backend.model.Event;
import com.example.backend.model.Location;
import com.example.backend.model.User;
import com.example.backend.service.EventService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.doNothing;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Tests unitaires pour EventController
 */
@WebMvcTest(EventController.class)
class EventControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private EventService eventService;

    private User testUser;
    private Event testEvent;
    private Location testLocation;

    @BeforeEach
    void setUp() {
        testUser = new User("testuser", "password123");
        testUser.setId(1L);

        testLocation = new Location(
            "5 Avenue Anatole France, 75007 Paris, France",
            48.8584,
            2.2945
        );
        testLocation.setId(1L);
        testLocation.setName("Tour Eiffel");

        testEvent = new Event(
            "Réunion client",
            LocalDateTime.of(2025, 11, 25, 9, 0),
            LocalDateTime.of(2025, 11, 25, 10, 0),
            testUser
        );
        testEvent.setId(1L);
        testEvent.setLocation(testLocation);
    }

    @Test
    void testGetEventsByUser() throws Exception {
        // Given
        List<Event> events = Arrays.asList(testEvent);
        when(eventService.getEventsByUserId(1L)).thenReturn(events);

        // When & Then
        mockMvc.perform(get("/api/events/user/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(1))
                .andExpect(jsonPath("$[0].summary").value("Réunion client"))
                .andExpect(jsonPath("$[0].location.name").value("Tour Eiffel"));

        verify(eventService, times(1)).getEventsByUserId(1L);
    }

    @Test
    void testGetEventsByUserAndDateRange() throws Exception {
        // Given
        List<Event> events = Arrays.asList(testEvent);
        
        when(eventService.getEventsByUserIdAndPeriod(eq(1L), any(LocalDateTime.class), any(LocalDateTime.class)))
                .thenReturn(events);

        // When & Then
        mockMvc.perform(get("/api/events/user/1/period")
                .param("start", "2025-11-25T00:00:00")
                .param("end", "2025-11-26T00:00:00"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(1))
                .andExpect(jsonPath("$[0].summary").value("Réunion client"));

        verify(eventService, times(1)).getEventsByUserIdAndPeriod(eq(1L), any(LocalDateTime.class), any(LocalDateTime.class));
    }

    @Test
    void testGetEventById() throws Exception {
        // Given
        when(eventService.getEventById(1L)).thenReturn(testEvent);

        // When & Then
        mockMvc.perform(get("/api/events/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.summary").value("Réunion client"))
                .andExpect(jsonPath("$.location.latitude").value(48.8584));

        verify(eventService, times(1)).getEventById(1L);
    }

    @Test
    void testCreateEvent() throws Exception {
        // Given
        when(eventService.createEvent(any())).thenReturn(testEvent);

        // Mise à jour du JSON pour inclure le nouveau champ optionnel (transportMode) pour test
        String requestJson = """
            {
                "summary": "Réunion client",
                "startTime": "2025-11-25T09:00:00",
                "endTime": "2025-11-25T10:00:00",
                "userId": 1,
                "transportMode": "DRIVING",
                "location": {
                    "address": "5 Avenue Anatole France, 75007 Paris, France",
                    "latitude": 48.8584,
                    "longitude": 2.2945,
                    "name": "Tour Eiffel"
                }
            }
            """;

        // When & Then
        mockMvc.perform(post("/api/events")
                .contentType(MediaType.APPLICATION_JSON)
                .content(requestJson))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.summary").value("Réunion client"));

        verify(eventService, times(1)).createEvent(any());
    }

    @Test
    void testUpdateEvent() throws Exception {
        // Given
        Event updatedEvent = new Event(
            "Réunion client - URGENT",
            LocalDateTime.of(2025, 11, 25, 9, 0),
            LocalDateTime.of(2025, 11, 25, 10, 0),
            testUser
        );
        updatedEvent.setId(1L);
        
        when(eventService.updateEvent(eq(1L), any())).thenReturn(updatedEvent);

        String requestJson = """
            {
                "summary": "Réunion client - URGENT"
            }
            """;

        // When & Then
        mockMvc.perform(put("/api/events/1")
                .contentType(MediaType.APPLICATION_JSON)
                .content(requestJson))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.summary").value("Réunion client - URGENT"));

        verify(eventService, times(1)).updateEvent(eq(1L), any());
    }

    @Test
    void testDeleteEvent() throws Exception {
        // Given
        doNothing().when(eventService).deleteEvent(1L);

        // When & Then
        mockMvc.perform(delete("/api/events/1"))
                .andExpect(status().isOk()); 

        verify(eventService, times(1)).deleteEvent(1L);
    }

    @Test
    void testCreateEventWithoutLocation() throws Exception {
        // Given
        Event eventWithoutLocation = new Event(
            "Réunion interne",
            LocalDateTime.of(2025, 11, 25, 9, 0),
            LocalDateTime.of(2025, 11, 25, 10, 0),
            testUser
        );
        eventWithoutLocation.setId(2L);
        
        when(eventService.createEvent(any())).thenReturn(eventWithoutLocation);

        String requestJson = """
            {
                "summary": "Réunion interne",
                "startTime": "2025-11-25T09:00:00",
                "endTime": "2025-11-25T10:00:00",
                "userId": 1
            }
            """;

        // When & Then
        mockMvc.perform(post("/api/events")
                .contentType(MediaType.APPLICATION_JSON)
                .content(requestJson))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(2))
                .andExpect(jsonPath("$.summary").value("Réunion interne"));

        verify(eventService, times(1)).createEvent(any());
    }
}