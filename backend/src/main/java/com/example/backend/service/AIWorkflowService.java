package com.example.backend.service;

import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import java.util.List;
import com.example.backend.service.parser.FileParsingService;
import com.example.backend.repository.TaskRepository;
import com.example.backend.service.impl.AISchedulingService;
import com.example.backend.record.AIPlanningResponse;
import com.example.backend.record.AIProposedTask;
import com.example.backend.model.User;
import java.io.IOException;
import com.example.backend.model.Task;

@Service
public class AIWorkflowService {

    private final FileParsingService fileParsingService;
    private final AISchedulingService aiSchedulingService;
    private final TaskRepository taskRepository; // Ton accès à la DB
    private final ScheduleOptimizerService scheduleOptimizerService;

    public AIWorkflowService(FileParsingService fileParsingService, 
                             AISchedulingService aiSchedulingService, 
                             TaskRepository taskRepository,
                             ScheduleOptimizerService scheduleOptimizerService) {
        this.fileParsingService = fileParsingService;
        this.aiSchedulingService = aiSchedulingService;
        this.taskRepository = taskRepository;
        this.scheduleOptimizerService = scheduleOptimizerService;
    }

    public AIPlanningResponse handleDocumentUpload(String sessionId, MultipartFile file) throws IOException {

        String extractedText = fileParsingService.extractText(file);

        // Intelligence : Analyse par le service du Dev A
        return aiSchedulingService.processRequest(sessionId, extractedText);
    }

    public void saveValidatedTasks(List<AIProposedTask> proposals, User user) {
        List<Task> entities = proposals.stream().map(dto -> {
            Task t = new Task();
            t.setTitle(dto.title());
            t.setEstimatedDuration(dto.durationMinutes());
            // Ici, tu gères aussi les dépendances si ton entité Task le permet
            t.setUser(user);
            return t;
        }).toList();
        taskRepository.saveAll(entities);
    }

    public void finalizeAndSchedule(AIPlanningResponse validatedResponse, User user) {
        List<AIProposedTask> proposals = validatedResponse.tasks();
        saveValidatedTasks(proposals,user);
        
        scheduleOptimizerService.reshuffle(user.getId());
        System.out.println("Tâches sauvegardées. Déclenchement de l'algorithme de placement...");
    }
}