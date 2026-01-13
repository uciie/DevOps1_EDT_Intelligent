package com.example.backend.service;

import com.example.backend.model.Task;
import java.time.LocalDateTime;
import java.util.List;

public interface TaskService {

    // Récupérer toutes les tâches d’un utilisateur
    List<Task> getTasksByUserId(Long userId);

    // Récupère les tâches où l'utilisateur est l'assigné (pour "Mes Tâches")
    List<Task> getTasksAssignedToUser(Long userId);
    
    // Récupère les tâches créées par l'utilisateur mais déléguées à d'autres
    List<Task> getDelegatedTasks(Long creatorId);

    // Créer une nouvelle tâche (non planifiée)
    Task createTask(Task task, Long userId);

    // Note : On ajoute userId pour savoir QUI tente de modifier
    Task updateTask(Long id, Task task, Long userId);

    // Mettre à jour une tâche existante
    Task updateTask(Long id, Task task);

    // Supprimer une tâche
    void deleteTask(Long id);

    // Planifier une tâche dans le calendrier (drag and drop)
    Task planifyTask(Long taskId, LocalDateTime start, LocalDateTime end);

    List<Task> getTasksByTeam(Long teamId);

    // Récupérer une tâche par son ID
    Task getTaskById(Long id);
}