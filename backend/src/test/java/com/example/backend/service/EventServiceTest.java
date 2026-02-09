package com.example.backend.service;

import com.example.backend.controller.EventController.EventRequest;
import com.example.backend.controller.EventController.LocationRequest;
import com.example.backend.model.Event;
import com.example.backend.model.Location;
import com.example.backend.model.Team;
import com.example.backend.model.TravelTime;
import com.example.backend.model.User;
import com.example.backend.repository.EventRepository;
import com.example.backend.repository.TeamRepository;
import com.example.backend.repository.TravelTimeRepository;
import com.example.backend.repository.UserRepository;
import com.example.backend.service.TravelTimeCalculator;
import com.example.backend.service.TravelTimeService;
import com.example.backend.service.impl.EventServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class EventServiceTest {

    @Mock
    private EventRepository eventRepository;

    @Mock
    private UserRepository userRepository;

    // --- CORRECTION : Ajout du mock manquant pour le constructeur ---
    @Mock
    private TravelTimeService travelTimeService;

    @InjectMocks
    private EventServiceImpl eventService;

    private User user;
    private Event event;
    private EventRequest eventRequest;

    @BeforeEach
    void setUp() {
        user = new User();
        user.setId(1L);

        event = new Event("Réunion", LocalDateTime.now(), LocalDateTime.now().plusHours(1), user);
        event.setId(1L);

        eventRequest = new EventRequest();
        eventRequest.setSummary("Réunion");
        eventRequest.setStartTime(LocalDateTime.now());
        eventRequest.setEndTime(LocalDateTime.now().plusHours(1));
        eventRequest.setUserId(1L);
        // Par défaut, transportMode est null, donc la logique de trajet ne s'active pas dans les tests existants
    }

    @Test
    void testCreateEvent() {
        // GIVEN
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(eventRepository.save(any(Event.class))).thenReturn(event);

        // WHEN
        Event created = eventService.createEvent(eventRequest);

        // THEN
        assertNotNull(created);
        assertEquals("Réunion", created.getSummary());
        verify(eventRepository).save(any(Event.class));
    }

    @Test
    void testCreateEventUserNotFound() {
        // GIVEN
        when(userRepository.findById(99L)).thenReturn(Optional.empty());
        eventRequest.setUserId(99L);

        // WHEN / THEN
        assertThrows(IllegalArgumentException.class, () -> eventService.createEvent(eventRequest));
    }

    @Test
    void testCreateEventWithoutLocation() {
        // GIVEN
        eventRequest.setLocation(null); // Pas de location
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(eventRepository.save(any(Event.class))).thenReturn(event);

        // WHEN
        Event created = eventService.createEvent(eventRequest);

        // THEN
        assertNotNull(created);
        assertNull(created.getLocation()); // Vérifie que c'est null
    }
    
    @Test
    void testCreateEventWithLocation() {
        // GIVEN
        LocationRequest locReq = new LocationRequest();
        locReq.setAddress("1 rue rivoli, 75001 Paris, France");
        eventRequest.setLocation(locReq);
        
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        // On simule le save qui retourne l'event avec la location
        Event eventWithLoc = new Event();
        eventWithLoc.setLocation(new Location("1 rue rivoli, 75001 Paris, France"));
        when(eventRepository.save(any(Event.class))).thenReturn(eventWithLoc);

        // WHEN
        Event created = eventService.createEvent(eventRequest);

        // THEN
        assertNotNull(created.getLocation());
    }

    @Test
    void testDeleteEvent() {
        // GIVEN
        when(eventRepository.existsById(1L)).thenReturn(true);
        when(eventRepository.findById(1L)).thenReturn(Optional.of(event));

        // WHEN
        eventService.deleteEvent(1L);

        // THEN
        verify(eventRepository).deleteById(1L);
    }

    @Test
    void testDeleteEventNotFound() {
        // GIVEN
        when(eventRepository.existsById(1L)).thenReturn(false);

        // WHEN / THEN
        assertThrows(IllegalArgumentException.class, () -> eventService.deleteEvent(1L));
        verify(eventRepository, never()).deleteById(anyLong());
    }

    @Test
    void testGetEventById() {
        // GIVEN
        when(eventRepository.findById(1L)).thenReturn(Optional.of(event));

        // WHEN
        Event found = eventService.getEventById(1L);

        // THEN
        assertEquals(event.getId(), found.getId());
    }

    @Test
    void testGetEventByIdNotFound() {
        // GIVEN
        when(eventRepository.findById(1L)).thenReturn(Optional.empty());

        // WHEN / THEN
        assertThrows(IllegalArgumentException.class, () -> eventService.getEventById(1L));
    }

    @Test
    void testGetEventsByUserId() {
        // GIVEN
        when(eventRepository.findByUser_IdOrderByStartTime(1L)).thenReturn(Arrays.asList(event));

        // WHEN
        List<Event> events = eventService.getEventsByUserId(1L);

        // THEN
        assertFalse(events.isEmpty());
        assertEquals(1, events.size());
    }
    
    @Test
    void testUpdateEvent() {
        // GIVEN
        when(eventRepository.findById(1L)).thenReturn(Optional.of(event));
        when(eventRepository.save(any(Event.class))).thenReturn(event);
        
        EventRequest updateReq = new EventRequest();
        updateReq.setSummary("Updated Summary");

        // WHEN
        Event updated = eventService.updateEvent(1L, updateReq);

        // THEN
        assertEquals("Updated Summary", updated.getSummary());
        verify(eventRepository).save(event);
    }
    
    @Test
    void testUpdateEventNotFound() {
        // GIVEN
        when(eventRepository.findById(1L)).thenReturn(Optional.empty());
        
        // WHEN / THEN
        assertThrows(IllegalArgumentException.class, () -> eventService.updateEvent(1L, eventRequest));
    }
}