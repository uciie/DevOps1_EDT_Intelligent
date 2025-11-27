package com.example.backend.model.TypesDevenements;

import java.time.LocalDateTime;
import com.example.backend.model.Event;
import com.example.backend.model.User;

public class SportEvent extends Event{  
    
    public enum SportCategory{VOLLEYBALL, FOOTBALL, BASKETBALL,
                                TENNIS, NATATION, RUNNING, VELO,
                                MUSCULATION, AUTRE_FIELD_SPORT,
                                AUTRE_SPORT_RENFORCEMENT}
    private SportCategory sportType;
    public SportEvent() {
        super();
    }

    public SportEvent(String summary, LocalDateTime startTime, LocalDateTime endTime, User user, SportCategory sportType) {
        super(summary, startTime, endTime, user);
        this.sportType = sportType;
    }

    public SportCategory getSportType() {
        return this.sportType;
    }
    
}
