package com.example.backend.service;

import com.example.backend.controller.EventController.EventRequest;
import com.example.backend.controller.EventController.LocationRequest;
import com.example.backend.model.Event;
import com.example.backend.model.Location;
import com.example.backend.model.User;
import com.example.backend.repository.EventRepository;
import com.example.backend.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Tests unitaires pour EventService
 */
@ExtendWith(MockitoExtension.class)
class EventServiceTest {

    @Mock
    private EventRepository eventRepository;

    @Mock
    private UserRepository userRepository;

    @InjectMocks
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
    void testGetEventsByUserId() {
        // Given
        Event event2 = new Event(
            "Autre réunion",
            LocalDateTime.of(2025, 11, 25, 14, 0),
            LocalDateTime.of(2025, 11, 25, 15, 0),
            testUser
        );
        event2.setId(2L);

        when(eventRepository.findAll()).thenReturn(Arrays.asList(testEvent, event2));

        // When
        List<Event> result = eventService.getEventsByUserId(1L);

        // Then
        assertThat(result).hasSize(2);
        assertThat(result.get(0).getSummary()).isEqualTo("Réunion client");
        assertThat(result.get(1).getSummary()).isEqualTo("Autre réunion");
        verify(eventRepository, times(1)).findAll();
    }

    @Test
    void testGetEventsByUserIdFiltersCorrectly() {
        // Given
        User otherUser = new User("otheruser", "password");
        otherUser.setId(2L);
        
        Event otherEvent = new Event(
            "Event d'un autre user",
            LocalDateTime.of(2025, 11, 25, 14, 0),
            LocalDateTime.of(2025, 11, 25, 15, 0),
            otherUser
        );

        when(eventRepository.findAll()).thenReturn(Arrays.asList(testEvent, otherEvent));

        // When
        List<Event> result = eventService.getEventsByUserId(1L);

        // Then
        assertThat(result).hasSize(1);
        assertThat(result.get(0).getUser().getId()).isEqualTo(1L);
    }

    @Test
    void testGetEventById() {
        // Given
        when(eventRepository.findById(1L)).thenReturn(Optional.of(testEvent));

        // When
        Event result = eventService.getEventById(1L);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getId()).isEqualTo(1L);
        assertThat(result.getSummary()).isEqualTo("Réunion client");
        verify(eventRepository, times(1)).findById(1L);
    }

    @Test
    void testGetEventByIdNotFound() {
        // Given
        when(eventRepository.findById(999L)).thenReturn(Optional.empty());

        // When & Then
        assertThatThrownBy(() -> eventService.getEventById(999L))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("Event not found");
    }

    @Test
    void testCreateEvent() {
        // Given
        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
        when(eventRepository.save(any(Event.class))).thenReturn(testEvent);

        EventRequest request = new EventRequest();
        request.setSummary("Réunion client");
        request.setStartTime(LocalDateTime.of(2025, 11, 25, 9, 0));
        request.setEndTime(LocalDateTime.of(2025, 11, 25, 10, 0));
        request.setUserId(1L);

        LocationRequest locationRequest = new LocationRequest();
        locationRequest.setAddress("5 Avenue Anatole France, 75007 Paris, France");
        locationRequest.setLatitude(48.8584);
        locationRequest.setLongitude(2.2945);
        locationRequest.setName("Tour Eiffel");
        request.setLocation(locationRequest);

        // When
        Event result = eventService.createEvent(request);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getSummary()).isEqualTo("Réunion client");
        verify(userRepository, times(1)).findById(1L);
        verify(eventRepository, times(1)).save(any(Event.class));
    }

    @Test
    void testCreateEventWithoutLocation() {
        // Given
        Event eventWithoutLocation = new Event(
            "Réunion interne",
            LocalDateTime.of(2025, 11, 25, 9, 0),
            LocalDateTime.of(2025, 11, 25, 10, 0),
            testUser
        );
        eventWithoutLocation.setId(2L);

        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
        when(eventRepository.save(any(Event.class))).thenReturn(eventWithoutLocation);

        EventRequest request = new EventRequest();
        request.setSummary("Réunion interne");
        request.setStartTime(LocalDateTime.of(2025, 11, 25, 9, 0));
        request.setEndTime(LocalDateTime.of(2025, 11, 25, 10, 0));
        request.setUserId(1L);

        // When
        Event result = eventService.createEvent(request);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getSummary()).isEqualTo("Réunion interne");
        verify(eventRepository, times(1)).save(any(Event.class));
    }

    @Test
    void testCreateEventUserNotFound() {
        // Given
        when(userRepository.findById(999L)).thenReturn(Optional.empty());

        EventRequest request = new EventRequest();
        request.setUserId(999L);
        request.setSummary("Test");
        request.setStartTime(LocalDateTime.now());
        request.setEndTime(LocalDateTime.now().plusHours(1));

        // When & Then
        assertThatThrownBy(() -> eventService.createEvent(request))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("User not found");
    }

    @Test
    void testUpdateEvent() {
        // Given
        when(eventRepository.findById(1L)).thenReturn(Optional.of(testEvent));
        when(eventRepository.save(any(Event.class))).thenReturn(testEvent);

        EventRequest request = new EventRequest();
        request.setSummary("Réunion client - URGENT");
        request.setStartTime(LocalDateTime.of(2025, 11, 25, 8, 0));
        request.setEndTime(LocalDateTime.of(2025, 11, 25, 9, 0));

        // When
        Event result = eventService.updateEvent(1L, request);

        // Then
        assertThat(result).isNotNull();
        verify(eventRepository, times(1)).findById(1L);
        verify(eventRepository, times(1)).save(any(Event.class));
    }

    @Test
    void testUpdateEventNotFound() {
        // Given
        when(eventRepository.findById(999L)).thenReturn(Optional.empty());

        EventRequest request = new EventRequest();
        request.setSummary("Test");

        // When & Then
        assertThatThrownBy(() -> eventService.updateEvent(999L, request))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("Event not found");
    }

    @Test
    void testDeleteEvent() {
        // Given
        when(eventRepository.existsById(1L)).thenReturn(true);

        // When
        eventService.deleteEvent(1L);

        // Then
        verify(eventRepository, times(1)).existsById(1L);
        verify(eventRepository, times(1)).deleteById(1L);
    }

    @Test
    void testDeleteEventNotFound() {
        // Given
        when(eventRepository.existsById(999L)).thenReturn(false);

        // When & Then
        assertThatThrownBy(() -> eventService.deleteEvent(999L))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("Event not found");
    }
}