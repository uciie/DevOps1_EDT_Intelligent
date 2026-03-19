package com.example.backend.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam; // Import important
import org.springframework.web.bind.annotation.RestController;

import com.example.backend.record.AIPlanningResponse;
import com.example.backend.service.impl.AISchedulingService;

@RestController
public class AITestController {

    @Autowired
    private AISchedulingService aiSchedulingService;

    // On ajoute @RequestParam pour capturer le texte après le "?" dans l'URL
    @GetMapping("/test-ai")
    public AIPlanningResponse test(@RequestParam(value = "prompt", defaultValue = "Planifier ma journée") String prompt) {
        // On utilise maintenant la variable 'prompt' au lieu du texte en dur
        return aiSchedulingService.processRequest("test-session", prompt);
    }
}