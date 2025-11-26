package com.example.backend.model.TypesDevenements;

import java.time.LocalDateTime;
import com.example.backend.model.Event;
import com.example.backend.model.User;

public class TravailEvent extends Event{  
    
    public TravailEvent() {
        super();
    }

    public TravailEvent(String summary, LocalDateTime startTime, LocalDateTime endTime, User user) {
        super(summary, startTime, endTime, user);
    }
    
}
