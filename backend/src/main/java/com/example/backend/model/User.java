package com.example.backend.model;

import jakarta.persistence.*;
import java.util.List;

/**
 * Représente un utilisateur de l'application.
 * Un utilisateur a un nom d'utilisateur unique, un mot de passe, et des listes d'événements et de tâches associés.
 */
@Entity
@Table(name = "users")
public class User {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true, nullable = false)
    private String username;

    @Column(nullable = false)
    private String password;

    @OneToMany(mappedBy = "user", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Event> events;

    @OneToMany(mappedBy = "user", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Task> tasks;

    /**
     * Constructeur par défaut.
     */
    public User() {}

    /**
     * Construit un nouvel utilisateur avec le nom d'utilisateur et le mot de passe donnés.
     *
     * @param username le nom d'utilisateur.
     * @param password le mot de passe.
     */
    public User(String username, String password) {
        this.username = username;
        this.password = password;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }
    public String getPassword() { return password; }
    public void setPassword(String password) { this.password = password; }

    public List<Event> getEvents() { return events; }
    public void setEvents(List<Event> events) { this.events = events; }

    public List<Task> getTasks() { return tasks; }
    public void setTasks(List<Task> tasks) { this.tasks = tasks; }
}