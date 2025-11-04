package com.example.backend.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import com.example.backend.scheduler.service.ScheduleOptimizerService;

@RestController
@RequestMapping("/api/schedule")
public class ScheduleController {

    private final ScheduleOptimizerService optimizerService;

    public ScheduleController(ScheduleOptimizerService optimizerService) {
        this.optimizerService = optimizerService;
    }

    @PostMapping("/reshuffle/{eventId}")
    public ResponseEntity<String> reshuffle(@PathVariable Long eventId) {
        optimizerService.reshuffle(eventId);
        return ResponseEntity.ok("Schedule updated successfully.");
    }
}
