package com.example.backend.dto;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * DTO représentant un conflit de synchronisation entre événements.
 */
public class SyncConflictDTO {
    
    private List<ConflictingEvent> conflicts = new ArrayList<>();
    private String message;
    private boolean hasConflicts;

    public SyncConflictDTO() {
        this.hasConflicts = false;
        this.message = "Aucun conflit détecté";
    }

    public void addConflict(ConflictingEvent conflict) {
        this.conflicts.add(conflict);
        this.hasConflicts = true;
        this.message = conflicts.size() + " conflit(s) détecté(s)";
    }

    // Getters et Setters
    public List<ConflictingEvent> getConflicts() { return conflicts; }
    public void setConflicts(List<ConflictingEvent> conflicts) { this.conflicts = conflicts; }
    
    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }
    
    public boolean isHasConflicts() { return hasConflicts; }
    public void setHasConflicts(boolean hasConflicts) { this.hasConflicts = hasConflicts; }

    /**
     * Classe interne représentant un événement en conflit.
     */
    public static class ConflictingEvent {
        private Long eventId;
        private String title;
        private LocalDateTime startTime;
        private LocalDateTime endTime;
        private String source; // "GOOGLE" ou "LOCAL"
        private Long conflictingWithId;
        private String conflictingWithTitle;
        private String conflictingWithSource;

        public ConflictingEvent() {}

        public ConflictingEvent(Long eventId, String title, LocalDateTime startTime, 
                                LocalDateTime endTime, String source) {
            this.eventId = eventId;
            this.title = title;
            this.startTime = startTime;
            this.endTime = endTime;
            this.source = source;
        }

        // Getters et Setters
        public Long getEventId() { return eventId; }
        public void setEventId(Long eventId) { this.eventId = eventId; }
        
        public String getTitle() { return title; }
        public void setTitle(String title) { this.title = title; }
        
        public LocalDateTime getStartTime() { return startTime; }
        public void setStartTime(LocalDateTime startTime) { this.startTime = startTime; }
        
        public LocalDateTime getEndTime() { return endTime; }
        public void setEndTime(LocalDateTime endTime) { this.endTime = endTime; }
        
        public String getSource() { return source; }
        public void setSource(String source) { this.source = source; }
        
        public Long getConflictingWithId() { return conflictingWithId; }
        public void setConflictingWithId(Long conflictingWithId) { this.conflictingWithId = conflictingWithId; }
        
        public String getConflictingWithTitle() { return conflictingWithTitle; }
        public void setConflictingWithTitle(String conflictingWithTitle) { this.conflictingWithTitle = conflictingWithTitle; }
        
        public String getConflictingWithSource() { return conflictingWithSource; }
        public void setConflictingWithSource(String conflictingWithSource) { this.conflictingWithSource = conflictingWithSource; }
    }
}