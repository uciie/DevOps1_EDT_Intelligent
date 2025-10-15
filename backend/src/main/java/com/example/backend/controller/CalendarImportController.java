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

    /**
     * Gère l'importation d'un fichier de calendrier et traite ses événements.
     *
     * @param file le fichier de calendrier téléchargé à importer, fourni en tant que 
     *             {@link MultipartFile}.
     * @return un {@link ResponseEntity} contenant un message de succès avec le 
     *         nombre d'événements importés, ou un message d'erreur en cas d'échec.
     */
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

