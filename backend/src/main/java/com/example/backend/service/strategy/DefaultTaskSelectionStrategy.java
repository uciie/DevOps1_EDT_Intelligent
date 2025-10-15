package com.example.backend.service.strategy;

import com.example.backend.model.Task;
import com.example.backend.repository.TaskRepository;
import org.springframework.stereotype.Component;

import java.util.Comparator;
import java.util.List;

@Component
public class DefaultTaskSelectionStrategy implements TaskSelectionStrategy {

    private final TaskRepository taskRepository;

    public DefaultTaskSelectionStrategy(TaskRepository taskRepository) {
        this.taskRepository = taskRepository;
    }

    @Override
    public Task selectTask(Long userId, long availableMinutes) {
        // Récupération des tâches liées à l’utilisateur
        List<Task> tasks = taskRepository.findByUser_Id(userId);

        return tasks.stream()
                // Tâches non terminées
                .filter(t -> !t.isDone())
                // Ignorer les tâches liées à un événement annulé ou terminé
                .filter(t -> t.getEvent() == null ||
                        !(t.getEvent().getStatus().equalsIgnoreCase("CANCELLED")
                        || t.getEvent().getStatus().equalsIgnoreCase("DONE")))
                // Durée compatible avec le temps disponible
                .filter(t -> t.getEstimatedDuration() <= availableMinutes)
                // Choisir celle avec la priorité la plus haute
                .max(Comparator.comparing(Task::getPriority))
                .orElse(null);
    }
}