package com.example.backend.service.parser;

import com.example.backend.model.Event;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class BiweeklyCalendarParserTest {

    @Test
    void testParseSingleEvent() throws Exception {
        BiweeklyCalendarParser parser = new BiweeklyCalendarParser();

        String icsContent = """
                            BEGIN:VCALENDAR
                            BEGIN:VEVENT
                            SUMMARY:My Test Event
                            DTSTART:20251014T100000Z
                            DTEND:20251014T110000Z
                            END:VEVENT
                            END:VCALENDAR
                            """;

        InputStream inputStream = new ByteArrayInputStream(icsContent.getBytes());

        List<Event> events = parser.parse(inputStream);

        assertEquals(1, events.size(), "Il doit y avoir exactement 1 événement");
        Event event = events.get(0);
        assertEquals("My Test Event", event.getSummary(), "Le summary de l'événement doit correspondre");
        assertNotNull(event.getStart(), "La date de début ne doit pas être null");
        assertNotNull(event.getEnd(), "La date de fin ne doit pas être null");
    }

    @Test
    void testParseMultipleEvents() throws Exception {
        BiweeklyCalendarParser parser = new BiweeklyCalendarParser();

        String icsContent = """
                            BEGIN:VCALENDAR
                            BEGIN:VEVENT
                            SUMMARY:Event 1
                            DTSTART:20251014T100000Z
                            DTEND:20251014T110000Z
                            END:VEVENT
                            BEGIN:VEVENT
                            SUMMARY:Event 2
                            DTSTART:20251015T120000Z
                            DTEND:20251015T130000Z
                            END:VEVENT
                            END:VCALENDAR
                            """;

        InputStream inputStream = new ByteArrayInputStream(icsContent.getBytes());
        List<Event> events = parser.parse(inputStream);

        assertEquals(2, events.size(), "Il doit y avoir exactement 2 événements");
        assertEquals("Event 1", events.get(0).getSummary());
        assertEquals("Event 2", events.get(1).getSummary());
    }

    @Test
    void testParseEventWithoutEndDate() throws Exception {
        BiweeklyCalendarParser parser = new BiweeklyCalendarParser();

        String icsContent = """
                            BEGIN:VCALENDAR
                            BEGIN:VEVENT
                            SUMMARY:No End Event
                            DTSTART:20251014T100000Z
                            END:VEVENT
                            END:VCALENDAR
                            """;

        InputStream inputStream = new ByteArrayInputStream(icsContent.getBytes());
        List<Event> events = parser.parse(inputStream);

        assertEquals(1, events.size());
        Event event = events.get(0);
        assertEquals("No End Event", event.getSummary());
        assertNotNull(event.getEnd(), "La date de fin doit être définie par défaut si absente dans le fichier ICS");
    }
}
