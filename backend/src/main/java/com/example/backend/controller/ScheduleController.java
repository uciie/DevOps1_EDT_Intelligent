package com.example.backend.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.example.backend.service.ScheduleOptimizerService;

/**
 * Contrôleur pour la gestion de la planification.
 * Fournit des points de terminaison pour optimiser et mettre à jour l'emploi du temps.
 */
@RestController
@RequestMapping("/api/schedule")
public class ScheduleController {

    private final ScheduleOptimizerService optimizerService;

    /**
     * Construit un nouveau ScheduleController avec le service d'optimisation de planification donné.
     *
     * @param optimizerService le service à utiliser pour l'optimisation de la planification.
     */
    public ScheduleController(ScheduleOptimizerService optimizerService) {
        this.optimizerService = optimizerService;
    }

    /**
     * Réorganise l'emploi du temps en fonction d'un événement annulé.
     *
     * @param eventId l'ID de l'événement annulé.
     * @return une ResponseEntity indiquant le succès de l'opération.
     */
    @PostMapping("/reshuffle/{eventId}")
    public ResponseEntity<String> reshuffle(@PathVariable Long eventId) {
        optimizerService.reshuffle(eventId);
        return ResponseEntity.ok("Schedule updated successfully.");
    }
}
