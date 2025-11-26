package com.example.backend.model;
import com.fasterxml.jackson.annotation.JsonBackReference;
import com.fasterxml.jackson.annotation.JsonManagedReference;

import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonManagedReference;

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

    @ManyToOne(cascade = CascadeType.ALL)
    @JoinColumn(name = "location_id")
    private Location location;

    private String status = "PLANNED";

    @ManyToOne
    @JoinColumn(name = "user_id", nullable = false)
    @JsonBackReference("userEvents")
    private User user;

    @OneToOne
    @JoinColumn(name = "task_id", nullable = true)
    @JsonManagedReference("taskEvent")
    private Task task; // ✅ une tâche associée (facultative)

    public Event(String summary, LocalDateTime startTime, LocalDateTime endTime) {
    this.summary = summary;
    this.startTime = startTime;
    this.endTime = endTime;
    }

    // Liste des tâches (Correctement géré en liste)
    @OneToMany(mappedBy = "event", cascade = CascadeType.ALL, orphanRemoval = true)
    @JsonManagedReference // <--- INDISPENSABLE : Côté Parent
    private List<Task> tasks = new ArrayList<>();

    public Event() {}

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

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
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

    // --- Gestion des Tâches (Logique corrigée : On garde la LISTE) ---

    public List<Task> getTasks() {
        return tasks;
    }

    public void setTasks(List<Task> tasks) {
        this.tasks = tasks;
    }

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
        return id != null ? id.equals(event.id) : event.id == null;
    }
}