package com.example.backend.model.TypesDevenements;

import java.time.LocalDateTime;
import com.example.backend.model.Event;
import com.example.backend.model.User;

public class EtudeEvent extends Event{  
    
    public enum EtudeCategory{COURS, TD, TP, REVISION, EXAMEN, AUTRE_ETUDE}
    private EtudeCategory etudeType;
    public EtudeEvent() {
        super();
    }

    public EtudeEvent(String summary, LocalDateTime startTime, LocalDateTime endTime, User user, EtudeCategory etudeType) {
        super(summary, startTime, endTime, user);
        this.etudeType = etudeType;
    }

    public EtudeCategory getEtudeType() {
        return this.etudeType;
    }
    
}
