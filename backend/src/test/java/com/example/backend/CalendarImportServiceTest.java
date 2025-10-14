package com.example.backend;

import com.example.backend.model.Event;
import com.example.backend.repository.EventRepository;
import com.example.backend.service.CalendarImportService;
import com.example.backend.service.parser.ICalendarParser;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.mock.web.MockMultipartFile;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.util.Collections;
import java.util.List;

import static org.mockito.Mockito.*;
import static org.junit.jupiter.api.Assertions.*;

class CalendarImportServiceTest {

    private EventRepository eventRepository;
    private ICalendarParser parser;
    private CalendarImportService importService;

    @BeforeEach
    void setUp() {
        eventRepository = mock(EventRepository.class);
        parser = mock(ICalendarParser.class);
        importService = new CalendarImportService(eventRepository, parser);
    }

    @Test
    void testImportCalendar() throws Exception {
        // Crée un fichier ICS simulé
        String icsContent = "BEGIN:VCALENDAR\n" +
                "BEGIN:VEVENT\n" +
                "SUMMARY:Test Event\n" +
                "DTSTART:20251014T100000Z\n" +
                "DTEND:20251014T110000Z\n" +
                "END:VEVENT\n" +
                "END:VCALENDAR";

        MockMultipartFile file = new MockMultipartFile(
                "file",
                "test.ics",
                "text/calendar",
                icsContent.getBytes()
        );

        // Simule le parser
        Event mockEvent = new Event("Test Event", null, null);
        when(parser.parse(any(InputStream.class))).thenReturn(Collections.singletonList(mockEvent));

        // Appelle le service
        int count = importService.importCalendar(file);

        // Vérifie que le repository a bien été appelé
        verify(eventRepository, times(1)).saveAll(anyList());

        // Vérifie le nombre d’événements retournés
        assertEquals(1, count);
    }
}
