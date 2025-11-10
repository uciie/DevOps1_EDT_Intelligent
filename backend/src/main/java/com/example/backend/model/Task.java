package com.example.backend.model;

import com.fasterxml.jackson.annotation.JsonBackReference;
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

    private String title;
    private int estimatedDuration; // en minutes
    private int priority; // 1 = haute, 2 = moyenne, 3 = basse
    private boolean done;

    @ManyToOne
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @OneToOne(mappedBy = "task")
    @JsonBackReference
    private Event event; // ✅ Une tâche peut être liée à un seul événement

    public Task() {}

    public Task(String title, int estimatedDuration, int priority, boolean done, User user) {
        this.title = title;
        this.estimatedDuration = estimatedDuration;
        this.priority = priority;
        this.done = done;
        this.user = user;
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

    public void setUser(User user) {
        this.user = user;
    }

    public Event getEvent() {
        return event;
    }

    public void setEvent(Event event) {
        this.event = event;
    }
}
