package com.example.backend.model;

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
    private int estimatedDuration;
    private int priority;
    private boolean done;

    @ManyToOne
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne
    @JoinColumn(name = "event_id")
    private Event event;

    /**
     * Constructeur par défaut.
     */
    public Task() {}

    /**
     * Construit une nouvelle tâche avec le titre, la durée estimée, la priorité, le statut, l'utilisateur et l'événement donnés.
     *
     * @param title le titre de la tâche.
     * @param estimatedDuration la durée estimée de la tâche en minutes.
     * @param priority la priorité de la tâche.
     * @param done le statut de la tâche (terminé ou non).
     * @param user l'utilisateur associé à la tâche.
     * @param event l'événement associé à la tâche.
     */
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
