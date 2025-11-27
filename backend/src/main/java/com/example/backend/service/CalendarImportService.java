package com.example.backend.service;
import com.example.backend.model.User;
import com.example.backend.model.Event;
import com.example.backend.repository.EventRepository;
import com.example.backend.service.parser.ICalendarParser;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;

@Service
public class CalendarImportService {

    private final EventRepository eventRepository;
    private final ICalendarParser parser;

    public CalendarImportService(EventRepository eventRepository, ICalendarParser parser) {
        this.eventRepository = eventRepository;
        this.parser = parser;
    }

    

    public List<Event> importCalendar(MultipartFile file, User user) throws IOException {
        List<Event> events = parser.parse(file.getInputStream());
        // assigner l'utilisateur à chaque événement
        for (Event event : events) {
            event.setUser(user);
        }
        eventRepository.saveAll(events);
        return events;
    }
}