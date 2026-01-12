package com.example.backend.model;

import java.time.LocalDateTime;

import com.fasterxml.jackson.annotation.JsonAlias;
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

    
    @JsonAlias("durationMinutes")
    private int estimatedDuration; // en minutes

    private int priority; // 1 = haute, 2 = moyenne, 3 = basse

    @JsonAlias("completed")
    private boolean done;

    @ManyToOne
    @JoinColumn(name = "user_id", nullable = false)
    @JsonBackReference("userTasks")
    private User user;

    @OneToOne
    @JoinColumn(name = "event_id", nullable = true)
    private Event event;

    // L'utilisateur qui doit FAIRE la tâche
    @ManyToOne
    @JoinColumn(name = "assignee_id")
    @JsonIgnoreProperties({"tasks", "events", "teams"})
    private User assignee;

    // Le contexte du projet (Optionnel mais recommandé par vos specs)
    @ManyToOne
    @JoinColumn(name = "team_id")
    @JsonIgnoreProperties({"members", "description"}) // On garde l'ID et le nom, on ignore les détails lourds
    private Team team;

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

    // Mettez à jour vos constructeurs pour inclure assignee et team si nécessaire
    // Exemple de mise à jour d'un constructeur :
    public Task(String title, int estimatedDuration, int priority, boolean done, User creator, User assignee, Team team) {
        this.title = title;
        this.estimatedDuration = estimatedDuration;
        this.priority = priority;
        this.done = done;
        this.user = creator;
        this.assignee = assignee != null ? assignee : creator; // Par défaut (RM-02), l'assigné est le créateur
        this.team = team;
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

    public User getAssignee() { return assignee; }
    
    public void setAssignee(User assignee) { this.assignee = assignee; }
    
    public Team getTeam() { return team; }
    
    public void setTeam(Team team) { this.team = team; }

    // Helper pour savoir si la tâche est déléguée (RM-04)
    @JsonIgnore
    public boolean isDelegated() {
        return this.assignee != null && !this.assignee.equals(this.user);
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
