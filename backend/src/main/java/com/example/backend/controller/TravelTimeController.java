package com.example.backend.controller;

import com.example.backend.model.TravelTime;
import com.example.backend.repository.TravelTimeRepository;
import com.example.backend.service.TravelTimeService;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Contrôleur REST pour la gestion des temps de trajet.
 */
@RestController
@RequestMapping("/api/travel-times")
@CrossOrigin(origins = "http://localhost:5173")
public class TravelTimeController {

    private final TravelTimeRepository travelTimeRepository;
    private final TravelTimeService travelTimeService;

    public TravelTimeController(TravelTimeRepository travelTimeRepository,
                               TravelTimeService travelTimeService) {
        this.travelTimeRepository = travelTimeRepository;
        this.travelTimeService = travelTimeService;
    }

    /**
     * Récupère tous les temps de trajet d'un utilisateur.
     *
     * @param userId l'ID de l'utilisateur
     * @return liste des temps de trajet
     */
    @GetMapping("/user/{userId}")
    public ResponseEntity<List<Map<String, Object>>> getTravelTimesByUser(@PathVariable Long userId) {
        List<TravelTime> travelTimes = travelTimeRepository.findByUser_Id(userId);
        List<Map<String, Object>> response = travelTimes.stream()
                .map(this::convertToMap)
                .collect(Collectors.toList());
        return ResponseEntity.ok(response);
    }

    /**
     * Récupère les temps de trajet d'un utilisateur dans une période donnée.
     *
     * @param userId l'ID de l'utilisateur
     * @param start date de début (format ISO: 2024-01-01T10:00:00)
     * @param end date de fin
     * @return liste des temps de trajet dans la période
     */
    @GetMapping("/user/{userId}/period")
    public ResponseEntity<List<Map<String, Object>>> getTravelTimesByUserAndPeriod(
            @PathVariable Long userId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime start,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime end) {
        
        List<TravelTime> travelTimes = travelTimeRepository.findByUser_IdAndStartTimeBetween(userId, start, end);
        List<Map<String, Object>> response = travelTimes.stream()
                .map(this::convertToMap)
                .collect(Collectors.toList());
        return ResponseEntity.ok(response);
    }

    /**
     * Récupère un temps de trajet par son ID.
     *
     * @param id l'ID du temps de trajet
     * @return le temps de trajet trouvé
     */
    @GetMapping("/{id}")
    public ResponseEntity<Map<String, Object>> getTravelTimeById(@PathVariable Long id) {
        return travelTimeRepository.findById(id)
                .map(this::convertToMap)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    /**
     * Supprime un temps de trajet.
     *
     * @param id l'ID du temps de trajet à supprimer
     * @return réponse vide avec status 204
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteTravelTime(@PathVariable Long id) {
        try {
            travelTimeService.deleteTravelTime(id);
            return ResponseEntity.noContent().build();
        } catch (IllegalArgumentException e) {
            return ResponseEntity.notFound().build();
        }
    }

    /**
     * Met à jour l'heure de début d'un temps de trajet.
     *
     * @param id l'ID du temps de trajet
     * @param request contient newStartTime
     * @return le temps de trajet mis à jour
     */
    @PutMapping("/{id}")
    public ResponseEntity<Map<String, Object>> updateTravelTime(
            @PathVariable Long id,
            @RequestBody Map<String, String> request) {
        try {
            LocalDateTime newStartTime = LocalDateTime.parse(request.get("newStartTime"));
            travelTimeService.updateTravelTime(id, newStartTime);
            
            return travelTimeRepository.findById(id)
                    .map(this::convertToMap)
                    .map(ResponseEntity::ok)
                    .orElse(ResponseEntity.notFound().build());
        } catch (Exception e) {
            return ResponseEntity.badRequest().build();
        }
    }

    /**
     * Convertit un TravelTime en Map pour la sérialisation JSON.
     * Évite les problèmes de référence circulaire avec les entités JPA.
     *
     * @param travelTime le temps de trajet à convertir
     * @return une Map représentant le temps de trajet
     */
    private Map<String, Object> convertToMap(TravelTime travelTime) {
        return Map.of(
                "id", travelTime.getId(),
                "fromEventId", travelTime.getFromEvent() != null ? travelTime.getFromEvent().getId() : null,
                "toEventId", travelTime.getToEvent() != null ? travelTime.getToEvent().getId() : null,
                "fromEventSummary", travelTime.getFromEvent() != null ? travelTime.getFromEvent().getSummary() : "",
                "toEventSummary", travelTime.getToEvent() != null ? travelTime.getToEvent().getSummary() : "",
                "startTime", travelTime.getStartTime().toString(),
                "endTime", travelTime.getEndTime().toString(),
                "durationMinutes", travelTime.getDurationMinutes(),
                "distanceKm", travelTime.getDistanceKm() != null ? travelTime.getDistanceKm() : 0.0,
                "mode", travelTime.getMode().toString()
        );
    }

    /**
     * Calcule et crée un temps de trajet (Endpoint requis pour les tests).
     */
    @PostMapping("/calculate")
    public ResponseEntity<Map<String, Object>> calculateTravelTime(@RequestBody Map<String, Object> request) {
        Long fromEventId = ((Number) request.get("fromEventId")).longValue();
        Long toEventId = ((Number) request.get("toEventId")).longValue();
        String modeStr = (String) request.get("mode");
        TravelTime.TransportMode mode = TravelTime.TransportMode.valueOf(modeStr);

        TravelTime createdTravelTime = travelTimeService.calculateAndCreateTravelTime(fromEventId, toEventId, mode);
        
        // Convert to map to match the JSON structure expected by the test
        return ResponseEntity.ok(convertToMap(createdTravelTime));
    }
}