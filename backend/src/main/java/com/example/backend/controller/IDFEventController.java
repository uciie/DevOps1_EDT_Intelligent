package com.example.backend.controller;

import com.example.backend.dto.EventDTO;
import com.example.backend.model.Event;
import com.example.backend.model.Location;
import com.example.backend.repository.EventRepository;
import com.example.backend.service.EventService;
import com.example.backend.service.impl.IDFEventService;
import org.springframework.http.HttpStatus;    // NE PAS OUBLIER CET IMPORT
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/events/idf")
// Vérifie bien ton port (3000 ou 5173 ?)
@CrossOrigin(origins = "http://localhost:5173", allowCredentials = "true")
public class IDFEventController {

    private final IDFEventService idfEventService;
    private final EventService eventService;
    private final EventRepository eventRepository;

    // Un seul constructeur pour tout injecter proprement
    public IDFEventController(IDFEventService idfEventService, 
                              EventService eventService, 
                              EventRepository eventRepository) {
        this.idfEventService = idfEventService;
        this.eventService = eventService;
        this.eventRepository = eventRepository;
    }

    // 1. Rechercher des événements
    @GetMapping("/search")
    public List<EventDTO> search(@RequestParam(defaultValue = "informatique") String q) {
        return idfEventService.searchEvents(q);
    }

    // 2. Importer (via ta méthode simplifiée si besoin)
    @PostMapping("/import")
    public ResponseEntity<Event> importToSchedule(@RequestBody EventDTO dto) {
        Event event = new Event();
        event.setSummary(dto.getTitle());
        event.setStartTime(dto.getStart());
        event.setEndTime(dto.getEnd());
        
        Location loc = new Location();
        loc.setName(dto.getLocation());
        loc.setAddress(dto.getAddress());
        event.setLocation(loc);

        return ResponseEntity.ok(eventRepository.save(event));
    }

    // 3. Ajouter via le service (C'est celle-ci qu'on va utiliser pour la synchro Google)
    @PostMapping("/add") // J'ai raccourci le chemin car il y a déjà /api/events/idf au niveau de la classe
    public ResponseEntity<?> addIdfEvent(@RequestBody EventDTO eventDto, @RequestParam Long userId) {
        try {
            Event saved = eventService.addExternalEvent(eventDto, userId);
            return ResponseEntity.ok(saved);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                                 .body("Erreur lors de l'ajout : " + e.getMessage());
        }
    }
}