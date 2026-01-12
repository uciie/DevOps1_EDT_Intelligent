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

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.verify;
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

    private User user;
    private Task task;

    @BeforeEach
    void setUp() {
        user = new User("testuser", "password");
        user.setId(1L);
        // Constructeur: Task(String title, int estimatedDuration, int priority, boolean done, User user)
        task = new Task("Test Task", 60, 1, false, user, (LocalDateTime) null); 
        
        task.setId(100L);
    }

    @Test
    void testGetTasksByUserId() {
        // 1. GIVEN : Préparer les objets
        User mockUser = new User();
        mockUser.setId(1L);
        mockUser.setUsername("testuser");

        Task task1 = new Task();
        task1.setTitle("Test Task");
        task1.setAssignee(mockUser); // La tâche est assignée à cet utilisateur

        // 2. MOCKING : Configurer les comportements attendus
        // Le service appelle d'abord userRepository.findById(1L)
        when(userRepository.findById(1L)).thenReturn(Optional.of(mockUser));
        
        // Le service appelle ensuite taskRepository.findByAssignee(mockUser)
        // IMPORTANT : On utilise findByAssignee et non findByUserId car c'est ce qu'il y a dans votre TaskServiceImpl
        when(taskRepository.findByAssignee(mockUser)).thenReturn(Arrays.asList(task1));

        // 3. WHEN : Appeler la méthode
        List<Task> result = taskService.getTasksByUserId(1L);

        // 4. THEN : Vérifier le résultat
        assertNotNull(result);
        assertEquals(1, result.size(), "La liste devrait contenir 1 tâche");
        assertEquals("Test Task", result.get(0).getTitle());
        
        // Vérifier que les méthodes ont bien été appelées
        verify(userRepository).findById(1L);
        verify(taskRepository).findByAssignee(mockUser);
    }

    @Test
    void testCreateTask() {
        // 1. Préparation explicite des données (Given)
        User user = new User();
        user.setId(1L);
        user.setTeams(new ArrayList<>());
        
        Task taskToCreate = new Task();
        taskToCreate.setTitle("Test Task");
        
        // Configurer le mock pour simuler l'enregistrement avec succès et l'attribution d'un ID
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(taskRepository.save(any(Task.class))).thenAnswer(invocation -> {
            Task t = invocation.getArgument(0);
            t.setId(100L); // Simuler l'ID généré par la DB
            return t;
        });

        // 2. Exécution (When)
        Task created = taskService.createTask(taskToCreate, 1L);

        // 3. Vérifications (Then)
        assertNotNull(created);
        assertEquals(100L, created.getId()); // Vérifie que l'ID a bien été "généré"
        assertFalse(created.isDone()); // Vérifie la logique métier
        assertEquals(user, created.getUser()); // Vérifie l'association à l'utilisateur
        
        verify(userRepository).findById(1L);
        verify(taskRepository).save(any(Task.class));
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

    // NOUVEAU TEST : Teste la logique First-Fit
    @Test
    void testPlanifyTask_AutomaticFirstFit() {
        // Given
        // Simuler le calendrier vide pour que First-Fit trouve un créneau
        when(taskRepository.findById(100L)).thenReturn(Optional.of(task));
        when(eventRepository.findByUser_IdOrderByStartTime(1L)).thenReturn(List.of()); // Calendrier vide

        // Simuler la sauvegarde de l'événement et lier son ID à la tâche
        when(eventRepository.save(any(Event.class))).thenAnswer(invocation -> {
            Event e = invocation.getArgument(0);
            e.setId(600L); 
            return e;
        });
        when(taskRepository.save(any(Task.class))).thenReturn(task);

        // When: Appel avec start=null et end=null
        Task result = taskService.planifyTask(100L, null, null); 

        // Then
        assertNotNull(result.getEventId());
        assertEquals(600L, result.getEventId());
        
        // Vérifier que la méthode a calculé une heure de début et de fin
        ArgumentCaptor<Event> eventCaptor = ArgumentCaptor.forClass(Event.class);
        verify(eventRepository).save(eventCaptor.capture());
        
        Event savedEvent = eventCaptor.getValue();
        assertNotNull(savedEvent.getStartTime(), "L'heure de début doit être calculée");
        assertNotNull(savedEvent.getEndTime(), "L'heure de fin doit être calculée");
        
        // Vérifie que l'événement a la bonne durée (60 minutes, si votre logique First-Fit le fait)
        // Ici, on vérifie juste que l'heure de fin est après l'heure de début.
        assertTrue(savedEvent.getEndTime().isAfter(savedEvent.getStartTime())); 

        verify(taskRepository).save(task);
    }
}