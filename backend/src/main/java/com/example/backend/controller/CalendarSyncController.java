package com.example.backend.controller;

import com.example.backend.model.User;
import com.example.backend.repository.UserRepository;
import com.example.backend.service.CalendarImportService;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/calendar")
public class CalendarSyncController {

    private final UserRepository       userRepository;
    private final CalendarImportService calendarImportService;

    public CalendarSyncController(UserRepository userRepository,
                                  CalendarImportService calendarImportService) {
        this.userRepository       = userRepository;
        this.calendarImportService = calendarImportService;
    }

    /**
     * Force un pull immédiat depuis Google Calendar.
     * Appelé par le frontend via POST /api/calendar/sync/pull/{userId}
     * Le scheduler fait déjà ça toutes les 15 min ;
     * cette route permet de le déclencher à la demande.
     */
    @PostMapping("/sync/pull/{userId}")
    public ResponseEntity<Void> pullNow(@PathVariable Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("Utilisateur introuvable : " + userId));

        calendarImportService.pullEventsFromGoogle(user);

        return ResponseEntity.ok().build();
    }
}