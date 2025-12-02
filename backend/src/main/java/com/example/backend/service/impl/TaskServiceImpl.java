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

// Dans TaskServiceImpl.java

@Override
public Task planifyTask(Long taskId, LocalDateTime start, LocalDateTime end) {
    Task task = taskRepository.findById(taskId)
            .orElseThrow(() -> new IllegalArgumentException("Tâche non trouvée"));

    User user = task.getUser(); // On sait déjà que l'utilisateur est présent grâce à la vérification
    if (user == null) {
        throw new IllegalStateException("Impossible de planifier une tâche sans utilisateur associé");
    }

    // --- NOUVELLE LOGIQUE : ALLOCATION SIMPLE AU PREMIER TROU ---
    if (start == null || end == null) {
        // 1. Définir la durée requise pour la tâche
        long durationMinutes = task.getEstimatedDuration(); 
        
        // 2. Récupérer et trier les événements existants par heure de début
        // Vous devez implémenter cette méthode de récupération par utilisateur dans EventRepository ou EventService
        List<Event> userEvents = eventRepository.findByUser_IdOrderByStartTime(user.getId());
        
        // --- Algorithme "First-Fit" (Premier Trou) ---
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime currentCheckTime = now.plusMinutes(10); // Commence à vérifier dans 10 minutes (ou dès maintenant)

        // Itérer sur les événements existants
        for (Event existingEvent : userEvents) {
            
            // Si le temps entre la fin du créneau actuel et le début du prochain événement 
            // est suffisant pour notre tâche.
            if (currentCheckTime.plusMinutes(durationMinutes).isBefore(existingEvent.getStartTime())) {
                start = currentCheckTime;
                end = currentCheckTime.plusMinutes(durationMinutes);
                break; // Créneau trouvé, sortir de la boucle
            }
            
            // Sinon, avancer le point de vérification après la fin de l'événement existant
            currentCheckTime = existingEvent.getEndTime();
        }

        // Si aucun trou n'a été trouvé (c'est-à-dire que le créneau est à la suite du dernier événement)
        if (start == null) {
            start = currentCheckTime; // Placer la tâche immédiatement après le dernier événement
            end = currentCheckTime.plusMinutes(durationMinutes);
        }
    }
    // -------------------------------------------------------------

    System.out.println("DEBUG PLANIFICATION: Tâche ID " + taskId + 
                       " planifiée. Start: " + start + 
                       ", End: " + end);
                       
    // Crée un nouvel événement à partir de la tâche (utilise les 'start' et 'end' trouvés ou fournis)
    Event event = new Event(
            task.getTitle(),
            start,
            end,
            user // Utilisez l'objet User déjà récupéré
    );

    // Save the event first so it has an ID
    event = eventRepository.save(event);

    // Lier l’événement à la tâche
    task.setEvent(event);
    
    return taskRepository.save(task);
}
}
