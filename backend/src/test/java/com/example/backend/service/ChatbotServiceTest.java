package com.example.backend.service;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.example.backend.model.Event;
import com.example.backend.model.Task;
import com.example.backend.model.User;
import com.example.backend.repository.ChatMessageRepository;
import com.example.backend.repository.EventRepository;
import com.example.backend.repository.TaskRepository;
import com.example.backend.repository.UserRepository;

class ChatbotServiceTest {

    private GeminiService geminiService;
    private EventRepository eventRepository;
    private TaskRepository taskRepository;
    private UserRepository userRepository;
    private ChatbotService chatbotService;
    private ChatMessageRepository chatMessageRepository;

    @BeforeEach
    void setUp() {
        geminiService = mock(GeminiService.class);
        eventRepository = mock(EventRepository.class);
        taskRepository = mock(TaskRepository.class);
        userRepository = mock(UserRepository.class);
        chatMessageRepository = mock(ChatMessageRepository.class);
        chatbotService = new ChatbotService(geminiService, chatMessageRepository, eventRepository, taskRepository, userRepository);
    }

    @Test
    void handleUserRequest_returnsSimpleText() {
        // Mocking Gemini text response
        var part = new GeminiService.Part("Bonjour, comment puis-je vous aider ?", null);
        var content = new GeminiService.Content(List.of(part));
        var response = new GeminiService.GeminiResponse(List.of(new GeminiService.Candidate(content)));
        
        when(geminiService.chatWithGemini(anyString(), anyList())).thenReturn(response);

        String result = chatbotService.handleUserRequest(1L, "Salut");
        assertEquals("Bonjour, comment puis-je vous aider ?", result);
    }

    @Test
    void handleUserRequest_executesAddTask() {
        // Mocking Gemini function call response
        var funcCall = new GeminiService.FunctionCall("add_task", Map.of("title", "Acheter du pain", "priority", "1"));
        var part = new GeminiService.Part(null, funcCall);
        var content = new GeminiService.Content(List.of(part));
        var response = new GeminiService.GeminiResponse(List.of(new GeminiService.Candidate(content)));

        when(geminiService.chatWithGemini(anyString(), anyList())).thenReturn(response);
        when(userRepository.findById(1L)).thenReturn(Optional.of(new User()));

        String result = chatbotService.handleUserRequest(1L, "Ajoute la tâche Acheter du pain en priorité 1");
        
        assertTrue(result.contains("ajoutée"));
        verify(taskRepository, times(1)).save(any(Task.class));
    }

    @Test
    void handleUserRequest_executesListTasks_empty() {
        var funcCall = new GeminiService.FunctionCall("list_all_tasks", Collections.emptyMap());
        var part = new GeminiService.Part(null, funcCall);
        var content = new GeminiService.Content(List.of(part));
        var response = new GeminiService.GeminiResponse(List.of(new GeminiService.Candidate(content)));

        when(geminiService.chatWithGemini(anyString(), anyList())).thenReturn(response);
        when(taskRepository.findByUser_Id(1L)).thenReturn(Collections.emptyList());

        String result = chatbotService.handleUserRequest(1L, "Liste mes tâches");
        assertEquals("Aucune tâche à faire.", result);
    }

    @Test
    void handleUserRequest_executesCancelMorning() {
        var funcCall = new GeminiService.FunctionCall("cancel_morning", Map.of("date", "2026-01-25"));        var part = new GeminiService.Part(null, funcCall);
        var content = new GeminiService.Content(List.of(part));
        var response = new GeminiService.GeminiResponse(List.of(new GeminiService.Candidate(content)));

        when(geminiService.chatWithGemini(anyString(), anyList())).thenReturn(response);
        when(eventRepository.findByUser_IdAndStartTimeBetween(anyLong(), any(), any()))
            .thenReturn(List.of(new Event()));
        String result = chatbotService.handleUserRequest(1L, "Annule ma matinée");
        assertTrue(result.contains("annulé"));
        verify(eventRepository).deleteAll(anyList());
    }
}