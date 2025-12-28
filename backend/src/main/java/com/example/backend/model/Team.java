package com.example.backend.model;

import java.util.HashSet;
import java.util.Set;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinTable;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.Table;

@Entity
@Table(name = "teams")
public class Team {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String name;

    private String description;
    
    @Column(name = "owner_id", nullable = false)
    private Long ownerId;

    // Relation ManyToMany avec User (Une équipe a plusieurs membres)
    // On utilise Set pour éviter les doublons
    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(
        name = "team_members",
        joinColumns = @JoinColumn(name = "team_id"),
        inverseJoinColumns = @JoinColumn(name = "user_id")
    )
    @JsonIgnoreProperties("teams") 
    private Set<User> members = new HashSet<>();

    // Constructeurs
    public Team() {}

    public Team(String name, String description,Long ownerId) {
        this.name = name;
        this.description = description;
        this.ownerId = ownerId;
    }

    // Getters et Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public Set<User> getMembers() { 
        return members; 
    }
    public void setMembers(Set<User> members) { this.members = members; }
    
    // Méthodes utilitaires pour ajouter/retirer des membres facilement
    public void addMember(User user) {
        this.members.add(user);
        user.getTeams().add(this);
    }

    public void removeMember(User user) {
        this.members.remove(user);
        user.getTeams().remove(this);
    }

    public Long getOwnerId() { return ownerId; }
    public void setOwnerId(Long ownerId) { this.ownerId = ownerId; }
}