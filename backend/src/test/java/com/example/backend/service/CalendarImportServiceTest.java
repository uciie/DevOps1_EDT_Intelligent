package com.example.backend.service;

import com.example.backend.model.User;
import com.example.backend.model.Event;
import com.example.backend.repository.EventRepository;
import com.example.backend.service.parser.ICalendarParser;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockMultipartFile;

import java.io.InputStream;
import java.util.Collections;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.*;
import static org.junit.jupiter.api.Assertions.*;

class CalendarImportServiceTest {

    private EventRepository eventRepository;
    private ICalendarParser parser;
    private CalendarImportService importService;
    private User u1;

    @BeforeEach
    void setUp() {
        eventRepository = mock(EventRepository.class);
        parser = mock(ICalendarParser.class);
        importService = new CalendarImportService(eventRepository, parser);
        
        u1 = new User();        //utilisateur factice
        u1.setId(1L);
        u1.setUsername("a4");
    }

   @Test
    void testImportCalendar() throws Exception {
        // Crée un fichier ICS simulé
        String icsContent = """
                BEGIN:VCALENDAR
                BEGIN:VEVENT
                SUMMARY:Test Event
                DTSTART:20251014T100000Z
                DTEND:20251014T110000Z
                END:VEVENT
                END:VCALENDAR
                """;

        MockMultipartFile file = new MockMultipartFile(
                "file",
                "test.ics",
                "text/calendar",
                icsContent.getBytes()
        );

        // Simule le parser
        // constructeur Event(summary, start, end, user)
        Event mockEvent = new Event("Test Event", null, null, null);
        
        when(parser.parse(any(InputStream.class))).thenReturn(Collections.singletonList(mockEvent));

        // Appelle le service
        List<Event> eventsImportes = importService.importCalendar(file, u1);

        // Vérifie repository appelé
        verify(eventRepository, times(1)).saveAll(anyList());

        // Vérifie le nombre d’événements retournés
        assertEquals(1, eventsImportes.size());

        // Vérifie que l'utilisateur est bien associé
        assertEquals(u1, eventsImportes.get(0).getUser());
    }
}

