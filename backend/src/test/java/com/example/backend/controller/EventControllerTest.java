package com.example.backend.controller;

import com.example.backend.controller.EventController.EventRequest;
import com.example.backend.model.Event;
import com.example.backend.service.EventService;
import com.example.backend.service.impl.FocusService;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.Arrays;

import static org.mockito.ArgumentMatchers.any;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(EventController.class)
public class EventControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private EventService eventService;

    @MockitoBean
    private FocusService focusService;

    private ObjectMapper objectMapper;

    private Event testEvent;
    private EventRequest eventRequest;

    @BeforeEach
    void setUp() {
        // Configuration de l'ObjectMapper pour gérer les dates Java 8
        objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule());

        testEvent = new Event();
        testEvent.setId(1L);
        testEvent.setSummary("Réunion");
        testEvent.setStartTime(LocalDateTime.of(2026, 1, 13, 10, 0));
        testEvent.setEndTime(LocalDateTime.of(2026, 1, 13, 11, 0));

        eventRequest = new EventRequest();
        eventRequest.setSummary("Réunion");
        eventRequest.setUserId(1L);
        eventRequest.setStartTime(testEvent.getStartTime());
        eventRequest.setEndTime(testEvent.getEndTime());
    }

    /**
     * Test de création d'un événement.
     */
    @Test
    void testCreateEvent() throws Exception {
        Mockito.when(eventService.createEvent(any(EventRequest.class))).thenReturn(testEvent);

        mockMvc.perform(post("/api/events")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(eventRequest)))
                .andExpect(status().isOk()) // Attend 200
                .andExpect(jsonPath("$.summary").value("Réunion"));
    }

    /**
     * Test de suppression d'un événement.
     */
    @Test
    void testDeleteEvent() throws Exception {
        Mockito.doNothing().when(eventService).deleteEvent(1L);

        mockMvc.perform(delete("/api/events/1"))
                .andExpect(status().isOk()); // Attend 200
    }

    @Test
    void testGetEvents() throws Exception {
        Mockito.when(eventService.getEventsByUserId(1L)).thenReturn(Arrays.asList(testEvent));

        mockMvc.perform(get("/api/events/user/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].summary").value("Réunion"));
    }

    @Test
    void testGetEventById() throws Exception {
        Mockito.when(eventService.getEventById(1L)).thenReturn(testEvent);

        mockMvc.perform(get("/api/events/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.summary").value("Réunion"));
    }
}