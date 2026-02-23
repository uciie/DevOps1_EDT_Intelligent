package com.example.backend.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import com.example.backend.record.AIPlanningResponse;
import com.example.backend.service.impl.AISchedulingService;

@RestController
public class AITestController {
    @Autowired
    private AISchedulingService aiSchedulingService;

    @GetMapping("/test-ai")
    public AIPlanningResponse test() {
        return aiSchedulingService.processRequest("test-session", "Je dois ranger ma chambre (15min) puis passer l'aspirateur (10min).");
    }
}