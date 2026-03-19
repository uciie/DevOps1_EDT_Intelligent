package com.example.backend.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.beans.factory.annotation.Autowired;

import com.example.backend.service.parser.FileParsingService;
import com.example.backend.service.impl.AISchedulingService;
import com.example.backend.record.AIPlanningResponse;

import java.io.IOException;

@RestController
@RequestMapping("/api/focus/ai")
@CrossOrigin(origins = "*") // Permet au Front-end de communiquer avec le Back-end
public class UploadDocController {

    // On déclare les services (La caisse à outils)
    private final FileParsingService fileParserService;
    private final AISchedulingService aiSchedulingService;

    // On les injecte via le constructeur (C'est la méthode la plus propre en Spring)
    @Autowired
    public UploadDocController(FileParsingService fileParserService, AISchedulingService aiSchedulingService) {
        this.fileParserService = fileParserService;
        this.aiSchedulingService = aiSchedulingService;
    }

    @PostMapping("/upload")
    public ResponseEntity<AIPlanningResponse> uploadDocument(
            @RequestParam("file") MultipartFile file,
            @RequestParam("sessionId") String sessionId) { // On ajoute le sessionId pour le Dev A
        
        try {
            // 1. Ton job (B) : Extraire le texte brut du fichier
            String documentContent = fileParserService.extractText(file);
            
            // 2. Envoyer au "Cerveau" (Service du Développeur A)
            // On utilise la méthode processRequest que ton pote a codée
            AIPlanningResponse response = aiSchedulingService.processRequest(sessionId, documentContent);
            
            return ResponseEntity.ok(response);
            
        } catch (IOException e) {
            // On gère l'erreur si le fichier est corrompu ou illisible
            return ResponseEntity.internalServerError().build();
        } catch (IllegalArgumentException e) {
            // On gère l'erreur si le format n'est pas le bon (ex: une image)
            return ResponseEntity.badRequest().build();
        }
    }

    @PostMapping("/feedback")
    public ResponseEntity<AIPlanningResponse> sendFeedback(
        @RequestParam("sessionId") String sessionId,
        @RequestBody String userComment) {
    
        AIPlanningResponse updatedResponse = aiSchedulingService.processRequest(sessionId, userComment);
    
    return ResponseEntity.ok(updatedResponse);
}
}