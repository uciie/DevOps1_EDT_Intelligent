package com.example.backend.model;

import jakarta.persistence.*;
import java.util.List;
import java.util.Set;
import java.util.HashSet;

import com.fasterxml.jackson.annotation.JsonManagedReference;
import com.fasterxml.jackson.annotation.JsonIgnore;

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
    @JsonManagedReference("userEvents")
    private List<Event> events;

    @OneToMany(mappedBy = "user", cascade = CascadeType.ALL, orphanRemoval = true)
    @JsonManagedReference("userTasks")
    private List<Task> tasks;

    @ManyToMany(mappedBy = "members")
    @JsonIgnore // Important pour éviter les boucles infinies lors de la sérialisation JSON
    private Set<Team> teams = new HashSet<>();

    // Champs pour stocker les tokens OAuth2 de Google
    @Column(name = "google_access_token", length = 1024)
    private String googleAccessToken;

    // Champ pour stocker le refresh token de Google
    @Column(name = "google_refresh_token", length = 1024)
    private String googleRefreshToken;

    /**
     * Constructeur par défaut.
     */
    public User() {}

    /**
     * Construit un nouvel utilisateur avec le nom d'utilisateur et le mot de passe donnés.
     *
     * @param username le nom d'utilisateur (non-null, non-vide)
     * @param password le mot de passe (non-null, peut être vide)
     * @throws IllegalArgumentException si le nom d'utilisateur est nul ou vide, ou si le mot de passe est null.
     */
    public User(String username, String password) {
        if (username == null || username.trim().isEmpty()) {
            throw new IllegalArgumentException("username ne doit pas être vide");
        }
        if (password == null) {
            throw new IllegalArgumentException("password ne doit pas être null");
        }
        this.username = username;
        this.password = password;
    }

    @Override
    public int hashCode() {
        return username.hashCode();
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;

        User user = (User) obj;

        return username != null ? username.equals(user.username) : user.username == null;
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

    // Ajoutez les getters/setters pour teams
    public Set<Team> getTeams() { return teams; }
    public void setTeams(Set<Team> teams) { this.teams = teams; }

    // Getters et setters pour les tokens Google
    public String getGoogleAccessToken() {
        return googleAccessToken;
    }
    public void setGoogleAccessToken(String googleAccessToken) {
        this.googleAccessToken = googleAccessToken;
    }

    public String getGoogleRefreshToken() {
        return googleRefreshToken;
    }
    public void setGoogleRefreshToken(String googleRefreshToken) {
        this.googleRefreshToken = googleRefreshToken;
    }
}