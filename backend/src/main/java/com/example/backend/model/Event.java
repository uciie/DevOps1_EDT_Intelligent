package com.example.backend.model;
import com.fasterxml.jackson.annotation.JsonBackReference;
import com.fasterxml.jackson.annotation.JsonManagedReference;
import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Représente un événement dans le calendrier.
 * Un événement a un début, une fin, un résumé et est associé à un utilisateur.
 */
@Entity
public class Event {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String summary;
    private LocalDateTime startTime;
    private LocalDateTime endTime;

    private String status = "PLANNED"; // PLANNED, CANCELLED, DONE, etc.

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


    public Event() {}

    public Event(String summary, LocalDateTime startTime, LocalDateTime endTime, User user) {
        this.summary = summary;
        this.startTime = startTime;
        this.endTime = endTime;
        this.user = user;
    }

    public Long getId() {
        return id;
    }

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

    public Task getTask() {
        return task;
    }

    public void setTask(Task task) {
        this.task = task;
    }

        // Méthodes de compatibilité pour les anciens tests
    public void setTasks(List<Task> tasks) {
        if (tasks != null && !tasks.isEmpty()) {
            this.task = tasks.get(0); // On garde seulement la première tâche
        }
    }

    public List<Task> getTasks() {
        return task != null ? List.of(task) : List.of();
    }


    
}
