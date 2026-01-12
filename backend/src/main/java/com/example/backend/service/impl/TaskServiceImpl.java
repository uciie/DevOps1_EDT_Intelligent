package com.example.backend.service.impl;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.springframework.stereotype.Service;

import com.example.backend.model.Event;
import com.example.backend.model.Task;
import com.example.backend.model.Team; 
import com.example.backend.model.User;
import com.example.backend.repository.EventRepository;
import com.example.backend.repository.TaskRepository;
import com.example.backend.repository.TeamRepository; 
import com.example.backend.repository.UserRepository;
import com.example.backend.service.TaskService;

import jakarta.transaction.Transactional;

@Service
public class TaskServiceImpl implements TaskService {

    private final TaskRepository taskRepository;
    private final EventRepository eventRepository;
    private final UserRepository userRepository;
    private final TeamRepository teamRepository;

    public TaskServiceImpl(TaskRepository taskRepository, EventRepository eventRepository, UserRepository userRepository, TeamRepository teamRepository) {
        this.taskRepository = taskRepository;
        this.eventRepository = eventRepository;
        this.userRepository = userRepository;
        this.teamRepository = teamRepository;
    }

    
    @Override
    public List<Task> getTasksByTeam(Long teamId) {
        // Cette méthode existe déjà dans votre TaskRepository (voir fichier fourni)
        return taskRepository.findByTeamId(teamId);
    }

    // --- RM-04 : FILTRES ---

    @Override
    public List<Task> getTasksByUserId(Long userId) {
        User user = userRepository.findById(userId).orElse(null);
        if (user == null) return new ArrayList<>();

        List<Task> createdTasks = taskRepository.findByUser_Id(userId); 
        
        List<Task> assignedTasks = taskRepository.findByAssignee(user);

        Set<Task> uniqueTasks = new HashSet<>(createdTasks);
        uniqueTasks.addAll(assignedTasks);

        return new ArrayList<>(uniqueTasks);
    }

    @Override
    public List<Task> getTasksAssignedToUser(Long userId) {
        // Utilise la méthode repository créée à l'étape 4 : findByAssignee
        User user = userRepository.findById(userId).orElse(null);
        if (user == null) return List.of();
        return taskRepository.findByAssignee(user);
    }

    @Override
    public List<Task> getDelegatedTasks(Long creatorId) {
        // Utilise la méthode repository créée à l'étape 4 : findByUserAndAssigneeNot
        User creator = userRepository.findById(creatorId).orElseThrow();
        // On cherche les tâches créées par moi, mais assignées à quelqu'un d'autre (ou null)
        // Note: Assurez-vous que findByUserAndAssigneeNot est bien dans TaskRepository
        return taskRepository.findByUserAndAssigneeNot(creator, creator);
    }

    @Override
    public Task getTaskById(Long id) {
        return taskRepository.findById(id)
            .orElseThrow(() -> new IllegalArgumentException("Tâche introuvable avec l'ID : " + id));
    }

    // --- RM-02 : CREATION & ASSIGNATION ---

    @Override
    @Transactional
    public Task createTask(Task task, Long creatorId) {
        User creator = userRepository.findById(creatorId)
            .orElseThrow(() -> new IllegalArgumentException("Utilisateur non trouvé avec l'ID: " + creatorId));

        task.setUser(creator);

        User assigneeCandidate; 

        if (task.getAssignee() == null) {
            assigneeCandidate = creator;
        } else {
            // Récupération de l'utilisateur assigné depuis la BDD
            assigneeCandidate = userRepository.findById(task.getAssignee().getId())
                    .orElseThrow(() -> new IllegalArgumentException("Utilisateur assigné introuvable"));
        }
        
        // On affecte l'assigné validé à la tâche
        task.setAssignee(assigneeCandidate);

        // --- Gestion de l'Équipe ---
        if (task.getTeam() != null && task.getTeam().getId() != null) {
            Team team = teamRepository.findById(task.getTeam().getId())
                    .orElseThrow(() -> new IllegalArgumentException("Équipe introuvable"));
            
            // On peut maintenant utiliser 'assigneeCandidate' ici sans erreur
            boolean isMember = team.getMembers().contains(assigneeCandidate);
            
            // Vérification : l'assigné doit être membre ou propriétaire
            if (!isMember && !assigneeCandidate.getId().equals(team.getOwnerId())) { 
                 throw new IllegalArgumentException("L'utilisateur assigné ne fait pas partie de l'équipe sélectionnée.");
            }
            
            task.setTeam(team); 
        } else {
            // Logique de repli : vérification d'équipe commune
            boolean sameTeam = creator.getTeams().stream()
                    .anyMatch(t -> t.getMembers().contains(assigneeCandidate)); // 'assigneeCandidate' est visible ici

            if (!sameTeam && !creator.equals(assigneeCandidate)) {
                throw new IllegalArgumentException("Vous ne pouvez assigner une tâche qu'aux membres de vos équipes.");
            }
        }

        task.setDone(false);
        return taskRepository.save(task);
    }
    // --- RM-03 : DROITS DE MODIFICATION ---

    @Override
    @Transactional
    public Task updateTask(Long taskId, Task updatedTask, Long currentUserId) {
        Task existing = taskRepository.findById(taskId)
                .orElseThrow(() -> new IllegalArgumentException("Tâche non trouvée"));

        boolean isCreator = existing.getUser().getId().equals(currentUserId);
        boolean isAssignee = existing.getAssignee().getId().equals(currentUserId);

        if (isCreator) {
            // LE CRÉATEUR peut tout modifier : Titre, Durée, Priorité, Assigné
            existing.setTitle(updatedTask.getTitle());
            existing.setEstimatedDuration(updatedTask.getEstimatedDuration());
            existing.setPriority(updatedTask.getPriority());
            existing.setDone(updatedTask.isDone());
            
            // Réassignation possible par le créateur
            if (updatedTask.getAssignee() != null) {
                 // On pourrait refaire la validation d'équipe ici
                 existing.setAssignee(updatedTask.getAssignee());
            }
            
            // Mise à jour date/deadline si présentes
            if (updatedTask.getDeadline() != null) existing.setDeadline(updatedTask.getDeadline());

        } else if (isAssignee) {
            // L'ASSIGNÉ peut modifier uniquement le statut (Done, Late)
            // On ignore silencieusement les autres changements ou on lance une erreur ? 
            // Ici on applique uniquement les changements autorisés.
            existing.setDone(updatedTask.isDone());
            existing.setLate(updatedTask.isLate());
            
        } else {
            // Ni créateur ni assigné -> Accès refusé
            throw new SecurityException("Vous n'avez pas les droits pour modifier cette tâche.");
        }

        return taskRepository.save(existing);
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

    // --- ALLOCATION SIMPLE AU PREMIER TROU ---
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
