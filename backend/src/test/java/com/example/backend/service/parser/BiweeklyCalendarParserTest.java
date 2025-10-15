package com.example.backend.service.parser;

import com.example.backend.model.Event;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests unitaires pour la classe BiweeklyCalendarParser.
 * Cette classe teste la conversion de contenu ICS (calendrier) en objets Event.
 */
class BiweeklyCalendarParserTest {

    /**
     * Teste le parsing d’un fichier ICS contenant un seul événement.
     * Vérifie que l’événement est correctement interprété et que ses champs principaux sont remplis.
     */
    @Test
    void testParseSingleEvent() throws Exception {
        // Création d’une instance du parser
        BiweeklyCalendarParser parser = new BiweeklyCalendarParser();

        // Définition d’un contenu ICS simulé (1 seul événement)
        String icsContent = """
                            BEGIN:VCALENDAR
                            BEGIN:VEVENT
                            SUMMARY:My Test Event
                            DTSTART:20251014T100000Z
                            DTEND:20251014T110000Z
                            END:VEVENT
                            END:VCALENDAR
                            """;

        // Conversion du texte ICS en InputStream pour simuler un fichier
        InputStream inputStream = new ByteArrayInputStream(icsContent.getBytes());

        // Appel du parser pour transformer le flux ICS en liste d’Event
        List<Event> events = parser.parse(inputStream);

        // Vérifications
        assertEquals(1, events.size(), "Il doit y avoir exactement 1 événement");
        Event event = events.get(0);
        assertEquals("My Test Event", event.getSummary(), "Le summary de l'événement doit correspondre");
        assertNotNull(event.getStartTime(), "La date de début ne doit pas être null");
        assertNotNull(event.getEndTime(), "La date de fin ne doit pas être null");
    }

    /**
     * Teste le parsing d’un ICS contenant plusieurs événements.
     * Vérifie que les deux événements sont correctement détectés et stockés dans la liste.
     */
    @Test
    void testParseMultipleEvents() throws Exception {
        BiweeklyCalendarParser parser = new BiweeklyCalendarParser();

        // Contenu ICS avec deux événements distincts
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

        // Transformation du contenu en flux
        InputStream inputStream = new ByteArrayInputStream(icsContent.getBytes());

        // Parsing du flux
        List<Event> events = parser.parse(inputStream);

        // Vérifications
        assertEquals(2, events.size(), "Il doit y avoir exactement 2 événements");
        assertEquals("Event 1", events.get(0).getSummary());
        assertEquals("Event 2", events.get(1).getSummary());
    }

    /**
     * Teste le cas où un événement n’a pas de date de fin dans le fichier ICS.
     * Le parser doit gérer ce cas et définir une date de fin par défaut (ex : égale à la date de début).
     */
    @Test
    void testParseEventWithoutEndDate() throws Exception {
        BiweeklyCalendarParser parser = new BiweeklyCalendarParser();

        // Contenu ICS sans champ DTEND
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

        // Vérifications
        assertEquals(1, events.size(), "Un seul événement attendu");
        Event event = events.get(0);
        assertEquals("No End Event", event.getSummary());
        assertNotNull(event.getEndTime(), "La date de fin doit être définie par défaut si absente dans le fichier ICS");
    }
}
