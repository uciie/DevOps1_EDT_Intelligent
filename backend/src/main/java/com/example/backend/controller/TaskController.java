package com.example.backend.controller;

import com.example.backend.model.Task;
import com.example.backend.service.impl.FocusService;
import com.example.backend.service.TaskService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.List;

@RestController
@RequestMapping("/api/tasks")
public class TaskController {

    private final TaskService taskService;
    private final FocusService focusService;

    public TaskController(TaskService taskService, FocusService focusService) {
        this.taskService = taskService;
        this.focusService = focusService;
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

    // On ajoute le userId pour vérifier les droits (RM-03)
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
    public ResponseEntity<?> planifyTask(
        @PathVariable Long taskId,
        @RequestParam(required = false) LocalDateTime start, 
        @RequestParam(required = false) LocalDateTime end
    ) {
        try {
            // 2. On a besoin du userId pour vérifier ses préférences
            // On récupère d'abord la tâche via le service
            Task taskToPlan = taskService.getTaskById(taskId); // Assure-toi que cette méthode existe dans TaskService
            
            if (start != null) {
                // 3. Validation de la surcharge (Etudiant 1)
                focusService.validateDayNotOverloaded(taskToPlan.getUser().getId(), start);
            }

            Task plannedTask = taskService.planifyTask(taskId, start, end);
            return ResponseEntity.ok(plannedTask);
            
        } catch (ResponseStatusException e) {
            return ResponseEntity.status(e.getStatusCode()).body(e.getReason());
        }
    }
    
    //Récupérer les tâches d'une équipe spécifique
    @GetMapping("/team/{teamId}")
    public ResponseEntity<List<Task>> getTeamTasks(@PathVariable Long teamId) {
        List<Task> tasks = taskService.getTasksByTeam(teamId);
        return ResponseEntity.ok(tasks);
    }
}
