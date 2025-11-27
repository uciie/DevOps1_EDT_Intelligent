package com.example.backend.service.impl;

import com.example.backend.model.Event;
import com.example.backend.model.Location;
import com.example.backend.model.Task;
import com.example.backend.model.TravelTime;
import com.example.backend.model.TravelTime.TransportMode;
import com.example.backend.model.User;
import com.example.backend.repository.EventRepository;
import com.example.backend.repository.TaskRepository;
import com.example.backend.service.TravelTimeService;
import com.example.backend.service.strategy.TaskSelectionStrategy;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class DefaultScheduleOptimizerServiceTest {

    @Mock
    private EventRepository eventRepository;
    @Mock
    private TaskRepository taskRepository;
    @Mock
    private TaskSelectionStrategy taskSelectionStrategy;
    @Mock
    private TravelTimeService travelTimeService;

    private DefaultScheduleOptimizerService optimizerService;

    private Event cancelledEvent;
    private User user;

    @BeforeEach
    void setUp() {
        // Initialisation manuelle
        optimizerService = new DefaultScheduleOptimizerService(
            eventRepository,
            taskRepository,
            taskSelectionStrategy,
            travelTimeService
        );

        user = new User("testuser", "pwd");
        user.setId(1L);

        // Event de 10h à 11h (60 min de libre)
        cancelledEvent = new Event("Cancelled Meeting", 
                LocalDateTime.of(2025, 1, 1, 10, 0),
                LocalDateTime.of(2025, 1, 1, 11, 0), 
                user);
        cancelledEvent.setId(100L);
        
        // CORRECTION : Utilisation d'une adresse valide (> 10 caractères, avec virgule)
        cancelledEvent.setLocation(new Location("10 Rue de la Paix, 75002 Paris, France"));
        
        // Configuration par défaut
        lenient().when(eventRepository.findAll()).thenReturn(Collections.emptyList());
    }

    @Test
    void testReshuffle_EventNotFound() {
        when(eventRepository.findById(100L)).thenReturn(Optional.empty());

        assertThrows(IllegalArgumentException.class, () -> optimizerService.reshuffle(100L));
    }

    @Test
    void testReshuffle_NoTaskSelected() {
        when(eventRepository.findById(100L)).thenReturn(Optional.of(cancelledEvent));
        when(taskSelectionStrategy.selectTask(eq(1L), anyLong())).thenReturn(null);

        optimizerService.reshuffle(100L);

        assertEquals("CANCELLED", cancelledEvent.getStatus());
        verify(eventRepository).save(cancelledEvent);

        ArgumentCaptor<Event> eventCaptor = ArgumentCaptor.forClass(Event.class);
        verify(eventRepository, atLeastOnce()).save(eventCaptor.capture());
        
        boolean createdNewEvent = eventCaptor.getAllValues().stream()
                .anyMatch(e -> !e.getId().equals(100L));
        
        assertFalse(createdNewEvent, "Aucun nouvel événement n'aurait dû être créé");
    }

    @Test
    void testReshuffle_WithTaskAndNoTravelNeeded() {
        when(eventRepository.findById(100L)).thenReturn(Optional.of(cancelledEvent));
        
        Task task = new Task("New Task", 45, 1, false, user, (LocalDateTime) null);
        when(taskSelectionStrategy.selectTask(eq(1L), anyLong())).thenReturn(task);

        optimizerService.reshuffle(100L);

        assertEquals("CANCELLED", cancelledEvent.getStatus());
        
        ArgumentCaptor<Event> eventCaptor = ArgumentCaptor.forClass(Event.class);
        verify(eventRepository, atLeast(2)).save(eventCaptor.capture());
        
        Optional<Event> newEvent = eventCaptor.getAllValues().stream()
                .filter(e -> "New Task".equals(e.getSummary()))
                .findFirst();
        
        assertTrue(newEvent.isPresent(), "Le nouvel événement 'New Task' aurait dû être sauvegardé");
    }

    @Test
    void testReshuffle_WithTravelTime() {
        // Given
        Event nextEvent = new Event("Next Event", 
                LocalDateTime.of(2025, 1, 1, 12, 0), 
                LocalDateTime.of(2025, 1, 1, 13, 0), user);
        
        // Adresse valide pour le validateur
        nextEvent.setLocation(new Location("Place Bellecour, 69002 Lyon, France"));

        when(eventRepository.findById(100L)).thenReturn(Optional.of(cancelledEvent));
        when(eventRepository.findAll()).thenReturn(List.of(cancelledEvent, nextEvent));
        
        TravelTime mockTravelTime = new TravelTime();
        mockTravelTime.setDurationMinutes(15);
        // Important : Mock doit répondre à n'importe quel appel
        when(travelTimeService.createTravelTime(any(), any(), any())).thenReturn(mockTravelTime);

        Task task = new Task("Quick Task", 30, 1, false, user, (LocalDateTime) null);
        when(taskSelectionStrategy.selectTask(eq(1L), anyLong())).thenReturn(task);

        // When
        optimizerService.reshuffle(100L);

        // Then
        // CORRECTION ICI : on attend 2 appels (1 pour l'estimation, 1 pour la création finale)
        verify(travelTimeService, times(2)).createTravelTime(any(), eq(nextEvent), eq(TransportMode.DRIVING));
        
        ArgumentCaptor<Event> eventCaptor = ArgumentCaptor.forClass(Event.class);
        verify(eventRepository, atLeastOnce()).save(eventCaptor.capture());
        
        boolean taskSaved = eventCaptor.getAllValues().stream()
                .anyMatch(e -> "Quick Task".equals(e.getSummary()));
        assertTrue(taskSaved, "L'événement 'Quick Task' aurait dû être sauvegardé");
    }
}