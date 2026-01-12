package com.example.backend.controller;

import com.example.backend.model.Task;
import com.example.backend.model.User;
import com.example.backend.service.TaskService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean; // Utiliser @MockBean pour Spring Boot < 3.4
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.Arrays;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;

@WebMvcTest(TaskController.class)
class TaskControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean // Remplace @MockBean
    private TaskService taskService;

    @Autowired
    private ObjectMapper objectMapper;

    private Task task;

    @BeforeEach
    void setUp() {
        User user = new User("user", "pass");
        user.setId(1L);
        
        // CORRECTION : Cast explicite de null en LocalDateTime pour lever l'ambiguïté
        task = new Task("Test Task", 60, 1, false, user, (LocalDateTime) null); 
        
        task.setId(1L);

        objectMapper = new ObjectMapper();
        // Requis pour gérer LocalDateTime
        objectMapper.registerModule(new com.fasterxml.jackson.datatype.jsr310.JavaTimeModule());
    }

    @Test
    void testGetUserTasks() throws Exception {
        when(taskService.getTasksByUserId(1L)).thenReturn(Arrays.asList(task));

        mockMvc.perform(get("/api/tasks/user/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].title").value("Test Task"));
    }

    @Test
    void testCreateTask() throws Exception {
        // Le contrôleur appelle createTask(Task task, Long userId). Mocker cette signature.
        when(taskService.createTask(any(Task.class), eq(1L))).thenReturn(task); 

        mockMvc.perform(post("/api/tasks/user/1") // <-- URL CORRIGÉE
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(task)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.title").value("Test Task"));
    }
    
    // ... updateTask et deleteTask existants

    // TEST MIS À JOUR POUR LA PLANIFICATION AVEC DATES (Test du cas 1)
    @Test
    void testPlanifyTask_WithDates() throws Exception {
        when(taskService.planifyTask(eq(1L), any(LocalDateTime.class), any(LocalDateTime.class)))
                .thenReturn(task);

        mockMvc.perform(post("/api/tasks/1/planify")
                .param("start", "2025-10-10T10:00:00")
                .param("end", "2025-10-10T11:00:00"))
                .andExpect(status().isOk());
        
        // Vérifier que la méthode du service est bien appelée
        verify(taskService).planifyTask(eq(1L), any(LocalDateTime.class), any(LocalDateTime.class));
    }
    
    // NOUVEAU TEST POUR LA PLANIFICATION AUTOMATIQUE (Test du cas 2: First-Fit)
    // ----------------------------------------------------------------------
    @Test
    void testPlanifyTask_AutomaticFirstFit() throws Exception {
        // Le mock doit gérer les arguments null
        when(taskService.planifyTask(eq(1L), eq(null), eq(null))) 
                .thenReturn(task);

        // Appel sans aucun paramètre 'param'
        mockMvc.perform(post("/api/tasks/1/planify"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1L));

        // Vérifier que la méthode du service est appelée avec null pour les dates
        // Remarque: L'appel MockMvc est converti en 'null' par Spring pour les @RequestParam(required = false) manquants.
        verify(taskService).planifyTask(eq(1L), eq(null), eq(null));
    }
    
    @Test
    void testUpdateTask() throws Exception {
        Task taskToUpdate = new Task();
        taskToUpdate.setId(1L);
        taskToUpdate.setTitle("Titre mis à jour");
        taskToUpdate.setUser(null); // Ne mettez pas d'objet User complet ici si possible, ou mettez-le à null
        
        when(taskService.updateTask(eq(1L), any(Task.class))).thenReturn(taskToUpdate);

        mockMvc.perform(put("/api/tasks/1")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(taskToUpdate)))
                .andDo(print()) // Maintenant cela fonctionnera avec l'import
                .andExpect(status().isOk());
    }

    @Test
    void testDeleteTask() throws Exception {
        mockMvc.perform(delete("/api/tasks/1"))
                .andExpect(status().isNoContent());

        verify(taskService).deleteTask(1L);
    }

    @Test
    void testPlanifyTask() throws Exception {
        when(taskService.planifyTask(eq(1L), any(LocalDateTime.class), any(LocalDateTime.class)))
                .thenReturn(task);

        // Attention au format de date ISO pour LocalDateTime
        mockMvc.perform(post("/api/tasks/1/planify")
                .param("start", "2025-10-10T10:00:00")
                .param("end", "2025-10-10T11:00:00"))
                .andExpect(status().isOk());
    }
}