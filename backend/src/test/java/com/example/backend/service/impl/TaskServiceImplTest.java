package com.example.backend.service.impl;

import com.example.backend.model.Event;
import com.example.backend.model.Task;
import com.example.backend.model.User;
import com.example.backend.repository.EventRepository;
import com.example.backend.repository.TaskRepository;
import com.example.backend.repository.UserRepository; 
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.Collections;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.atLeast;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.times;

@ExtendWith(MockitoExtension.class)
class TaskServiceImplTest {

    @Mock
    private TaskRepository taskRepository;

    @Mock
    private EventRepository eventRepository;

    @Mock
    private UserRepository userRepository; // Mock ajouté

    @InjectMocks
    private TaskServiceImpl taskService;

    @Mock 
    private FocusService focusService;

    private User user;
    private Task task;

    @BeforeEach
    void setUp() {
        user = new User("testuser", "password");
        user.setId(1L);
        // Constructeur: Task(String title, int estimatedDuration, int priority, boolean done, User user)
        task = new Task("Test Task", 60, 1, false, user, (LocalDateTime) null); 
        
        task.setId(100L);
        user.setTeams(new ArrayList<>());
    }

    @Test
    void testGetTasksByUserId() {
        // Given
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        // Mock les appels spécifiques faits par le service
        when(taskRepository.findByUser_Id(1L)).thenReturn(List.of(task));
        when(taskRepository.findByAssignee(user)).thenReturn(List.of(task));

        // When
        List<Task> result = taskService.getTasksByUserId(1L);

        // Then
        assertFalse(result.isEmpty());
        assertEquals(1, result.size()); 
        // Note: Le service utilise un Set, donc le doublon (créateur + assigné) est géré.
    }
    @Test
    void testCreateTask() {
        // Given
        // On doit mocker la recherche de l'utilisateur
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(taskRepository.save(any(Task.class))).thenReturn(task);

        // When
        Task created = taskService.createTask(task, user.getId());

        // Then
        assertFalse(created.isDone()); // Vérifie que done est forcé à false
        verify(userRepository).findById(1L); // Vérifie l'appel
        verify(taskRepository).save(task);
    }

    @Test
    void testCreateTask_UserNotFound() {
        // Test du cas d'erreur
        when(userRepository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(IllegalArgumentException.class, () -> {
            taskService.createTask(task, 99L);
        });
    }

    @Test
    void testUpdateTask() {
        // Given
        Task updateInfo = new Task("Updated Title", 120, 2, true, user, (LocalDateTime) null);
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
    void devraitPlanifierImmediatementSiToutEstLibre() {
        // Configuration du mock
        when(taskRepository.findById(100L)).thenReturn(Optional.of(task));
        when(eventRepository.findByUser_IdOrderByStartTime(1L)).thenReturn(Collections.emptyList());
        when(focusService.estBloqueParLeFocus(eq(1L), any(), any())).thenReturn(false);

        when(eventRepository.save(any(Event.class))).thenAnswer(i -> i.getArgument(0));
        when(taskRepository.save(any(Task.class))).thenReturn(task);

        // Action
        Task result = taskService.planifyTask(100L, null, null);

        // Vérification
        assertNotNull(result.getEvent());
        verify(eventRepository).save(any(Event.class));
    }

    /**
     * Vérifie que l'algorithme "saute" un créneau occupé par une réunion existante
     * pour placer la tâche juste après.
     */
    @Test
    void devraitSauterEvenementExistant() {
        // Configuration : Une réunion existe déjà
        LocalDateTime debutReunion = LocalDateTime.now().plusMinutes(5);
        Event reunion = new Event("Réunion Importante", debutReunion, debutReunion.plusMinutes(60), user);
        
        when(taskRepository.findById(100L)).thenReturn(Optional.of(task));
        when(eventRepository.findByUser_IdOrderByStartTime(1L)).thenReturn(List.of(reunion));
        when(focusService.estBloqueParLeFocus(eq(1L), any(), any())).thenReturn(false);
        
        when(eventRepository.save(any(Event.class))).thenAnswer(i -> i.getArgument(0));
        when(taskRepository.save(any(Task.class))).thenReturn(task);

        // Action
        taskService.planifyTask(100L, null, null);

        // Vérification : L'événement créé doit commencer à la fin de la réunion
        ArgumentCaptor<Event> captor = ArgumentCaptor.forClass(Event.class);
        verify(eventRepository).save(captor.capture());
        assertEquals(reunion.getEndTime(), captor.getValue().getStartTime());
    }

    /**
     * Vérifie que l'algorithme respecte les zones de concentration.
     * Si un créneau est libre de réunion mais marqué comme "Focus", l'algorithme doit chercher plus loin.
     */
    @Test
    void devraitSauterZoneDeFocusActive() {
        // Configuration
        when(taskRepository.findById(100L)).thenReturn(Optional.of(task));
        when(eventRepository.findByUser_IdOrderByStartTime(1L)).thenReturn(Collections.emptyList());

        // Simulation : Le 1er créneau est bloqué par le focus, le 2ème est libre
        when(focusService.estBloqueParLeFocus(eq(1L), any(), any()))
            .thenReturn(true)  // Premier essai : bloqué
            .thenReturn(false); // Deuxième essai : libre

        when(eventRepository.save(any(Event.class))).thenAnswer(i -> i.getArgument(0));
        when(taskRepository.save(any(Task.class))).thenReturn(task);

        // Action
        taskService.planifyTask(100L, null, null);

        // Vérification : On s'assure que le FocusService a été consulté pour valider le créneau
        verify(focusService, atLeast(2)).estBloqueParLeFocus(eq(1L), any(), any());
    }

    /**
     * Vérifie qu'une erreur est lancée si on tente de planifier une tâche 
     * qui n'est rattachée à aucun utilisateur.
     */
    @Test
    void devraitLancerExceptionSiUtilisateurAbsent() {
        // Configuration
        task.setUser(null);
        when(taskRepository.findById(100L)).thenReturn(Optional.of(task));

        // Vérification
        assertThrows(IllegalStateException.class, () -> {
            taskService.planifyTask(100L, null, null);
        });
    }

    /**
     * Vérifie que la planification manuelle fonctionne toujours mais reste 
     * soumise à la validation du blocage Focus.
     */
    
}