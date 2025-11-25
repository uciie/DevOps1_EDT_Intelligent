package com.example.backend.controller;

import com.example.backend.model.Task;
import com.example.backend.service.TaskService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;

@RestController
@RequestMapping("/api/tasks")
public class TaskController {

    private final TaskService taskService;

    public TaskController(TaskService taskService) {
        this.taskService = taskService;
    }

    @GetMapping("/user/{userId}")
    public ResponseEntity<List<Task>> getUserTasks(@PathVariable Long userId) {
        return ResponseEntity.ok(taskService.getTasksByUserId(userId));
    }

    @PostMapping("/user/{userId}")
    public ResponseEntity<Task> createTaskForUser(
            @PathVariable Long userId, // Capture l'ID depuis l'URL
            @RequestBody Task task     // Récupère la tâche sans l'objet User complet
    ) {
        // Appelle la méthode du service avec l'ID utilisateur
        return ResponseEntity.ok(taskService.createTask(task, userId));
    }

    @PutMapping("/{id}")
    public ResponseEntity<Task> updateTask(@PathVariable Long id, @RequestBody Task task) {
        return ResponseEntity.ok(taskService.updateTask(id, task));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteTask(@PathVariable Long id) {
        taskService.deleteTask(id);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{taskId}/planify")
    public ResponseEntity<Task> planifyTask(
            @PathVariable Long taskId,
            @RequestParam LocalDateTime start,
            @RequestParam LocalDateTime end
    ) {
        return ResponseEntity.ok(taskService.planifyTask(taskId, start, end));
    }
}
