package com.example.backend.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
public class Event {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String summary;
    private LocalDateTime start;
    private LocalDateTime end;

    public Event() {}

    public Event(String summary, LocalDateTime start, LocalDateTime end) {
        this.summary = summary;
        this.start = start;
        this.end = end;
    }

    public Long getId() { return id; }
    public String getSummary() { return summary; }
    public LocalDateTime getStart() { return start; }
    public LocalDateTime getEnd() { return end; }

    public void setSummary(String summary) { this.summary = summary; }
    public void setStart(LocalDateTime start) { this.start = start; }
    public void setEnd(LocalDateTime end) { this.end = end; }
}