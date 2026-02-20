package com.example.backend.controller;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import com.example.backend.model.Event;
import com.example.backend.model.Location;
import com.example.backend.service.EventService;
import com.example.backend.service.impl.FocusService;
import com.fasterxml.jackson.annotation.JsonFormat;

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
    private final FocusService focusService;

    public EventController(EventService eventService, FocusService focusService) {
        this.eventService = eventService;
        this.focusService = focusService;
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
    public ResponseEntity<Object> getEventById(@PathVariable Long id) {
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
    public ResponseEntity<Object> createEvent(@RequestBody EventRequest eventRequest) {
        try {
            // Validation : userId est obligatoire
            if (eventRequest.getUserId() == null) {
                return ResponseEntity.badRequest().body("userId est obligatoire");
            }

            // Validation de la surcharge (Etudiant 1)
            focusService.validateDayNotOverloaded(eventRequest.getUserId(), eventRequest.getStartTime());

            Event createdEvent = eventService.createEvent(eventRequest);
            return ResponseEntity.ok(createdEvent);
        } catch (ResponseStatusException e) {
            // Capture l'erreur de surcharge pour renvoyer le bon message au front
            return ResponseEntity.status(e.getStatusCode()).body(e.getReason());
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Erreur serveur : " + e.getMessage());
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
    public ResponseEntity<Object> updateEvent(
            @PathVariable Long id,
            @RequestBody EventRequest eventRequest) {
        try {
            Event updatedEvent = eventService.updateEvent(id, eventRequest);
            return ResponseEntity.ok(updatedEvent);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Erreur serveur : " + e.getMessage());
        }
    }

    /**
     * Supprime un événement.
     *
     * @param id l'ID de l'événement à supprimer
     * @return confirmation de suppression
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Object> deleteEvent(@PathVariable Long id) {
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
    public ResponseEntity<Object> recalculateTravelTimes(
            @RequestParam Long userId,
            @RequestParam Boolean useGoogleMaps) {
        try {
            eventService.recalculateAllTravelTimes(userId, useGoogleMaps);
            return ResponseEntity.ok(Map.of("message", "Temps de trajets recalculés avec succès"));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Erreur lors du recalcul : " + e.getMessage());
        }
    }

    /**
     * Récupère les événements d'un coéquipier en masquant les détails sensibles.
     *
     * @param requesterId l'ID de l'utilisateur qui fait la requête
     * @param memberId l'ID du coéquipier dont on veut voir les événements
     * @return liste des événements du coéquipier avec détails masqués
     */
    @GetMapping("/member/{memberId}")
    public ResponseEntity<List<Event>> getTeamMemberEvents(
            @RequestParam Long requesterId, 
            @PathVariable Long memberId) {
        return ResponseEntity.ok(eventService.getTeamMemberEvents(requesterId, memberId));
    }

    /**
     * DTO pour la création/modification d'événements.
     * 
     * Correction : Ajout de @JsonFormat pour gérer la désérialisation des dates
     * sans timezone (format "2026-02-07T09:00:00")
     */
    public static class EventRequest {
        private String summary;
        private String description; // Support de la description
        
        // Annotation pour accepter le format ISO local sans 'Z'
        // Spring Boot peut parser "2026-02-07T09:00:00" nativement, mais on est explicite
        @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss")
        private LocalDateTime startTime;
        
        @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss")
        private LocalDateTime endTime;
        
        private Long userId;
        private LocationRequest location;
        
        private String category;
        private String subCategory; // Support des sous-catégories
        
        // Mode de transport pour la vérification de faisabilité
        private String transportMode;
        
        // Préférence utilisateur pour Google Maps
        private Boolean useGoogleMaps;
        
        // (On les accepte pour éviter les erreurs de parsing, mais on ne les utilise pas)
        private String color; // Géré côté frontend uniquement
        private String source; // Source de l'événement (LOCAL, GOOGLE_CALENDAR, etc.)

        // Getters et Setters
        public String getSummary() { return summary; }
        public void setSummary(String summary) { this.summary = summary; }

        public String getDescription() { return description; }
        public void setDescription(String description) { this.description = description; }

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

        public String getCategory() { return category; }
        public void setCategory(String category) { this.category = category; }
        
        public String getSubCategory() { return subCategory; }
        public void setSubCategory(String subCategory) { this.subCategory = subCategory; }

        public Boolean getUseGoogleMaps() { return useGoogleMaps; }
        public void setUseGoogleMaps(Boolean useGoogleMaps) { this.useGoogleMaps = useGoogleMaps; }
        
        public String getColor() { return color; }
        public void setColor(String color) { this.color = color; }
        
        public String getSource() { return source; }
        public void setSource(String source) { this.source = source; }
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