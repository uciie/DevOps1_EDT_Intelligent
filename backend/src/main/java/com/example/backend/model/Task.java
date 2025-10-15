package com.example.backend.model;

import jakarta.persistence.*;

@Entity
public class Task {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String title;
    private int estimatedDuration;
    private int priority;

    @ManyToOne
    @JoinColumn(name = "event_id")
    private Event event;

    public Task() {}

    public Task(String title, int estimatedDuration, int priority, Event event) {
        this.title = title;
        this.estimatedDuration = estimatedDuration;
        this.priority = priority;
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

    public Event getEvent() {
        return event;
    }

    public void setEvent(Event event) {
        this.event = event;
    }
}