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

class ChatbotIntegrationTest {

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

        // Mock d'un utilisateur par défaut pour les tests de création
        User user = new User();
        user.setId(1L);
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
    }

    @Test
    void handleUserRequest_shouldExecuteAddTask() {
        // 1. On simule que Gemini décide d'appeler "add_task"
        var funcCall = new GeminiService.FunctionCall("add_task", Map.of(
            "title", "Faire les courses",
            "priority", "1"
        ));
        var part = new GeminiService.Part(null, funcCall);
        var response = new GeminiService.GeminiResponse(List.of(
            new GeminiService.Candidate(new GeminiService.Content(List.of(part)))
        ));

        when(geminiService.chatWithGemini(anyString())).thenReturn(response);

        // 2. Appel du service
        String result = chatbotService.handleUserRequest(1L, "Ajoute faire les courses en prio 1");

        // 3. Vérifications
        assertTrue(result.contains("ajoutée"));
        ArgumentCaptor<Task> taskCaptor = ArgumentCaptor.forClass(Task.class);
        verify(taskRepository).save(taskCaptor.capture());
        assertEquals("Faire les courses", taskCaptor.getValue().getTitle());
        assertEquals(1, taskCaptor.getValue().getPriority());
    }

    @Test
    void handleUserRequest_shouldExecuteCancelAfternoon() {
        // Simulation de l'appel fonctionnel "cancel_afternoon"
        var funcCall = new GeminiService.FunctionCall("cancel_afternoon", Collections.emptyMap());
        var response = new GeminiService.GeminiResponse(List.of(
            new GeminiService.Candidate(new GeminiService.Content(List.of(new GeminiService.Part(null, funcCall))))
        ));

        when(geminiService.chatWithGemini(anyString())).thenReturn(response);
        
        // Simuler la présence d'un événement à supprimer
        Event e = new Event();
        e.setSummary("Réunion");
        when(eventRepository.findByUser_IdAndStartTimeBetween(anyLong(), any(), any()))
            .thenReturn(List.of(e));

        String result = chatbotService.handleUserRequest(1L, "Annule mon après-midi");

        assertTrue(result.contains("annulé 1 événements"));
        verify(eventRepository).deleteAll(anyList());
    }

    @Test
    void handleUserRequest_shouldReturnTextIfNoFunction() {
        // Simulation d'une réponse texte simple de l'IA
        var response = new GeminiService.GeminiResponse(List.of(
            new GeminiService.Candidate(new GeminiService.Content(List.of(new GeminiService.Part("Bonjour !", null))))
        ));

        when(geminiService.chatWithGemini(anyString())).thenReturn(response);

        String result = chatbotService.handleUserRequest(1L, "Coucou");

        assertEquals("Bonjour !", result);
        verifyNoInteractions(eventRepository, taskRepository);
    }
}