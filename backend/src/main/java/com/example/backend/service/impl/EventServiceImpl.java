package com.example.backend.service.impl;

import com.example.backend.model.Event;
import com.example.backend.repository.EventRepository;
import com.example.backend.service.EventService;
import org.springframework.stereotype.Service;
import java.util.List;

@Service
public class EventServiceImpl implements EventService {

    private final EventRepository eventRepository;

    public EventServiceImpl(EventRepository eventRepository) {
        this.eventRepository = eventRepository;
    }

    @Override
    public List<Event> getEventsByUserId(Long userId) {
        // Utilisation de votre méthode optimisée du repository
        return eventRepository.findByUser_IdOrderByStartTime(userId); 
    }
}