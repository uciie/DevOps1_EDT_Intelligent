package com.example.backend.service.impl;

import com.example.backend.model.Event;
import com.example.backend.model.Task;
import com.example.backend.repository.EventRepository;
import com.example.backend.repository.TaskRepository;
import com.example.backend.service.TaskService;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class TaskServiceImpl implements TaskService {

    private final TaskRepository taskRepository;
    private final EventRepository eventRepository;

    public TaskServiceImpl(TaskRepository taskRepository, EventRepository eventRepository) {
        this.taskRepository = taskRepository;
        this.eventRepository = eventRepository;
    }

    @Override
    public List<Task> getTasksByUserId(Long userId) {
        return taskRepository.findAll()
                .stream()
                .filter(t -> t.getUser() != null && t.getUser().getId().equals(userId))
                .toList();
    }

    @Override
    public Task createTask(Task task) {
        task.setDone(false);
        return taskRepository.save(task);
    }

    @Override
    public Task updateTask(Long id, Task updatedTask) {
        Task existing = taskRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Tâche non trouvée"));

        existing.setTitle(updatedTask.getTitle());
        existing.setEstimatedDuration(updatedTask.getEstimatedDuration());
        existing.setPriority(updatedTask.getPriority());
        existing.setDone(updatedTask.isDone());
        return taskRepository.save(existing);
    }

    @Override
    public void deleteTask(Long id) {
        taskRepository.deleteById(id);
    }

    @Override
    public Task planifyTask(Long taskId, LocalDateTime start, LocalDateTime end) {
        Task task = taskRepository.findById(taskId)
                .orElseThrow(() -> new IllegalArgumentException("Tâche non trouvée"));

        if (task.getUser() == null) {
            throw new IllegalStateException("Impossible de planifier une tâche sans utilisateur associé");
        }

        // Crée un nouvel événement à partir de la tâche
        Event event = new Event(
                task.getTitle(),
                start,
                end,
                task.getUser()
        );

        // Ajout d'une tâche à la liste des tâches de l’événement
        event.getTasks().add(task); // Access the list and add to it
        
        // Save the event first so it has an ID
        event = eventRepository.save(event);

        // Lier l’événement à la tâche (update the task side of the relationship)
        task.setEvent(event);
        
        return taskRepository.save(task);
    }
}