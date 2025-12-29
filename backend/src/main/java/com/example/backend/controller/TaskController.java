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

    // Récupère les tâches que l'utilisateur a déléguées à d'autres
    @GetMapping("/user/{userId}/delegated")
    public ResponseEntity<List<Task>> getDelegatedTasks(@PathVariable Long userId) {
        return ResponseEntity.ok(taskService.getDelegatedTasks(userId));
    }

    @PostMapping("/user/{userId}")
    public ResponseEntity<Task> createTaskForUser(
            @PathVariable Long userId, // Capture l'ID depuis l'URL
            @RequestBody Task task     // Récupère la tâche sans l'objet User complet
    ) {
        // Appelle la méthode du service avec l'ID utilisateur
        return ResponseEntity.ok(taskService.createTask(task, userId));
    }

    // MISE À JOUR : On ajoute le userId pour vérifier les droits (RM-03)
    // Exemple d'appel : PUT /api/tasks/12?userId=5
    @PutMapping("/{id}")
    public ResponseEntity<Task> updateTask(
            @PathVariable Long id, 
            @RequestBody Task task,
            @RequestParam Long userId // Obligatoire pour savoir QUI modifie
    ) {
        return ResponseEntity.ok(taskService.updateTask(id, task, userId));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteTask(@PathVariable Long id) {
        taskService.deleteTask(id);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{taskId}/planify")
    public ResponseEntity<Task> planifyTask(
        @PathVariable Long taskId,
        // MARQUEZ LES PARAMÈTRES COMME OPTIONNELS
        @RequestParam(required = false) LocalDateTime start, 
        @RequestParam(required = false) LocalDateTime end
    ) {
        Task plannedTask = taskService.planifyTask(taskId, start, end);
        return ResponseEntity.ok(plannedTask);
    }
    
    //Récupérer les tâches d'une équipe spécifique
    @GetMapping("/team/{teamId}")
    public ResponseEntity<List<Task>> getTeamTasks(@PathVariable Long teamId) {
        List<Task> tasks = taskService.getTasksByTeam(teamId);
        return ResponseEntity.ok(tasks);
    }
}
