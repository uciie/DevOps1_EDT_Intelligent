package com.example.backend.service;

import com.example.backend.model.User;
import com.example.backend.model.Event;
import com.example.backend.repository.EventRepository;
import com.example.backend.service.parser.ICalendarParser;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.web.multipart.MultipartFile;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.util.Collections;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.eq;

import java.lang.reflect.Method;
import java.time.LocalDateTime;
import java.time.ZoneId;

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

    @Test
    void importCalendar_savesParsedEvents() throws Exception {
        MultipartFile file = Mockito.mock(MultipartFile.class);
        when(file.getInputStream()).thenReturn(new ByteArrayInputStream(new byte[0]));

        User user = new User();
        user.setId(1L);

        Event e1 = new Event();
        e1.setSummary("A");
        when(parser.parse(file.getInputStream())).thenReturn(List.of(e1));

        List<Event> result = importService.importCalendar(file, user);

        assertThat(result).hasSize(1);
        verify(eventRepository).saveAll(org.mockito.ArgumentMatchers.anyList());
    }

    @Test
    void pullEventsFromGoogle_noToken_throws() {
        User user = new User();
        user.setId(2L);
        user.setGoogleAccessToken(null);

        assertThrows(RuntimeException.class, () -> importService.pullEventsFromGoogle(user));
    }

    @Test
    void buildCalendarClient_reflection_returnsCalendar() throws Exception {
        User user = new User();
        user.setId(3L);
        user.setGoogleAccessToken("tok");

        Method m = CalendarImportService.class.getDeclaredMethod("buildCalendarClient", User.class);
        m.setAccessible(true);

        Object cal = m.invoke(importService, user);
        assertThat(cal).isNotNull();
    }

    @Test
    void deleteOrphanedGoogleEvents_deletesOrphans() throws Exception {
        User user = new User();
        user.setId(4L);

        Event e1 = new Event(); e1.setId(10L); e1.setSource(Event.EventSource.GOOGLE); e1.setGoogleEventId("g1");
        Event e2 = new Event(); e2.setId(11L); e2.setSource(Event.EventSource.GOOGLE); e2.setGoogleEventId("g2");

        when(eventRepository.findByUser_Id(4L)).thenReturn(List.of(e1, e2));

        // build googleEvents containing only g1
        com.google.api.services.calendar.model.Event ge = new com.google.api.services.calendar.model.Event();
        ge.setId("g1");
        List<com.google.api.services.calendar.model.Event> googleList = List.of(ge);

        Method m = CalendarImportService.class.getDeclaredMethod("deleteOrphanedGoogleEvents", User.class, List.class);
        m.setAccessible(true);

        Object res = m.invoke(importService, user, googleList);
        assertThat(res).isEqualTo(1);
        verify(eventRepository, times(1)).delete(eq(e2));
    }

    @Test
    void updateLocationIfNeeded_variousCases() throws Exception {
        Method m = CalendarImportService.class.getDeclaredMethod("updateLocationIfNeeded", Event.class, String.class);
        m.setAccessible(true);

        Event ev = new Event();
        // case 1: google has location, local null
        boolean changed = (Boolean) m.invoke(importService, ev, "Addr, City, Country");
        assertThat(changed).isTrue();
        assertThat(ev.getLocation()).isNotNull();

        // case 2: same address -> no change
        changed = (Boolean) m.invoke(importService, ev, "Addr, City, Country");
        assertThat(changed).isFalse();

        // case 3: different address -> updated
        changed = (Boolean) m.invoke(importService, ev, "New Addr, City, Country");
        assertThat(changed).isTrue();

        // case 4: google null and local exists -> removed
        changed = (Boolean) m.invoke(importService, ev, (Object) null);
        assertThat(changed).isTrue();
        assertThat(ev.getLocation()).isNull();
    }

    @Test
    void toLocalDateTime_handlesDateTimeAndDateAndNull() throws Exception {
        Method m = CalendarImportService.class.getDeclaredMethod("toLocalDateTime", com.google.api.services.calendar.model.EventDateTime.class, java.time.ZoneId.class);
        m.setAccessible(true);

        ZoneId zone = ZoneId.of("Europe/Paris");

        com.google.api.services.calendar.model.EventDateTime edt = new com.google.api.services.calendar.model.EventDateTime();
        long nowMs = System.currentTimeMillis();
        edt.setDateTime(new com.google.api.client.util.DateTime(nowMs));

        LocalDateTime res = (LocalDateTime) m.invoke(importService, edt, zone);
        assertThat(res).isNotNull();

        com.google.api.services.calendar.model.EventDateTime edDate = new com.google.api.services.calendar.model.EventDateTime();
        edDate.setDate(new com.google.api.client.util.DateTime(nowMs));
        LocalDateTime res2 = (LocalDateTime) m.invoke(importService, edDate, zone);
        assertThat(res2).isNotNull();

        LocalDateTime res3 = (LocalDateTime) m.invoke(importService, null, zone);
        assertThat(res3).isNotNull();
    }
}

