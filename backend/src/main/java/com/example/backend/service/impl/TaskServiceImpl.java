package com.example.backend.service.impl;

import com.example.backend.model.Event;
import com.example.backend.model.Task;
import com.example.backend.repository.EventRepository;
import com.example.backend.repository.TaskRepository;
import com.example.backend.repository.UserRepository;
import com.example.backend.service.TaskService;
import org.springframework.stereotype.Service;
import com.example.backend.model.User; 
import com.example.backend.repository.UserRepository;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class TaskServiceImpl implements TaskService {

    private final TaskRepository taskRepository;
    private final EventRepository eventRepository;
    private final UserRepository userRepository;

    public TaskServiceImpl(TaskRepository taskRepository, EventRepository eventRepository,UserRepository userRepository) {
        this.taskRepository = taskRepository;
        this.eventRepository = eventRepository;
        this.userRepository = userRepository;
    }

    @Override
    public List<Task> getTasksByUserId(Long userId) {
        return taskRepository.findAll()
                .stream()
                .filter(t -> t.getUser() != null && t.getUser().getId().equals(userId))
                .toList();
    }

    @Override
    public Task createTask(Task task, Long userId) { // <-- Implemente la nouvelle signature
        // 1. Chercher l'utilisateur complet en BDD en utilisant l'ID
        // userRepository.findById(userId) renvoie un Optional<User>
        User user = userRepository.findById(userId)
            // Si l'utilisateur n'est pas trouvé, on lance une exception HTTP 400 ou 500
            .orElseThrow(() -> new IllegalArgumentException("Utilisateur non trouvé avec l'ID: " + userId)); 

        // 2. Lier l'objet User trouvé à l'objet Task reçu du client
        task.setUser(user);

        // 3. Logique métier
        task.setDone(false);

        // 4. Sauvegarder la tâche (maintenant la colonne user_id n'est plus NULL)
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