package com.example.backend.controller;

import com.example.backend.service.CalendarSyncService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/sync")
@CrossOrigin(origins = "http://localhost:5173")
public class SyncController {

    private final CalendarSyncService syncService;

    public SyncController(CalendarSyncService syncService) {
        this.syncService = syncService;
    }

    /**
     * Déclenche une synchronisation manuelle pour un utilisateur
     */
    @PostMapping("/user/{userId}")
    public ResponseEntity<String> syncUser(@PathVariable Long userId) {
        try {
            syncService.syncUser(userId);
            return ResponseEntity.ok("Synchronisation réussie !");
        } catch (Exception e) {
            return ResponseEntity.status(500).body("Erreur : " + e.getMessage());
        }
    }
}