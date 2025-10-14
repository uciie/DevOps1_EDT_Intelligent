package com.example.backend.service;

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

    public int importCalendar(MultipartFile file) throws IOException {
        List<Event> events = parser.parse(file.getInputStream());
        eventRepository.saveAll(events);
        return events.size();
    }
}