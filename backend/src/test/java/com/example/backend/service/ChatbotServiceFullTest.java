package com.example.backend.service;

import com.example.backend.model.Event;
import com.example.backend.model.Task;
import com.example.backend.model.User;
import com.example.backend.repository.EventRepository;
import com.example.backend.repository.TaskRepository;
import com.example.backend.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class ChatbotServiceFullTest {

    private EventRepository eventRepository;
    private TaskRepository taskRepository;
    private UserRepository userRepository;
    private GeminiService geminiService;
    private ChatbotService chatbotService;

    @BeforeEach
    void setUp() {
        eventRepository = mock(EventRepository.class);
        taskRepository = mock(TaskRepository.class);
        userRepository = mock(UserRepository.class);
        geminiService = mock(GeminiService.class);
        chatbotService = new ChatbotService(geminiService, eventRepository, taskRepository, userRepository);

        User user = new User();
        user.setId(1L);
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
    }

    // --- HELPER POUR SIMULER GEMINI ---
    private void mockGeminiFunction(String name, Map<String, Object> args) {
        var funcCall = new GeminiService.FunctionCall(name, args);
        var response = new GeminiService.GeminiResponse(List.of(
            new GeminiService.Candidate(new GeminiService.Content(List.of(
                new GeminiService.Part(null, funcCall)
            )))
        ));
        when(geminiService.chatWithGemini(anyString())).thenReturn(response);
    }

    // 1. LISTER ÉVÉNEMENTS (DATES & CRÉNEAUX)
    @Test
    void testListEvents() {
        mockGeminiFunction("list_events_between", Map.of("start", "2026-01-25T08:00:00", "end", "2026-01-25T18:00:00"));
        when(eventRepository.findByUser_IdAndStartTimeBetween(anyLong(), any(), any()))
            .thenReturn(List.of(new Event("Réunion", LocalDateTime.now(), LocalDateTime.now())));

        String result = chatbotService.handleUserRequest(1L, "Quoi de prévu demain ?");
        assertTrue(result.contains("Réunion"));
    }

    // 2. LISTER TÂCHES (PRIORITÉ & TOUT)
    @Test
    void testListTasksByPriority() {
        mockGeminiFunction("list_tasks_by_priority", Map.of("priority", "1"));
        Task t = new Task(); t.setTitle("Urgent"); t.setPriority(1);
        when(taskRepository.findByUser_IdAndPriority(anyLong(), eq(1))).thenReturn(List.of(t));

        String result = chatbotService.handleUserRequest(1L, "Tâches prioritaires");
        assertTrue(result.contains("Urgent"));
    }

    // 3. ANNULATIONS (MATIN / APRÈS-MIDI / CRÉNEAU)
    @Test
    void testCancelMorningAndAfternoon() {
        // Test Matin
        mockGeminiFunction("cancel_morning", Collections.emptyMap());
        chatbotService.handleUserRequest(1L, "Annule mon matin");
        verify(eventRepository, atLeastOnce()).deleteAll(anyList());

        // Test Après-midi
        mockGeminiFunction("cancel_afternoon", Collections.emptyMap());
        chatbotService.handleUserRequest(1L, "Libère mon après-midi");
        verify(eventRepository, atLeast(2)).deleteAll(anyList());
    }

    // 4. ANNULER TÂCHE PRÉCISE OU PAR PRIORITÉ
    @Test
    void testCancelTasks() {
        mockGeminiFunction("cancel_tasks_by_priority", Map.of("priority", "3"));
        chatbotService.handleUserRequest(1L, "Supprime les tâches peu importantes");
        verify(taskRepository).deleteByUser_IdAndPriority(1L, 3);
    }

    // 5. DÉPLACER ACTIVITÉ
    @Test
    void testMoveActivity() {
        mockGeminiFunction("move_activity", Map.of("id", "10", "newStart", "2026-01-26T10:00:00"));
        Event e = new Event();
        when(eventRepository.findById(10L)).thenReturn(Optional.of(e));

        chatbotService.handleUserRequest(1L, "Déplace ma réunion à 10h");
        verify(eventRepository).save(e);
        assertEquals(LocalDateTime.parse("2026-01-26T10:00:00"), e.getStartTime());
    }

    // 6. AJOUTER TÂCHE / ÉVÉNEMENT
    @Test
    void testAddTaskAndEvent() {
        // Ajout Tâche
        mockGeminiFunction("add_task", Map.of("title", "Coder les tests", "priority", "1"));
        chatbotService.handleUserRequest(1L, "Ajoute une tâche");
        verify(taskRepository).save(any(Task.class));

        // Ajout Événement
        mockGeminiFunction("add_event", Map.of("summary", "Dîner", "start", "2026-01-25T20:00:00", "end", "2026-01-25T22:00:00"));
        chatbotService.handleUserRequest(1L, "J'ai un dîner ce soir");
        verify(eventRepository).save(any(Event.class));
    }
}