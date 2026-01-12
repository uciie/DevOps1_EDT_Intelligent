package com.example.backend.controller;

import com.example.backend.dto.TimeSlot;
import com.example.backend.service.impl.FocusService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/focus")
@CrossOrigin(origins = "*") // Pour permettre au Frontend de communiquer sans erreurs CORS
public class FocusController {

    @Autowired
    private FocusService focusService;

    /**
     * Endpoint pour récupérer les suggestions de Deep Work
     * URL exemple : GET /api/focus/suggestions?userId=1&date=2024-10-25
     */
    @GetMapping("/suggestions")
    public ResponseEntity<List<TimeSlot>> getFocusSuggestions(
            @RequestParam Long userId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        
        List<TimeSlot> suggestions = focusService.getOptimizedFocusSlots(userId, date);
        return ResponseEntity.ok(suggestions);
    }
}