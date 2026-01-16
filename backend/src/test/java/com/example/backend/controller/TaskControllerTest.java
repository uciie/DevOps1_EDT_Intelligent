package com.example.backend.controller;

import com.example.backend.model.Task;
import com.example.backend.model.User;
import com.example.backend.service.TaskService;
import com.example.backend.service.impl.FocusService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Arrays;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(TaskController.class)
public class TaskControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private TaskService taskService;

    @MockitoBean
    private FocusService focusService;

    @Autowired
    private ObjectMapper objectMapper;

    private Task testTask;
    private User testUser;

    @BeforeEach
    void setUp() {
        testUser = new User();
        testUser.setId(1L);
        testUser.setUsername("testuser");

        testTask = new Task();
        testTask.setId(1L);
        testTask.setTitle("Tâche de test");
        testTask.setUser(testUser);
    }

    @Test
    void testGetUserTasks() throws Exception {
        Mockito.when(taskService.getTasksByUserId(1L)).thenReturn(Arrays.asList(testTask));

        mockMvc.perform(get("/api/tasks/user/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].title").value("Tâche de test"));
    }

    @Test
    void testCreateTaskForUser() throws Exception {
        // La signature est createTask(Task, Long)
        Mockito.when(taskService.createTask(any(Task.class), eq(1L))).thenReturn(testTask);

        mockMvc.perform(post("/api/tasks/user/1")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(testTask)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.title").value("Tâche de test"));
    }

    @Test
    void testUpdateTask() throws Exception {
        // La signature utilisée dans le controller est updateTask(Long, Task, Long)
        Mockito.when(taskService.updateTask(eq(1L), any(Task.class), eq(1L))).thenReturn(testTask);

        mockMvc.perform(put("/api/tasks/1")
                .param("userId", "1")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(testTask)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.title").value("Tâche de test"));
    }

    @Test
    void testDeleteTask() throws Exception {
        Mockito.doNothing().when(taskService).deleteTask(1L, 1L);

        mockMvc.perform(delete("/api/tasks/1")
                .param("userId", "1"))
                .andExpect(status().isNoContent());
    }

    @Test
    void testGetTeamTasks() throws Exception {
        Mockito.when(taskService.getTasksByTeam(10L)).thenReturn(Arrays.asList(testTask));

        mockMvc.perform(get("/api/tasks/team/10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].title").value("Tâche de test"));
    }

    @Test
    void testPlanifyTask() throws Exception {
        Mockito.when(taskService.getTaskById(1L)).thenReturn(testTask);
        Mockito.when(taskService.planifyTask(eq(1L), any(), any())).thenReturn(testTask);

        mockMvc.perform(post("/api/tasks/1/planify")
                .param("start", "2024-12-30T10:00:00")
                .param("end", "2024-12-30T11:00:00"))
                .andExpect(status().isOk());
    }
}