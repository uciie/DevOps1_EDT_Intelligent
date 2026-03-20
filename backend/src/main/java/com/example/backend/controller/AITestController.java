package com.example.backend.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping; 
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.example.backend.record.AIPlanningResponse;
import com.example.backend.record.AIHistoryRequest; 
import com.example.backend.service.impl.AISchedulingService;

@RestController
@RequestMapping("/api")
public class AITestController {

    @Autowired
    private AISchedulingService aiSchedulingService;

    // Premier appel : L'utilisateur demande un planning à partir de zéro
    @GetMapping("/test-ai")
    public AIPlanningResponse test(@RequestParam(value = "prompt", defaultValue = "Planifier ma journée") String prompt) {
        // CORRECTION : On enlève "test-session"
        return aiSchedulingService.processRequest(prompt);
    }

    // Boucle d'échange : L'utilisateur n'est pas satisfait et renvoie l'existant + sa critique
    @PostMapping("/refine-ai")
    public AIPlanningResponse refine(@RequestBody AIHistoryRequest request) {
        // On délègue au service la logique de "raffinage"
        return aiSchedulingService.refineRequest(request);
    }
}