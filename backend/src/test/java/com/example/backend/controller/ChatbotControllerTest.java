package com.example.backend.controller;

import com.example.backend.service.ChatbotService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class ChatbotControllerTest {

    private MockMvc mockMvc;
    private ChatbotService chatbotService;

    @BeforeEach
    void setUp() {
        chatbotService = Mockito.mock(ChatbotService.class);
        ChatbotController controller = new ChatbotController(chatbotService);
        mockMvc = MockMvcBuilders.standaloneSetup(controller).build();
    }

    @Test
    void handleMessage_Success() throws Exception {
        String mockResponse = "Voici votre liste de tâches.";
        when(chatbotService.handleUserRequest(anyLong(), anyString())).thenReturn(mockResponse);

        String jsonPayload = "{\"message\": \"affiche mes tâches\", \"userId\": \"1\"}";

        mockMvc.perform(post("/api/chat/message")
                .contentType(MediaType.APPLICATION_JSON)
                .content(jsonPayload))
                .andExpect(status().isOk())
                .andExpect(content().string(mockResponse));
    }

    @Test
    void handleMessage_BadRequest_MissingMessage() throws Exception {
        String jsonPayload = "{\"userId\": \"1\"}";

        mockMvc.perform(post("/api/chat/message")
                .contentType(MediaType.APPLICATION_JSON)
                .content(jsonPayload))
                .andExpect(status().isBadRequest());
    }

    @Test
    void handleMessage_BadRequest_InvalidUserId() throws Exception {
        String jsonPayload = "{\"message\": \"hello\", \"userId\": \"abc\"}";

        mockMvc.perform(post("/api/chat/message")
                .contentType(MediaType.APPLICATION_JSON)
                .content(jsonPayload))
                .andExpect(status().isBadRequest());
    }
}