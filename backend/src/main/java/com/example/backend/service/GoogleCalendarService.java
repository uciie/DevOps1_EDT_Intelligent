package com.example.backend.service;

import com.example.backend.model.Event;
import com.example.backend.model.User;
import com.example.backend.repository.EventRepository;

import com.google.api.client.googleapis.auth.oauth2.GoogleCredential;
import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport;
import com.google.api.client.json.jackson2.JacksonFactory;
import com.google.api.services.calendar.Calendar;
import com.google.api.services.calendar.model.EventDateTime;
import com.google.api.client.util.DateTime;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.security.GeneralSecurityException;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;

@Service
public class GoogleCalendarService {

    private static final Logger log        = LoggerFactory.getLogger(GoogleCalendarService.class);
    private static final String CALENDAR_ID = "primary";
    private static final int    MAX_RETRIES = 3;

    @Value("${google.calendar.timezone:Europe/Paris}")
    private String defaultTimezone;

    private final EventRepository eventRepository;

    public GoogleCalendarService(EventRepository eventRepository) {
        this.eventRepository = eventRepository;
    }

    // ── authenticated Calendar client ────────────────────────────────────────
    // GoogleCredential (singular) implements HttpRequestInitializer directly,
    // so it is passed straight into Calendar.Builder — no .getRequestInitializer().
    private Calendar buildCalendarClient(User user) throws IOException, GeneralSecurityException {
        GoogleCredential credential = new GoogleCredential();
        credential.setAccessToken(user.getGoogleAccessToken());

        return new Calendar.Builder(
                GoogleNetHttpTransport.newTrustedTransport(),
                JacksonFactory.getDefaultInstance(),
                credential)                              // <-- IS the HttpRequestInitializer
                .setApplicationName("EDT-Intelligent")
                .build();
    }

    // ── export one event to Google Calendar ──────────────────────────────────
    @Transactional
    public void pushEventToGoogle(Event event) {
        User user = event.getUser();

        if (user.getGoogleAccessToken() == null || user.getGoogleAccessToken().isBlank()) {
            log.warn("[PUSH] Utilisateur {} sans token Google — export ignoré.", user.getId());
            return;
        }

        com.google.api.services.calendar.model.Event googleEvent = convertToGoogleEvent(event);

        for (int attempt = 1; attempt <= MAX_RETRIES; attempt++) {
            try {
                Calendar client = buildCalendarClient(user);

                com.google.api.services.calendar.model.Event created =
                        client.events().insert(CALENDAR_ID, googleEvent).execute();

                // persist the Google-assigned ID for later deduplication
                event.setGoogleEventId(created.getId());
                eventRepository.save(event);

                log.info("[PUSH] Événement '{}' exporté avec succès (googleId={}).",
                         event.getSummary(), created.getId());
                return; // success — exit retry loop

            } catch (IOException e) {
                log.warn("[PUSH] Tentative {}/{} échouée pour '{}' : {}",
                         attempt, MAX_RETRIES, event.getSummary(), e.getMessage());
                if (attempt == MAX_RETRIES) {
                    log.error("[PUSH] Échec définitif après {} tentatives.", MAX_RETRIES, e);
                }
            } catch (GeneralSecurityException e) {
                // transport-level security failure — no point retrying
                log.error("[PUSH] Erreur de sécurité (transport) : {}", e.getMessage());
                return;
            }
        }
    }

    // ── LocalDateTime  →  Google EventDateTime ──────────────────────────────
    private com.google.api.services.calendar.model.Event convertToGoogleEvent(Event event) {
        ZoneId zone = ZoneId.of(defaultTimezone);

        EventDateTime start = new EventDateTime()
                .setDateTime(new DateTime(event.getStartTime().atZone(zone)
                        .format(DateTimeFormatter.ISO_OFFSET_DATE_TIME)))
                .setTimeZone(defaultTimezone);

        EventDateTime end = new EventDateTime()
                .setDateTime(new DateTime(event.getEndTime().atZone(zone)
                        .format(DateTimeFormatter.ISO_OFFSET_DATE_TIME)))
                .setTimeZone(defaultTimezone);

        com.google.api.services.calendar.model.Event gEvent =
                new com.google.api.services.calendar.model.Event();
        gEvent.setSummary(event.getSummary());
        gEvent.setStart(start);
        gEvent.setEnd(end);

        // embed internal ID so a later pull can recognise events we already own
        if (event.getId() != null) {
            gEvent.setDescription("EDT-ID:" + event.getId());
        }

        return gEvent;
    }
}