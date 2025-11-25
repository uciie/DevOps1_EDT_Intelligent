package com.example.backend.controller;

import com.example.backend.model.Event;
import com.example.backend.model.Location;
import com.example.backend.model.TravelTime;
import com.example.backend.model.TravelTime.TransportMode;
import com.example.backend.model.User;
import com.example.backend.repository.TravelTimeRepository; // IMPORT ADDED
import com.example.backend.service.TravelTimeService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.doNothing;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(TravelTimeController.class)
class TravelTimeControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private TravelTimeService travelTimeService;

    // FIX: Add Mock for Repository because the Controller uses it directly for GET requests
    @MockBean
    private TravelTimeRepository travelTimeRepository;

    private User testUser;
    private Event fromEvent;
    private Event toEvent;
    private TravelTime testTravelTime;
    private Location location1;
    private Location location2;

    @BeforeEach
    void setUp() {
        testUser = new User("testuser", "password123");
        testUser.setId(1L);

        location1 = new Location("10 Rue de la Paix, 75002 Paris, France", 48.8692, 2.3312);
        location1.setId(1L);
        location1.setName("Bureau");

        location2 = new Location("5 Avenue Anatole France, 75007 Paris, France", 48.8584, 2.2945);
        location2.setId(2L);
        location2.setName("Tour Eiffel");

        fromEvent = new Event("Départ bureau", LocalDateTime.of(2025, 11, 25, 9, 0), LocalDateTime.of(2025, 11, 25, 10, 0), testUser);
        fromEvent.setId(1L);
        fromEvent.setLocation(location1);

        toEvent = new Event("Réunion client", LocalDateTime.of(2025, 11, 25, 11, 0), LocalDateTime.of(2025, 11, 25, 12, 0), testUser);
        toEvent.setId(2L);
        toEvent.setLocation(location2);

        testTravelTime = new TravelTime(fromEvent, toEvent, testUser, LocalDateTime.of(2025, 11, 25, 10, 0), 25);
        testTravelTime.setId(1L);
        testTravelTime.setDistanceKm(12.5);
        testTravelTime.setMode(TransportMode.DRIVING);
    }

    @Test
    void testGetTravelTimesByUser() throws Exception {
        // Given
        List<TravelTime> travelTimes = Arrays.asList(testTravelTime);
        // FIX: Mock the Repository, not the Service
        when(travelTimeRepository.findByUser_Id(1L)).thenReturn(travelTimes);

        // When & Then
        mockMvc.perform(get("/api/travel-times/user/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(1))
                .andExpect(jsonPath("$[0].durationMinutes").value(25))
                .andExpect(jsonPath("$[0].distanceKm").value(12.5))
                .andExpect(jsonPath("$[0].mode").value("DRIVING"));

        verify(travelTimeRepository, times(1)).findByUser_Id(1L);
    }

    @Test
    void testGetTravelTimesByUserAndDateRange() throws Exception {
        // Given
        List<TravelTime> travelTimes = Arrays.asList(testTravelTime);
        // FIX: Mock the Repository, not the Service
        when(travelTimeRepository.findByUser_IdAndStartTimeBetween(eq(1L), any(LocalDateTime.class), any(LocalDateTime.class)))
            .thenReturn(travelTimes);

        // When & Then
        mockMvc.perform(get("/api/travel-times/user/1/period") // Check URL matches Controller ("/period" vs "/range")
                .param("start", "2025-11-25T00:00:00")
                .param("end", "2025-11-26T00:00:00"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(1))
                .andExpect(jsonPath("$[0].durationMinutes").value(25));
        
        verify(travelTimeRepository, times(1))
            .findByUser_IdAndStartTimeBetween(eq(1L), any(LocalDateTime.class), any(LocalDateTime.class));
    }

    @Test
    void testCalculateTravelTime() throws Exception {
        // Given
        when(travelTimeService.calculateAndCreateTravelTime(1L, 2L, TransportMode.DRIVING))
            .thenReturn(testTravelTime);

        String requestJson = """
            {
                "fromEventId": 1,
                "toEventId": 2,
                "mode": "DRIVING"
            }
            """;

        // When & Then
        mockMvc.perform(post("/api/travel-times/calculate")
                .contentType(MediaType.APPLICATION_JSON)
                .content(requestJson))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.durationMinutes").value(25));

        verify(travelTimeService, times(1))
            .calculateAndCreateTravelTime(1L, 2L, TransportMode.DRIVING);
    }

    @Test
    void testCalculateTravelTimeWithWalkingMode() throws Exception {
        TravelTime walkingTravelTime = new TravelTime(fromEvent, toEvent, testUser, LocalDateTime.of(2025, 11, 25, 10, 0), 150);
        walkingTravelTime.setId(2L);
        walkingTravelTime.setMode(TransportMode.WALKING);

        when(travelTimeService.calculateAndCreateTravelTime(1L, 2L, TransportMode.WALKING))
            .thenReturn(walkingTravelTime);

        String requestJson = "{ \"fromEventId\": 1, \"toEventId\": 2, \"mode\": \"WALKING\" }";

        mockMvc.perform(post("/api/travel-times/calculate")
                .contentType(MediaType.APPLICATION_JSON)
                .content(requestJson))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.mode").value("WALKING"));
    }

    @Test
    void testCalculateTravelTimeWithCyclingMode() throws Exception {
        TravelTime cyclingTravelTime = new TravelTime(fromEvent, toEvent, testUser, LocalDateTime.of(2025, 11, 25, 10, 0), 50);
        cyclingTravelTime.setId(3L);
        cyclingTravelTime.setMode(TransportMode.CYCLING);

        when(travelTimeService.calculateAndCreateTravelTime(1L, 2L, TransportMode.CYCLING))
            .thenReturn(cyclingTravelTime);

        String requestJson = "{ \"fromEventId\": 1, \"toEventId\": 2, \"mode\": \"CYCLING\" }";

        mockMvc.perform(post("/api/travel-times/calculate")
                .contentType(MediaType.APPLICATION_JSON)
                .content(requestJson))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.mode").value("CYCLING"));
    }

    @Test
    void testDeleteTravelTime() throws Exception {
        // Given
        doNothing().when(travelTimeService).deleteTravelTime(1L);

        // When & Then
        mockMvc.perform(delete("/api/travel-times/1"))
                .andExpect(status().isNoContent());

        verify(travelTimeService, times(1)).deleteTravelTime(1L);
    }

    @Test
    void testGetTravelTimesByUserEmpty() throws Exception {
        // Given
        // FIX: Mock the Repository, not the Service
        when(travelTimeRepository.findByUser_Id(99L)).thenReturn(Collections.emptyList());

        // When & Then
        mockMvc.perform(get("/api/travel-times/user/99"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$").isEmpty());
    }
}