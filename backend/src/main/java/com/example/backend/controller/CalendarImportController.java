package com.example.backend.controller;

import com.example.backend.service.CalendarImportService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/import")
public class CalendarImportController {

    private final CalendarImportService importService;

    public CalendarImportController(CalendarImportService importService) {
        this.importService = importService;
    }

    @PostMapping
    public ResponseEntity<String> importCalendar(@RequestParam("file") MultipartFile file) {
        try {
            int count = importService.importCalendar(file);
            return ResponseEntity.ok(count + " événements importés avec succès.");
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Erreur : " + e.getMessage());
        }
    }
}

