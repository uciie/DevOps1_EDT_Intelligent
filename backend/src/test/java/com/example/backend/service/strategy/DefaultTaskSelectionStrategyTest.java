package com.example.backend.service.strategy;

import com.example.backend.model.Event;
import com.example.backend.model.Task;
import com.example.backend.model.User;
import com.example.backend.repository.TaskRepository;
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

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class DefaultTaskSelectionStrategyTest {

    @Mock
    private TaskRepository taskRepository;

    @InjectMocks
    private DefaultTaskSelectionStrategy strategy;

    private User user;
    private Task task1;
    private Task task2;
    private Task task3;

    @BeforeEach
    void setUp() {
        user = new User("testuser", "password");
        user.setId(1L);

        // Tâche avec priorité 3, durée 30 min, non terminée
        task1 = new Task("Task 1", 30, 3, Task.TaskStatus.PENDING_CREATION, user, (LocalDateTime) null);
        task1.setId(1L);

        // Tâche avec priorité 2, durée 60 min, non terminée
        task2 = new Task("Task 2", 60, 2, Task.TaskStatus.PENDING_CREATION, user, (LocalDateTime) null);
        task2.setId(2L);

        // Tâche avec priorité 1, durée 15 min, non terminée
        task3 = new Task("Task 3", 15, 1, Task.TaskStatus.PENDING_CREATION, user, (LocalDateTime) null);
        task3.setId(3L);
    }

    @Test
    void testSelectTask_SelectsHighestPriority() {
        // Arrange
        List<Task> tasks = Arrays.asList(task1, task2, task3);
        when(taskRepository.findByUser_Id(1L)).thenReturn(tasks);

        // Act
        Task selected = strategy.selectTask(1L, 120);

        // Assert
        assertNotNull(selected);
        assertEquals(task1.getId(), selected.getId()); // Priorité 3 (la plus haute)
        assertEquals(3, selected.getPriority());
    }

    @Test
    void testSelectTask_FiltersByAvailableTime() {
        // Arrange
        List<Task> tasks = Arrays.asList(task1, task2, task3);
        when(taskRepository.findByUser_Id(1L)).thenReturn(tasks);

        // Act - Seulement 45 minutes disponibles
        Task selected = strategy.selectTask(1L, 45);

        // Assert
        assertNotNull(selected);
        // Doit sélectionner task1 (30 min) ou task3 (15 min), mais pas task2 (60 min)
        assertTrue(selected.getEstimatedDuration() <= 45);
        assertNotEquals(task2.getId(), selected.getId());
    }

    @Test
    void testSelectTask_FiltersDoneTasks() {
        // Arrange
        task1.setStatus(Task.TaskStatus.DONE);
        List<Task> tasks = Arrays.asList(task1, task2, task3);
        when(taskRepository.findByUser_Id(1L)).thenReturn(tasks);

        // Act
        Task selected = strategy.selectTask(1L, 120);

        // Assert
        assertNotNull(selected);
        assertNotEquals(task1.getId(), selected.getId()); // task1 est terminée
        assertNotEquals(Task.TaskStatus.DONE, selected.getStatus());
    }

    @Test
    void testSelectTask_FiltersTasksWithCancelledEvent() {
        // Arrange
        Event cancelledEvent = new Event();
        cancelledEvent.setStatus(Event.EventStatus.PENDING_DELETION);
        task1.setEvent(cancelledEvent);

        List<Task> tasks = Arrays.asList(task1, task2, task3);
        when(taskRepository.findByUser_Id(1L)).thenReturn(tasks);

        // Act
        Task selected = strategy.selectTask(1L, 120);

        // Assert
        assertNotNull(selected);
        assertNotEquals(task1.getId(), selected.getId()); // task1 a un événement annulé
    }

    @Test
    void testSelectTask_FiltersTasksWithDoneEvent() {
        // Arrange
        Event doneEvent = new Event();
        doneEvent.setStatus(Event.EventStatus.CONFIRMED);
        task1.setEvent(doneEvent);

        List<Task> tasks = Arrays.asList(task1, task2, task3);
        when(taskRepository.findByUser_Id(1L)).thenReturn(tasks);

        // Act
        Task selected = strategy.selectTask(1L, 120);

        // Assert
        assertNotNull(selected);
        assertNotEquals(task1.getId(), selected.getId()); // task1 a un événement terminé
    }

    @Test
    void testSelectTask_AllowsTasksWithNullEvent() {
        // Arrange
        // task1, task2, task3 ont tous event = null
        List<Task> tasks = Arrays.asList(task1, task2, task3);
        when(taskRepository.findByUser_Id(1L)).thenReturn(tasks);

        // Act
        Task selected = strategy.selectTask(1L, 120);

        // Assert
        assertNotNull(selected);
        // Toutes les tâches sont valides car elles n'ont pas d'événement
    }

    @Test
    void testSelectTask_AllowsTasksWithActiveEvent() {
        // Arrange
        Event activeEvent = new Event();
        activeEvent.setStatus(Event.EventStatus.PLANNED);
        task1.setEvent(activeEvent);

        List<Task> tasks = Arrays.asList(task1, task2, task3);
        when(taskRepository.findByUser_Id(1L)).thenReturn(tasks);

        // Act
        Task selected = strategy.selectTask(1L, 120);

        // Assert
        assertNotNull(selected);
        // task1 devrait être sélectionnable car son événement est actif
    }

    @Test
    void testSelectTask_ReturnsNullWhenNoTasksAvailable() {
        // Arrange
        when(taskRepository.findByUser_Id(1L)).thenReturn(Collections.emptyList());

        // Act
        Task selected = strategy.selectTask(1L, 120);

        // Assert
        assertNull(selected);
    }

    @Test
    void testSelectTask_ReturnsNullWhenAllTasksFiltered() {
        // Arrange
        task1.setStatus(Task.TaskStatus.DONE);
        task2.setStatus(Task.TaskStatus.DONE);
        task3.setStatus(Task.TaskStatus.DONE);
        List<Task> tasks = Arrays.asList(task1, task2, task3);
        when(taskRepository.findByUser_Id(1L)).thenReturn(tasks);

        // Act
        Task selected = strategy.selectTask(1L, 120);

        // Assert
        assertNull(selected);
    }

    @Test
    void testSelectTask_ReturnsNullWhenNoTaskFitsTime() {
        // Arrange
        // Toutes les tâches nécessitent plus de 10 minutes
        when(taskRepository.findByUser_Id(1L)).thenReturn(Arrays.asList(task1, task2, task3));

        // Act - Seulement 5 minutes disponibles
        Task selected = strategy.selectTask(1L, 5);

        // Assert
        assertNull(selected);
    }

    @Test
    void testSelectTask_ExactTimeMatch() {
        // Arrange
        List<Task> tasks = Arrays.asList(task1, task2, task3);
        when(taskRepository.findByUser_Id(1L)).thenReturn(tasks);

        // Act - Exactement 30 minutes disponibles
        Task selected = strategy.selectTask(1L, 30);

        // Assert
        assertNotNull(selected);
        assertTrue(selected.getEstimatedDuration() <= 30);
    }
}
