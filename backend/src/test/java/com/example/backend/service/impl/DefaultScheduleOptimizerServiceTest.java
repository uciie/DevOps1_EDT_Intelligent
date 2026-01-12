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
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class DefaultScheduleOptimizerServiceTest {

    @Mock private EventRepository eventRepository;
    @Mock private TaskRepository taskRepository;
    @Mock private TaskSelectionStrategy taskSelectionStrategy;
    @Mock private TravelTimeService travelTimeService;
    @Mock private FocusService focusService;

    @InjectMocks
    private DefaultScheduleOptimizerService optimizerService;

    private User user;
    private Long userId = 1L;

    @BeforeEach
    void setUp() {
        user = new User("testuser", "password");
        user.setId(userId);
    }

    /**
     * Test de base : vérifie que si tout est libre, les tâches sont planifiées
     * les unes après les autres à partir de l'heure du curseur (8h).
     */
    @Test
    void devraitPlanifierTachesALaSuiteSiToutEstLibre() {
        // Given
        Task t1 = createSimpleTask("Tâche 1", 60, 1);
        Task t2 = createSimpleTask("Tâche 2", 30, 2);
        
        when(eventRepository.findByUser_IdOrderByStartTime(userId)).thenReturn(new ArrayList<>());
        when(taskRepository.findByUser_Id(userId)).thenReturn(new ArrayList<>(Arrays.asList(t1, t2)));
        when(focusService.estBloqueParLeFocus(eq(userId), any(), any())).thenReturn(false);

        // When
        optimizerService.reshuffle(userId);

        // Then
        verify(eventRepository, times(2)).save(any(Event.class));
        verify(taskRepository, times(2)).save(any(Task.class));
        assertNotNull(t1.getEvent());
        assertNotNull(t2.getEvent());
        // T2 doit commencer à la fin de T1
        assertEquals(t1.getEvent().getEndTime(), t2.getEvent().getStartTime());
    }

    /**
     * Vérifie que l'algorithme respecte l'ordre de priorité : 
     * Deadline la plus proche d'abord, puis priorité la plus haute.
     */
    @Test
    void devraitRespecterOrdrePrioriteEtDeadline() {
        // Given
        LocalDateTime demain = LocalDateTime.now().plusDays(1);
        Task peuPrioritaire = createSimpleTask("Pas pressé", 60, 5); // Priorité basse
        Task urgente = createSimpleTask("Urgente", 60, 1);
        urgente.setDeadline(demain); // Deadline proche

        when(eventRepository.findByUser_IdOrderByStartTime(userId)).thenReturn(new ArrayList<>());
        when(taskRepository.findByUser_Id(userId)).thenReturn(new ArrayList<>(Arrays.asList(peuPrioritaire, urgente)));
        when(focusService.estBloqueParLeFocus(eq(userId), any(), any())).thenReturn(false);

        // When
        optimizerService.reshuffle(userId);

        // Then : "Urgente" doit être planifiée avant "Pas pressé"
        assertTrue(urgente.getEvent().getStartTime().isBefore(peuPrioritaire.getEvent().getStartTime()));
    }

    /**
     * LE TEST CRUCIAL : Vérifie que le reshuffle saute une zone où le Focus est activé.
     */
    @Test
    void devraitSauterZoneDeFocusPendantLeReshuffle() {
        // Given
        Task task = createSimpleTask("Tâche Focus", 60, 1);
        
        when(eventRepository.findByUser_IdOrderByStartTime(userId)).thenReturn(new ArrayList<>());
        when(taskRepository.findByUser_Id(userId)).thenReturn(new ArrayList<>(List.of(task)));

        // Simulation : Le focus bloque le premier créneau testé, mais accepte le suivant
        when(focusService.estBloqueParLeFocus(eq(userId), any(), any()))
            .thenReturn(true)  // Bloqué au début
            .thenReturn(false); // Libre après décalage

        // When
        optimizerService.reshuffle(userId);

        // Then
        verify(focusService, atLeast(2)).estBloqueParLeFocus(eq(userId), any(), any());
        assertNotNull(task.getEvent());
        verify(eventRepository).save(any(Event.class));
    }

    /**
     * Vérifie que l'algorithme ne planifie pas une tâche sur une réunion existante.
     */
    @Test
    void devraitSauterReunionsExistantes() {
        // Given - On place la réunion pile au début (08h00) pour forcer le saut
        LocalDateTime debutReunion = LocalDateTime.now().withHour(8).withMinute(0).withSecond(0).withNano(0);
        LocalDateTime finReunion = debutReunion.plusHours(1); // Finit à 09h00
        
        Event reunion = new Event("Réunion Matinale", debutReunion, finReunion, user);
        
        Task task = createSimpleTask("Tâche après réunion", 60, 1);

        when(eventRepository.findByUser_IdOrderByStartTime(userId)).thenReturn(new ArrayList<>(List.of(reunion)));
        when(taskRepository.findByUser_Id(userId)).thenReturn(new ArrayList<>(List.of(task)));
        when(focusService.estBloqueParLeFocus(eq(userId), any(), any())).thenReturn(false);

        // When
        optimizerService.reshuffle(userId);

        // Then
        assertNotNull(task.getEvent(), "La tâche devrait être planifiée");
        // La tâche doit commencer au plus tôt à 09h00 (fin de la réunion)
        assertTrue(task.getEvent().getStartTime().isAfter(reunion.getEndTime()) 
                || task.getEvent().getStartTime().equals(reunion.getEndTime()),
                "La tâche devrait commencer à " + reunion.getEndTime() + " mais commence à " + task.getEvent().getStartTime());
    }

    /**
     * Vérifie que les tâches dont la deadline est passée sont marquées "Late"
     * et ne sont pas planifiées.
     */
    @Test
    void devraitMarquerTacheEnRetardSiDeadlineDepassee() {
        // Given
        Task taskEnRetard = createSimpleTask("Tâche périmée", 60, 1);
        taskEnRetard.setDeadline(LocalDateTime.now().minusDays(1)); // Deadline hier

        when(eventRepository.findByUser_IdOrderByStartTime(userId)).thenReturn(new ArrayList<>());
        when(taskRepository.findByUser_Id(userId)).thenReturn(new ArrayList<>(List.of(taskEnRetard)));

        // When
        optimizerService.reshuffle(userId);

        // Then
        assertTrue(taskEnRetard.isLate());
        assertNull(taskEnRetard.getEvent());
        verify(taskRepository).save(taskEnRetard);
    }

    // Helper pour créer des tâches rapidement
    private Task createSimpleTask(String title, int duration, int priority) {
        // On précise (LocalDateTime) devant le null pour lever l'ambiguïté
        Task t = new Task(title, duration, priority, false, user, (LocalDateTime) null);
        t.setId((long) (Math.random() * 1000));
        return t;
    }
}