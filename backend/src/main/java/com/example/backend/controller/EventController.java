package com.example.backend.controller;

import com.example.backend.model.Event;
import com.example.backend.model.Location;
import com.example.backend.service.EventService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * Contrôleur REST pour la gestion des événements.
 * Fournit des endpoints pour créer, lire, mettre à jour et supprimer des événements.
 */
@RestController
@RequestMapping("/api/events")
@CrossOrigin(origins = "http://localhost:5173")
public class EventController {

    private final EventService eventService;

    public EventController(EventService eventService) {
        this.eventService = eventService;
    }

    /**
     * Récupère tous les événements d'un utilisateur.
     *
     * @param userId l'ID de l'utilisateur
     * @return liste des événements
     */
    @GetMapping("/user/{userId}")
    public ResponseEntity<List<Event>> getEventsByUser(@PathVariable Long userId) {
        List<Event> events = eventService.getEventsByUserId(userId);
        return ResponseEntity.ok(events);
    }

    /**
     * Récupère les événements d'un utilisateur dans une période donnée.
     *
     * @param userId l'ID de l'utilisateur
     * @param start date de début (ISO format)
     * @param end date de fin (ISO format)
     * @return liste des événements dans la période
     */
    @GetMapping("/user/{userId}/period")
    public ResponseEntity<List<Event>> getEventsByUserAndPeriod(
            @PathVariable Long userId,
            @RequestParam String start,
            @RequestParam String end) {
        LocalDateTime startDate = LocalDateTime.parse(start);
        LocalDateTime endDate = LocalDateTime.parse(end);
        List<Event> events = eventService.getEventsByUserIdAndPeriod(userId, startDate, endDate);
        return ResponseEntity.ok(events);
    }

    /**
     * Récupère un événement par son ID.
     *
     * @param id l'ID de l'événement
     * @return l'événement trouvé
     */
    @GetMapping("/{id}")
    public ResponseEntity<?> getEventById(@PathVariable Long id) {
        try {
            Event event = eventService.getEventById(id);
            return ResponseEntity.ok(event);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(e.getMessage());
        }
    }

    /**
     * Crée un nouvel événement.
     *
     * @param eventRequest les données de l'événement à créer
     * @return l'événement créé avec les temps de trajet calculés
     */
    @PostMapping
    public ResponseEntity<?> createEvent(@RequestBody EventRequest eventRequest) {
        try {
            Event createdEvent = eventService.createEvent(eventRequest);
            return ResponseEntity.ok(createdEvent);
        } catch (IllegalArgumentException e) {
            // Renvoie une erreur 400 si le trajet est impossible ou les données invalides
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    /**
     * Met à jour un événement existant.
     *
     * @param id l'ID de l'événement à modifier
     * @param eventRequest les nouvelles données
     * @return l'événement mis à jour
     */
    @PutMapping("/{id}")
    public ResponseEntity<Event> updateEvent(
            @PathVariable Long id,
            @RequestBody EventRequest eventRequest) {
        Event updatedEvent = eventService.updateEvent(id, eventRequest);
        return ResponseEntity.ok(updatedEvent);
    }

    /**
     * Supprime un événement.
     *
     * @param id l'ID de l'événement à supprimer
     * @return confirmation de suppression
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<?> deleteEvent(@PathVariable Long id) {
        try {
            eventService.deleteEvent(id);
            return ResponseEntity.ok("Événement supprimé avec succès");
        } catch (IllegalArgumentException e) {
            // L'événement n'existe pas (404)
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(e.getMessage());
        } catch (Exception e) {
            // Erreur Base de données ou autre (500)
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Erreur lors de la suppression : " + e.getMessage());
        }
    }

    /**
     * Recalcule tous les temps de trajet pour un utilisateur donné selon le mode choisi.
     * C'est utile quand l'utilisateur change de préférence (Google Maps vs Simple).
     */
    @PostMapping("/recalculate")
    public ResponseEntity<?> recalculateTravelTimes(
            @RequestParam Long userId,
            @RequestParam Boolean useGoogleMaps) {
        try {
            eventService.recalculateAllTravelTimes(userId, useGoogleMaps);
            return ResponseEntity.ok(Map.of("message", "Temps de trajets recalculés avec succès"));
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Erreur lors du recalcul : " + e.getMessage());
        }
    }

    /**
     * DTO pour la création/modification d'événements.
     */
    public static class EventRequest {
        private String summary;
        private LocalDateTime startTime;
        private LocalDateTime endTime;
        private Long userId;
        private LocationRequest location;
        
        // NOUVEAU CHAMP pour le mode de transport
        private String transportMode;
        
        // MODIFICATION : Ajout du champ pour la préférence utilisateur
        private Boolean useGoogleMaps;

        // Getters et Setters
        public String getSummary() { return summary; }
        public void setSummary(String summary) { this.summary = summary; }

        public LocalDateTime getStartTime() { return startTime; }
        public void setStartTime(LocalDateTime startTime) { this.startTime = startTime; }

        public LocalDateTime getEndTime() { return endTime; }
        public void setEndTime(LocalDateTime endTime) { this.endTime = endTime; }

        public Long getUserId() { return userId; }
        public void setUserId(Long userId) { this.userId = userId; }

        public LocationRequest getLocation() { return location; }
        public void setLocation(LocationRequest location) { this.location = location; }

        public String getTransportMode() { return transportMode; }
        public void setTransportMode(String transportMode) { this.transportMode = transportMode; }

        public Boolean getUseGoogleMaps() { return useGoogleMaps; }
        public void setUseGoogleMaps(Boolean useGoogleMaps) { this.useGoogleMaps = useGoogleMaps; }
    }

    /**
     * DTO pour la localisation dans les requêtes.
     */
    public static class LocationRequest {
        private String address;
        private Double latitude;
        private Double longitude;
        private String name;

        // Getters et Setters
        public String getAddress() { return address; }
        public void setAddress(String address) { this.address = address; }

        public Double getLatitude() { return latitude; }
        public void setLatitude(Double latitude) { this.latitude = latitude; }

        public Double getLongitude() { return longitude; }
        public void setLongitude(Double longitude) { this.longitude = longitude; }

        public String getName() { return name; }
        public void setName(String name) { this.name = name; }

        /**
         * Convertit ce LocationRequest en objet Location.
         * @return un objet Location ou null si les données sont insuffisantes
         */
        public Location toLocation() {
            // Si on a des coordonnées GPS, créer avec GPS
            if (latitude != null && longitude != null) {
                Location location = new Location(latitude, longitude);
                location.setName(name);
                if (address != null && !address.trim().isEmpty()) {
                    location.setAddress(address);
                }
                return location;
            }

            // Sinon, si on a une adresse, créer avec adresse
            if (address != null && !address.trim().isEmpty()) {
                Location location = new Location(address);
                location.setName(name);
                return location;
            }

            // Pas assez de données pour créer une Location
            return null;
        }
    }
}