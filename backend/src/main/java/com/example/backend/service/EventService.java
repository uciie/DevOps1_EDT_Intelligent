package com.example.backend.service;

import com.example.backend.model.Event;
import java.util.List;

public interface EventService {
    List<Event> getEventsByUserId(Long userId);
}