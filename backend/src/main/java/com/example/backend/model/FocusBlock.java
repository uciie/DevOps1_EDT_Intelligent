package com.example.backend.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;
@Entity
public class FocusBlock {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    private User user;

    private LocalDateTime startTime;
    private LocalDateTime endTime;
    
    private int flexibility; // De 1 (fixe) à 10 (très déplaçable)
    private boolean isProtected; // Si true, le système ne propose rien dessus
    
    private String status = "ACTIVE"; // ACTIVE, CANCELLED
    private boolean suggestedBySystem; // Pour différencier de l'ajout manuel

    // Constructeurs

    public FocusBlock() {} // Obligatoire pour JPA

    public FocusBlock(User user, LocalDateTime startTime, LocalDateTime endTime) {
        this.user = user;
        this.startTime = startTime;
        this.endTime = endTime;
    }
    // Getters et setters pour tests

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public User getUser() { return user; }
    public void setUser(User user) { this.user = user; }

    public LocalDateTime getStartTime() { return startTime; }
    public void setStartTime(LocalDateTime startTime) { this.startTime = startTime; }

    public LocalDateTime getEndTime() { return endTime; }
    public void setEndTime(LocalDateTime endTime) { this.endTime = endTime; }

    public int getFlexibility() { return flexibility; }
    public void setFlexibility(int flexibility) { this.flexibility = flexibility; }

    public boolean isProtected() { return isProtected; }
    public void setProtected(boolean isProtected) { this.isProtected = isProtected; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public boolean isSuggestedBySystem() { return suggestedBySystem; }
    public void setSuggestedBySystem(boolean suggestedBySystem) { this.suggestedBySystem = suggestedBySystem; }
}
