package com.example.backend.service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;

import com.example.backend.model.ChatMessage;
import com.example.backend.model.Event;
import com.example.backend.model.Task;
import com.example.backend.model.User;
import com.example.backend.repository.ChatMessageRepository;
import com.example.backend.repository.EventRepository;
import com.example.backend.repository.TaskRepository;
import com.example.backend.repository.UserRepository;

@Service
public class ChatbotService {

    private final GeminiService geminiService;
    private final ChatMessageRepository chatMessageRepository;
    private final EventRepository eventRepository;
    private final TaskRepository taskRepository;
    private final UserRepository userRepository;

    public ChatbotService(GeminiService geminiService, 
                          ChatMessageRepository chatMessageRepository,
                          EventRepository eventRepository, 
                          TaskRepository taskRepository, 
                          UserRepository userRepository) {
        this.geminiService = geminiService;
        this.chatMessageRepository = chatMessageRepository; // Injection
        this.eventRepository = eventRepository;
        this.taskRepository = taskRepository;
        this.userRepository = userRepository;
    }

    public String handleUserRequest(Long userId, String message) {
    List<ChatMessage> history = chatMessageRepository.findTop10ByUserIdOrderByTimestampDesc(userId);
    GeminiService.GeminiResponse response = geminiService.chatWithGemini(message, history);

    // Sauvegarde du message utilisateur
    chatMessageRepository.save(new ChatMessage(userId, "user", message));

    // Vérification en cascade pour éviter les NPE
    if (response != null && response.candidates() != null && !response.candidates().isEmpty()) {
        var content = response.candidates().get(0).content();
        if (content != null && content.parts() != null && !content.parts().isEmpty()) {
            GeminiService.Part firstPart = content.parts().get(0);
            
            String replyText;
            if (firstPart.functionCall() != null) {
                replyText = executeAction(userId, firstPart.functionCall().name(), firstPart.functionCall().args());
            } else {
                replyText = firstPart.text() != null ? firstPart.text() : "L'IA n'a pas renvoyé de texte.";
            }
            
            chatMessageRepository.save(new ChatMessage(userId, "model", replyText));
            return replyText;
        }
    }
    return "Désolé, je rencontre une difficulté technique pour formuler une réponse.";
}
    
