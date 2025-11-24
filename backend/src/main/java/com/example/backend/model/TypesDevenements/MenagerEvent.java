package com.example.backend.model.TypesDevenements;

import java.time.LocalDateTime;
import com.example.backend.model.Event;
import com.example.backend.model.User;

public class MenagerEvent extends Event{  
    
    public MenagerEvent() {
        super();
    }

    public MenagerEvent(String summary, LocalDateTime startTime, LocalDateTime endTime, User user) {
        super(summary, startTime, endTime, user);
    }
    
}
