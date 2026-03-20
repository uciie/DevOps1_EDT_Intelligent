package com.example.backend.service;

import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import java.util.List;
import com.example.backend.service.parser.FileParsingService;
import com.example.backend.service.impl.AISchedulingService;
import com.example.backend.record.AIPlanningResponse;
import com.example.backend.record.AIProposedTask;
import com.example.backend.model.User;
import com.example.backend.model.Task;
import java.io.IOException;

@Service
public class AIWorkflowService {

    private final FileParsingService fileParsingService;
    private final AISchedulingService aiSchedulingService;
    private final TaskService taskService;
    private final ScheduleOptimizerService scheduleOptimizerService;

    public AIWorkflowService(FileParsingService fileParsingService, 
                             AISchedulingService aiSchedulingService, 
                             TaskService taskService, // Injection ici
                             ScheduleOptimizerService scheduleOptimizerService) {
        this.fileParsingService = fileParsingService;
        this.aiSchedulingService = aiSchedulingService;
        this.taskService = taskService;
        this.scheduleOptimizerService = scheduleOptimizerService;
    }

    /**
     * ÉTAPE 1 : Extraire le texte du PDF et obtenir la première proposition de l'IA.
     */
    public AIPlanningResponse handleDocumentUpload(String sessionId, MultipartFile file) throws IOException {
        String extractedText = fileParsingService.extractText(file);
        // CORRECTION : On enlève sessionId
        return aiSchedulingService.processRequest(extractedText);
    }
    /**
     * ÉTAPE 1 BIS : Gérer les retours de l'utilisateur (Chat continu).
     */
    public AIPlanningResponse handleChatFeedback(String sessionId, String userComment) {
        // CORRECTION : On enlève sessionId
        return aiSchedulingService.processRequest(userComment);
    }

    /**
     * ÉTAPE 2 : Transformer les records immuables en entités JPA persistantes.
     */
    public void saveValidatedTasks(List<AIProposedTask> proposals, User user) {
        for (AIProposedTask dto : proposals) {
            Task t = new Task();
            t.setTitle(dto.title());
            t.setEstimatedDuration(dto.durationMinutes());
            t.setPriority(2); // Valeur par défaut
            
            // On appelle la méthode officielle du projet
            // Cela va gérer le Status, l'Assignee, et les contraintes DB proprement
            taskService.createTask(t, user.getId());
        }
    }

    public void finalizeAndSchedule(AIPlanningResponse validatedResponse, User user) {
        saveValidatedTasks(validatedResponse.tasks(), user);
        scheduleOptimizerService.reshuffle(user.getId());
    }
}