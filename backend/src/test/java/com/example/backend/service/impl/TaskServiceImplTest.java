package com.example.backend.service.impl;

import com.example.backend.model.Event;
import com.example.backend.model.Task;
import com.example.backend.model.User;
import com.example.backend.repository.EventRepository;
import com.example.backend.repository.TaskRepository;
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

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TaskServiceImplTest {

    @Mock
    private TaskRepository taskRepository;

    @Mock
    private EventRepository eventRepository;

    @InjectMocks
    private TaskServiceImpl taskService;

    private User user;
    private Task task;

    @BeforeEach
    void setUp() {
        user = new User("testuser", "password");
        user.setId(1L);
        // Constructeur: Task(String title, int estimatedDuration, int priority, boolean done, User user)
        task = new Task("Test Task", 60, 1, false, user);
        task.setId(100L);
    }

    @Test
    void testGetTasksByUserId() {
        // Ajout d'une tâche appartenant à un autre utilisateur pour tester le filtrage
        User otherUser = new User("other", "pwd");
        otherUser.setId(2L);
        Task otherTask = new Task("Other Task", 30, 2, false, otherUser);
        
        when(taskRepository.findAll()).thenReturn(Arrays.asList(task, otherTask));

        // When
        List<Task> result = taskService.getTasksByUserId(1L);

        // Then: On ne doit récupérer que la tâche de l'user 1
        assertEquals(1, result.size());
        assertEquals("Test Task", result.get(0).getTitle());
    }

    @Test
    void testCreateTask() {
        // Given
        when(taskRepository.save(any(Task.class))).thenReturn(task);

        // When
        Task created = taskService.createTask(task);

        // Then
        assertFalse(created.isDone()); // Vérifie que done est forcé à false
        verify(taskRepository).save(task);
    }

    @Test
    void testUpdateTask() {
        // Given
        Task updateInfo = new Task("Updated Title", 120, 2, true, user);
        when(taskRepository.findById(100L)).thenReturn(Optional.of(task));
        when(taskRepository.save(any(Task.class))).thenReturn(task);

        // When
        Task result = taskService.updateTask(100L, updateInfo);

        // Then
        assertEquals("Updated Title", result.getTitle());
        assertEquals(120, result.getEstimatedDuration());
        assertTrue(result.isDone());
        verify(taskRepository).save(task);
    }

    @Test
    void testDeleteTask() {
        // When
        taskService.deleteTask(100L);

        // Then
        verify(taskRepository, times(1)).deleteById(100L);
    }

    @Test
    void testPlanifyTask() {
        // Given
        LocalDateTime start = LocalDateTime.now();
        LocalDateTime end = start.plusHours(1);
        
        when(taskRepository.findById(100L)).thenReturn(Optional.of(task));
        
        when(eventRepository.save(any(Event.class))).thenAnswer(invocation -> {
            Event e = invocation.getArgument(0);
            e.setId(500L);
            return e;
        });
        when(taskRepository.save(any(Task.class))).thenReturn(task);

        // When
        Task result = taskService.planifyTask(100L, start, end);
        // Then
        assertNotNull(result.getEventId());   
        assertEquals(500L, result.getEventId());
        assertEquals(user.getId(), result.getUserId());
        
        // Vérifie que la tâche a bien été ajoutée à la liste de l'événement en mémoire
        assertEquals(1, result.getEvent().getTasks().size());
        
        verify(eventRepository).save(any(Event.class));
        verify(taskRepository).save(task);
    }
    
    @Test
    void testPlanifyTask_ThrowsException_WhenUserIsNull() {
        // Given: Une tâche sans utilisateur
        task.setUser(null);
        when(taskRepository.findById(100L)).thenReturn(Optional.of(task));

        // When & Then
        assertThrows(IllegalStateException.class, () -> {
            taskService.planifyTask(100L, LocalDateTime.now(), LocalDateTime.now().plusHours(1));
        });
    }
}