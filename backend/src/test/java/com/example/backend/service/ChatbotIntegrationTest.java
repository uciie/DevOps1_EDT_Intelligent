package com.example.backend.service;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.example.backend.model.Event;
import com.example.backend.model.Task;
import com.example.backend.model.User;
import com.example.backend.repository.ChatMessageRepository;
import com.example.backend.repository.EventRepository;
import com.example.backend.repository.TaskRepository;
import com.example.backend.repository.UserRepository;

class ChatbotIntegrationTest {

    private EventRepository eventRepository;
    private TaskRepository taskRepository;
    private UserRepository userRepository;
    private ChatMessageRepository chatMessageRepository;
    private GeminiService geminiService;
    private ChatbotService chatbotService;

    @BeforeEach
    void setUp() {
        eventRepository = mock(EventRepository.class);
        taskRepository = mock(TaskRepository.class);
        userRepository = mock(UserRepository.class);
        chatMessageRepository = mock(ChatMessageRepository.class);
        geminiService = mock(GeminiService.class);
        
        chatbotService = new ChatbotService(geminiService, chatMessageRepository, eventRepository, taskRepository, userRepository);
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

        when(geminiService.chatWithGemini(anyString(), anyList())).thenReturn(response);

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
        var funcCall = new GeminiService.FunctionCall("cancel_afternoon", Map.of("date", "2026-01-25"));
        var response = new GeminiService.GeminiResponse(List.of(
            new GeminiService.Candidate(new GeminiService.Content(List.of(new GeminiService.Part(null, funcCall))))
        ));

        when(geminiService.chatWithGemini(anyString(), anyList())).thenReturn(response);
        
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

        when(geminiService.chatWithGemini(anyString(), anyList())).thenReturn(response);

        String result = chatbotService.handleUserRequest(1L, "Coucou");

        assertEquals("Bonjour !", result);
        verifyNoInteractions(eventRepository, taskRepository);
    }
}