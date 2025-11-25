package com.example.backend.service.impl;

import com.example.backend.model.Event;
import com.example.backend.model.Task;
import com.example.backend.model.User;
import com.example.backend.repository.EventRepository;
import com.example.backend.repository.TaskRepository;
import com.example.backend.service.strategy.TaskSelectionStrategy;
import com.example.backend.service.TravelTimeService;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.*;

/**
 * Tests unitaires pour DefaultScheduleOptimizerService.
 */
class DefaultScheduleOptimizerServiceTest {

    @Mock
    private EventRepository eventRepository;

    @Mock
    private TaskRepository taskRepository;

    @Mock
    private TaskSelectionStrategy taskSelectionStrategy;

    @Mock
    private TravelTimeService travelTimeService;

    private DefaultScheduleOptimizerService service;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        service = new DefaultScheduleOptimizerService(
            eventRepository, 
            taskRepository, 
            taskSelectionStrategy,
            travelTimeService
        );
    }

    @Test
    void testReshuffle_WithAvailableTask() {
        // Arrange
        User user = new User("alice", "password");
        user.setId(1L);

        Event cancelledEvent = new Event(
            "Cancelled Meeting",
            LocalDateTime.of(2025, 1, 15, 10, 0),
            LocalDateTime.of(2025, 1, 15, 11, 0),
            user
        );
        cancelledEvent.setStatus("PLANNED");

        Task availableTask = new Task(
            "Important Task",
            45,
            5,
            false,
            user,
            null
        );

        when(eventRepository.findById(1L)).thenReturn(Optional.of(cancelledEvent));
        when(taskSelectionStrategy.selectTask(anyLong(), anyLong())).thenReturn(availableTask);
        when(eventRepository.save(any(Event.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // Act
        service.reshuffle(1L);

        // Assert
        verify(eventRepository, times(2)).save(any(Event.class));
        verify(taskSelectionStrategy, times(1)).selectTask(user.getId(), 60L);
        assertEquals("CANCELLED", cancelledEvent.getStatus());
    }

    @Test
    void testReshuffle_NoAvailableTask() {
        // Arrange
        User user = new User("bob", "password");
        user.setId(2L);

        Event cancelledEvent = new Event(
            "Cancelled Event",
            LocalDateTime.of(2025, 1, 15, 14, 0),
            LocalDateTime.of(2025, 1, 15, 15, 30),
            user
        );

        when(eventRepository.findById(2L)).thenReturn(Optional.of(cancelledEvent));
        when(taskSelectionStrategy.selectTask(anyLong(), anyLong())).thenReturn(null);
        when(eventRepository.save(any(Event.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // Act
        service.reshuffle(2L);

        // Assert
        verify(eventRepository, times(1)).save(cancelledEvent);
        verify(taskSelectionStrategy, times(1)).selectTask(user.getId(), 90L);
        assertEquals("CANCELLED", cancelledEvent.getStatus());
    }

    @Test
    void testReshuffle_EventNotFound() {
        // Arrange
        when(eventRepository.findById(999L)).thenReturn(Optional.empty());

        // Act & Assert
        assertThrows(IllegalArgumentException.class, () -> {
            service.reshuffle(999L);
        });

        verify(eventRepository, times(1)).findById(999L);
        verify(eventRepository, never()).save(any());
    }
}