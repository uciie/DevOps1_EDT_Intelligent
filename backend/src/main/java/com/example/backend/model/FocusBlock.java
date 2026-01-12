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
}
