package com.example.backend.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.util.List;

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

    public Event() {}

    public Event(String summary, LocalDateTime startTime, LocalDateTime endTime, User user) {
        this.summary = summary;
        this.startTime = startTime;
        this.endTime = endTime;
        this.user = user;
    }

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

    public Long getUserId() {
        return user != null ? user.getId() : null;
    }
}