package com.example.backend.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.beans.factory.annotation.Autowired;

import com.example.backend.service.AIWorkflowService;
import com.example.backend.record.AIPlanningResponse;
import com.example.backend.model.User;
import com.example.backend.repository.UserRepository;

import java.io.IOException;

@RestController
@RequestMapping("/api/focus/ai")
@CrossOrigin(originPatterns = "*", allowCredentials = "true")
public class UploadDocController {

    private final AIWorkflowService workflowService;
    private final UserRepository userRepository; // Pour récupérer l'objet User via l'ID

    @Autowired
    public UploadDocController(AIWorkflowService workflowService, UserRepository userRepository) {
        this.workflowService = workflowService;
        this.userRepository = userRepository;
    }

    // 1. L'utilisateur envoie son PDF
    @PostMapping("/upload")
    public ResponseEntity<AIPlanningResponse> uploadDocument(
            @RequestParam("file") MultipartFile file,
            @RequestParam("sessionId") String sessionId) {
        try {
            AIPlanningResponse response = workflowService.handleDocumentUpload(sessionId, file);
            return ResponseEntity.ok(response);
        } catch (IOException e) {
            return ResponseEntity.internalServerError().build();
        }
    }

    // 2. L'utilisateur demande des modifications par chat
    @PostMapping("/feedback")
    public ResponseEntity<AIPlanningResponse> sendFeedback(
            @RequestParam("sessionId") String sessionId,
            @RequestBody String userComment) {
        
        AIPlanningResponse updatedResponse = workflowService.handleChatFeedback(sessionId, userComment);
        return ResponseEntity.ok(updatedResponse);
    }

    // 3. L'utilisateur valide tout : on enregistre et on reshuffle
    @PostMapping("/validate")
    public ResponseEntity<String> validateAndSchedule(
            @RequestBody AIPlanningResponse finalResponse,
            @RequestParam("userId") Long userId) {
        
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("Utilisateur non trouvé"));

        workflowService.finalizeAndSchedule(finalResponse, user);
        
        return ResponseEntity.ok("Planning mis à jour et optimisé !");
    }
}