    private String executeAction(Long userId, String functionName, Map<String, Object> args) {
        switch (functionName) {
            case "list_today_events":
                return formatEvents(eventRepository.findByUser_IdAndStartTimeBetween(userId, 
                    LocalDate.now().atStartOfDay(), LocalDate.now().atTime(23, 59, 59)));

            case "list_events_between":
                return formatEvents(eventRepository.findByUser_IdAndStartTimeBetween(userId, 
                    parseDateTime(args.get("start")), parseDateTime(args.get("end"))));

            case "list_all_tasks":
                return formatTasks(taskRepository.findByUser_Id(userId));

            case "list_tasks_by_priority":
                int priorityList = Integer.parseInt(args.get("priority").toString());
                return formatTasks(taskRepository.findByUser_IdAndPriority(userId, priorityList));

            case "add_task":
                Task task = new Task();
                User user = userRepository.findById(userId).orElseThrow();
                task.setUser(user);
                Object titleObj = args.get("title");
                if (titleObj == null) return "Erreur : titre manquant";
                task.setTitle(args.get("title").toString());
                task.setPriority(Integer.parseInt(args.get("priority").toString()));
                taskRepository.save(task);
                return "Tâche '" + task.getTitle() + "' ajoutée.";

            case "add_event":
                User eventUser = userRepository.findById(userId).orElseThrow();
                String summary = args.get("summary").toString();
                LocalDateTime s = parseDateTime(args.get("start"));
                LocalDateTime e = parseDateTime(args.get("end"));
                Event newEvent = new Event(summary, s, e, eventUser);
                eventRepository.save(newEvent);
                return "Événement '" + newEvent.getSummary() + "' ajouté.";

            case "cancel_morning":
                // Conversion de la date chaîne en LocalDate
                LocalDate dateM = LocalDate.parse(args.get("date").toString()); 
                return deleteEventsInRange(userId, dateM.atTime(8, 0), dateM.atTime(11, 0), "le matin du " + dateM);
            
            case "cancel_noon": 
                LocalDate dateN = LocalDate.parse(args.get("date").toString());
                return deleteEventsInRange(userId, dateN.atTime(12,0), dateN.atTime(14,0), "le midi du " + dateN);

            case "cancel_afternoon":
                LocalDate dateA = LocalDate.parse(args.get("date").toString());
                return deleteEventsInRange(userId, dateA.atTime(15, 0), dateA.atTime(18, 0), "l'après-midi du " + dateA);
            
            case "cancel_evening":
                LocalDate dateE = LocalDate.parse(args.get("date").toString());
                return deleteEventsInRange(userId,dateE.atTime(19,0), dateE.atTime(23,0), "le soir du " + dateE);
                
            case "move_activity":
                Long id = Long.valueOf(args.get("id").toString());
                Event event = eventRepository.findById(id).orElseThrow();
                event.setStartTime(parseDateTime(args.get("newStart")));
                eventRepository.save(event);
                return "Activité déplacée au " + args.get("newStart");

            case "cancel_tasks_by_priority":
                int priorityToCancel = Integer.parseInt(args.get("priority").toString());
                taskRepository.deleteByUser_IdAndPriority(userId, priorityToCancel);
                return "Tâches de priorité " + priorityToCancel + " supprimées.";
            
            case "find_task_by_name":
                // Gère "Récupérer une tache en fonction de son nom"
                String nameQuery = args.get("name").toString();
                return formatTasks(taskRepository.findByUser_IdAndTitleContainingIgnoreCase(userId, nameQuery));
            case "add_events_batch":
                return handleEventsBatch(userId, args);

            default:
                return "Action " + functionName + " exécutée, mais aucun retour spécifique configuré.";
        }
    }
    private String handleEventsBatch(Long userId, Map<String, Object> args) {
    // 1. Récupérer la liste des événements depuis les arguments
    List<Map<String, Object>> eventsData = (List<Map<String, Object>>) args.get("events");
    
    if (eventsData == null || eventsData.isEmpty()) {
        return "Je n'ai reçu aucun événement à ajouter.";
    }

    // 2. Récupérer l'utilisateur
    User user = userRepository.findById(userId)
            .orElseThrow(() -> new RuntimeException("Utilisateur non trouvé"));

    int count = 0;
    for (Map<String, Object> data : eventsData) {
        try {
            String summary = data.get("summary").toString();
            LocalDateTime start = parseDateTime(data.get("start"));
            LocalDateTime end = parseDateTime(data.get("end"));
            
            // Note : Si votre entité Event ne gère pas encore la catégorie, 
            // ignorez data.get("category") pour l'instant.
            Event event = new Event(summary, start, end, user);
            eventRepository.save(event);
            count++;
        } catch (Exception e) {
            System.err.println("Erreur lors de l'ajout d'un événement du batch : " + e.getMessage());
        }
    }

    return String.format("Succès ! J'ai ajouté %d événements à votre agenda.", count);
}
    private String deleteEventsInRange(Long userId, LocalDateTime start, LocalDateTime end, String label) {
        List<Event> toDelete = eventRepository.findByUser_IdAndStartTimeBetween(userId, start, end);
        if (toDelete.isEmpty()) return "Rien à annuler pour " + label + ".";
        eventRepository.deleteAll(toDelete);
        return "J'ai annulé " + toDelete.size() + " événements pour " + label + ".";
    }

    private LocalDateTime parseDateTime(Object val) {
        return LocalDateTime.parse(val.toString());
    }

    private String formatEvents(List<Event> events) {
        if (events.isEmpty()) return "Aucun événement trouvé.";
        return events.stream()
                .map(e -> e.getSummary() + " (" + e.getStartTime().toLocalTime() + ")")
                .collect(Collectors.joining("\n"));
    }

    private String formatTasks(List<Task> tasks) {
        if (tasks.isEmpty()) return "Aucune tâche à faire.";
        return tasks.stream()
                .map(t -> "- " + t.getTitle() + " [Prio: " + t.getPriority() + "]")
                .collect(Collectors.joining("\n"));
    }
}