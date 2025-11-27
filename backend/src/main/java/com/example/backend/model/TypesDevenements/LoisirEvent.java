package com.example.backend.model.TypesDevenements;

import java.time.LocalDateTime;
import com.example.backend.model.Event;
import com.example.backend.model.User;

public class LoisirEvent extends Event{  
    
    private enum LoisirCategory{ CINEMA, THEATRE, MUSIQUE,
                                 JEUX_VIDEO, LECTURE, VOYAGE,
                                 CULINAIRE, AUTRE_LOISIR};
    private LoisirCategory loisirType;
    public LoisirEvent() {
        super();
    }

    public LoisirEvent(String summary, LocalDateTime startTime, LocalDateTime endTime, User user, LoisirCategory loisirType) {
        super(summary, startTime, endTime, user);
        this.loisirType = loisirType;
    }

    public LoisirCategory getLoisirType() {
        return this.loisirType;
    }
    
}
