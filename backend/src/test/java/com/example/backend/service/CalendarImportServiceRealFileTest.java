package com.example.backend.service;

import com.example.backend.model.Event;
import com.example.backend.model.User;
import com.example.backend.repository.EventRepository;
import com.example.backend.repository.UserRepository;
import com.example.backend.service.parser.BiweeklyCalendarParser;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import org.springframework.mock.web.MockMultipartFile;

import java.io.InputStream;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;

@DataJpaTest
@Import({ CalendarImportService.class, BiweeklyCalendarParser.class })
class CalendarImportServiceRealFileTest {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private EventRepository eventRepository;

    @Autowired
    private CalendarImportService calendarImportService;

   @Test
        void testImportRealIcsFile() throws Exception {

        // Création d'un faux utilisateur
        User testUser = new User("testUser", "password123");
        userRepository.save(testUser);

        assertNotNull(testUser.getId(), "L'utilisateur doit être enregistré et avoir un ID");

        
        InputStream inputStream = getClass().getResourceAsStream("/test_calendrier.ics"); //fichier réel de test se trouvant dans les ressources
        assertNotNull(inputStream, "Le fichier ICS doit exister dans src/test/resources");

        MockMultipartFile file = new MockMultipartFile(
                "file",
                "test_calendar.ics",
                "text/calendar",
                inputStream
        );

        // Import des événements
        List<Event> events = calendarImportService.importCalendar(file, testUser);

        // Vérifications
        assertEquals(2, events.size(), "Le fichier ICS devrait contenir 2 événements");

        events.forEach(event -> {
                assertNotNull(event.getId(), "Chaque événement doit être sauvegardé en base");
                assertEquals(testUser.getId(), event.getUser().getId(), "Chaque event doit être rattaché à l'utilisateur");
        });

        // Vérifier réellement en base
        List<Event> savedEvents = eventRepository.findAll();
        assertEquals(2, savedEvents.size(), "Deux événements devraient être réellement persistés en base");
        }
}