package com.example.backend.model;

import java.time.LocalDateTime;

import com.fasterxml.jackson.annotation.JsonBackReference;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import jakarta.persistence.*;

/**
 * Représente une tâche à effectuer.
 * Une tâche a un titre, une durée estimée, une priorité, un statut (terminé ou non),
 * et est associée à un utilisateur et éventuellement à un événement.
 */
@Entity
public class Task {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private LocalDateTime deadline;
    // Dans votre classe Task
    @Column(nullable = false, columnDefinition = "boolean default false")
    private boolean late = false; 

    private String title;
    private int estimatedDuration; // en minutes
    private int priority; // 1 = haute, 2 = moyenne, 3 = basse
    private boolean done;

    @ManyToOne
    @JoinColumn(name = "user_id", nullable = false)
    @JsonBackReference("userTasks")
    private User user;

    @OneToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "event_id") // <-- Ceci crée la colonne FK dans la table Task
    //@JsonBackReference("taskEvent")
    @JsonIgnoreProperties("task")
 
    private Event event; // ✅ Une tâche peut être liée à un seul événement

    public Task() {

    }

    public Task(String title, int estimatedDuration, int priority, boolean done, User user, LocalDateTime deadline ) {
        this.title = title;
        this.estimatedDuration = estimatedDuration;
        this.priority = priority;
        this.done = done;
        this.user = user;
        this.deadline = deadline;
    }

    public Task(String title, int estimatedDuration, int priority, boolean done, User user, Event event) {
        this.title = title;
        this.estimatedDuration = estimatedDuration;
        this.priority = priority;
        this.done = done;
        this.user = user;
        this.event = event;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public int getEstimatedDuration() {
        return estimatedDuration;
    }

    public void setEstimatedDuration(int estimatedDuration) {
        this.estimatedDuration = estimatedDuration;
    }

    public int getPriority() {
        return priority;
    }

    public void setPriority(int priority) {
        this.priority = priority;
    }

    public boolean isDone() {
        return done;
    }

    public void setDone(boolean done) {
        this.done = done;
    }

    public User getUser() {
        return user;
    }

    public Long getUserId() {
        return user != null ? user.getId() : null;
    }

    public void setUser(User user) {
        this.user = user;
    }

    public Event getEvent() {
        return event;
    }

    public Long getEventId() {
        return event != null ? event.getId() : null;
    }

    public void setEvent(Event event) {
        this.event = event;
    }

    

    public boolean isLate() {
        return late;
    }

    public void setLate(boolean late) {
        this.late = late;
    }

    public LocalDateTime getDeadline() {
        return deadline;
    }

    public void setDeadline(LocalDateTime deadline) {
        this.deadline = deadline;
    }

    // alias pour l'algo
    public int getDuration() {
        return estimatedDuration;
    }

}
