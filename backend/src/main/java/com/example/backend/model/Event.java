package com.example.backend.model;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;

import com.fasterxml.jackson.annotation.JsonBackReference;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OneToOne;
import jakarta.persistence.PreRemove;
/**
 * Représente un événement dans le calendrier.
 */
@Entity
public class Event {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String summary;
    private LocalDateTime startTime;
    private LocalDateTime endTime;

    @Enumerated(EnumType.STRING)
    private ActivityCategory category;

    // OneToOne avec Cascade pour supprimer la location liée automatiquement
    @OneToOne(cascade = CascadeType.ALL, orphanRemoval = true)
    @JoinColumn(name = "location_id")
    private Location location;

    @ManyToOne
    @JoinColumn(name = "user_id", nullable = false)
    @JsonBackReference("userEvents")
    private User user;

    @OneToOne(mappedBy = "event", cascade = {CascadeType.PERSIST, CascadeType.MERGE})
    @JsonIgnoreProperties("event")
    private Task task; 
        @OneToMany(mappedBy = "fromEvent", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<TravelTime> departures;

    @OneToMany(mappedBy = "toEvent", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<TravelTime> arrivals;

    @Enumerated(EnumType.STRING)
    private EventStatus status = EventStatus.PLANNED;

    @Column(name = "google_event_id", unique = true)
    private String googleEventId;

    @Column(name = "last_synced_at")
    private LocalDateTime lastSyncedAt;

    @Enumerated(EnumType.STRING)
    private EventSource source = EventSource.LOCAL; // LOCAL ou GOOGLE

    @Column(name = "sync_status")
    @Enumerated(EnumType.STRING)
    private SyncStatus syncStatus = SyncStatus.SYNCED; // SYNCED, PENDING, CONFLICT

    public enum EventSource {
        LOCAL,
        GOOGLE
    }

    public enum SyncStatus {
        SYNCED,
        PENDING,
        CONFLICT
    }

    public enum EventStatus {
        PLANNED,
        CONFIRMED,
        PENDING_DELETION
    }

    public Event() {}

    public Event(String summary, LocalDateTime startTime, LocalDateTime endTime) {
        this.summary = summary;
        this.startTime = startTime;
        this.endTime = endTime;
    }

    public Event(String summary, LocalDateTime startTime, LocalDateTime endTime, User user) {
        this.summary = summary;
        this.startTime = startTime;
        this.endTime = endTime;
        this.user = user;
    }

    // --- Getters et Setters ---

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    /**
     * Helper pour récupérer l'ID utilisateur directement
     */
    public Long getUserId() {
        return user != null ? user.getId() : null;
    }

    public String getSummary() {
        return summary;
    }

    public void setSummary(String summary) {
        this.summary = summary;
    }

    public LocalDateTime getStartTime() {
        return startTime;
    }

    public void setStartTime(LocalDateTime startTime) {
        this.startTime = startTime;
    }

    public LocalDateTime getEndTime() {
        return endTime;
    }

    public void setEndTime(LocalDateTime endTime) {
        this.endTime = endTime;
    }

    public EventStatus getStatus() {
        return status;
    }

    public void setStatus(EventStatus status) {
        this.status = status;
    }

    public User getUser() {
        return user;
    }

    public void setUser(User user) {
        this.user = user;
    }

    // --- Gestion de la Location (Fusionné depuis la branche distante) ---

    public Location getLocation() {
        return location;
    }

    public void setLocation(Location location) {
        this.location = location;
    }
    
    public Task getTask() {
        return task;
    }

    public void setTask(Task task) {
        this.task = task;
    }
    public ActivityCategory getCategory() {
        return category;
    }
    public void setCategory(ActivityCategory category) {
        this.category = category;
    }

    public String getGoogleEventId() { return googleEventId; }
    public void setGoogleEventId(String googleEventId) { this.googleEventId = googleEventId; }

    public LocalDateTime getLastSyncedAt() { return lastSyncedAt; }
    public void setLastSyncedAt(LocalDateTime lastSyncedAt) { this.lastSyncedAt = lastSyncedAt; }

    public EventSource getSource() { return source; }
    public void setSource(EventSource source) { this.source = source; }

    public SyncStatus getSyncStatus() { return syncStatus; }
    public void setSyncStatus(SyncStatus syncStatus) { this.syncStatus = syncStatus; }

    // --- Méthodes standard ---

    @Override
    public int hashCode() {
        return id != null ? id.hashCode() : 0;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;
        Event event = (Event) obj;
        return Objects.equals(id, event.id);
    }

    @PreRemove
    private void preRemove() {
        // Si une tâche est liée, on coupe le lien (setEvent(null))
        // pour que la tâche reste en base de données avec event_id = NULL.
        if (task != null) {
            task.setEvent(null);
        }
    }


}