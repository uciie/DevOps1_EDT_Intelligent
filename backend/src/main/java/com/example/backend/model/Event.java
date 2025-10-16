package com.example.backend.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Représente un événement dans le calendrier.
 * Un événement a un début, une fin, un résumé et est associé à un utilisateur.
 * Il peut également contenir une liste de tâches.
 */
@Entity
public class Event {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String summary;
    private LocalDateTime startTime;
    private LocalDateTime endTime;

    private String status = "PLANNED"; // ajouté pour gestion des états

    @ManyToOne
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @OneToMany(mappedBy = "event", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Task> tasks;

    /**
     * Constructeur par défaut.
     */
    public Event() {}

    /**
     * Construit un nouvel événement avec le résumé, l'heure de début, l'heure de fin et l'utilisateur donnés.
     *
     * @param summary le résumé de l'événement.
     * @param startTime l'heure de début de l'événement.
     * @param endTime l'heure de fin de l'événement.
     * @param user l'utilisateur associé à l'événement.
     */
    public Event(String summary, LocalDateTime startTime, LocalDateTime endTime, User user) {
        this.summary = summary;
        this.startTime = startTime;
        this.endTime = endTime;
        this.user = user;
    }

    /**
     * Construit un nouvel événement avec le résumé, l'heure de début et l'heure de fin donnés.
     *
     * @param summary le résumé de l'événement.
     * @param startTime l'heure de début de l'événement.
     * @param endTime l'heure de fin de l'événement.
     */
    public Event(String summary, LocalDateTime startTime, LocalDateTime endTime) {
        this.summary = summary;
        this.startTime = startTime;
        this.endTime = endTime;
    }

    public Long getId() { return id; }

    public String getSummary() { return summary; }
    public void setSummary(String summary) { this.summary = summary; }

    public LocalDateTime getStartTime() { return startTime; }
    public void setStartTime(LocalDateTime startTime) { this.startTime = startTime; }

    public LocalDateTime getEndTime() { return endTime; }
    public void setEndTime(LocalDateTime endTime) { this.endTime = endTime; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public User getUser() { return user; }
    public void setUser(User user) { this.user = user; }

    public List<Task> getTasks() { return tasks; }
    public void setTasks(List<Task> tasks) { this.tasks = tasks; }

    /**
     * Renvoie l'ID de l'utilisateur associé à cet événement.
     *
     * @return l'ID de l'utilisateur, ou null si aucun utilisateur n'est associé.
     */
    public Long getUserId() {
        return user != null ? user.getId() : null;
    }
}