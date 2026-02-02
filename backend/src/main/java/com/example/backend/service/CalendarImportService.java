package com.example.backend.service;

import com.example.backend.model.Event;
import com.example.backend.model.User;
import com.example.backend.repository.EventRepository;
import com.example.backend.service.parser.ICalendarParser;

import com.google.api.client.googleapis.auth.oauth2.GoogleCredential;
import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport;
import com.google.api.client.json.jackson2.JacksonFactory;
import com.google.api.client.util.DateTime;
import com.google.api.services.calendar.Calendar;
import com.google.api.services.calendar.model.EventDateTime;
import com.google.api.services.calendar.model.Events;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.security.GeneralSecurityException;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.Optional;

@Service
public class CalendarImportService {

    private static final Logger log         = LoggerFactory.getLogger(CalendarImportService.class);
    private static final String CALENDAR_ID = "primary";

    @Value("${google.calendar.timezone:Europe/Paris}")
    private String defaultTimezone;

    private final EventRepository eventRepository;
    private final ICalendarParser  parser;

    public CalendarImportService(EventRepository eventRepository, ICalendarParser parser) {
        this.eventRepository = eventRepository;
        this.parser          = parser;
    }

    // ── existing ICS file import (unchanged) ─────────────────────────────────
    public List<Event> importCalendar(MultipartFile file, User user) throws IOException {
        List<Event> events = parser.parse(file.getInputStream());
        for (Event event : events) {
            event.setUser(user);
        }
        eventRepository.saveAll(events);
        return events;
    }

    // ── authenticated Calendar client ────────────────────────────────────────
    private Calendar buildCalendarClient(User user) throws IOException, GeneralSecurityException {
        GoogleCredential credential = new GoogleCredential();
        credential.setAccessToken(user.getGoogleAccessToken());

        return new Calendar.Builder(
                GoogleNetHttpTransport.newTrustedTransport(),
                JacksonFactory.getDefaultInstance(),
                credential)
                .setApplicationName("EDT-Intelligent")
                .build();
    }

    // ── pull Google events into the local DB with deduplication ──────────────
    @Transactional
    public void pullEventsFromGoogle(User user) {
        if (user.getGoogleAccessToken() == null || user.getGoogleAccessToken().isBlank()) {
            log.debug("[PULL] Utilisateur {} sans token — ignoré.", user.getId());
            return;
        }

        ZoneId  zone        = ZoneId.of(defaultTimezone);
        Instant windowStart = Instant.now();
        Instant windowEnd   = Instant.now().plusSeconds(48L * 60 * 60);

        try {
            Calendar client = buildCalendarClient(user);

            Events events = client.events().list(CALENDAR_ID)
                    .setTimeMin(new DateTime(windowStart.toString()))
                    .setTimeMax(new DateTime(windowEnd.toString()))
                    .setSingleEvents(true)
                    .setOrderBy("startTime")
                    .execute();

            if (events.getItems() == null) {
                log.info("[PULL] Aucun événement Google pour l'utilisateur {}.", user.getId());
                return;
            }

            for (var gEvent : events.getItems()) {
                String        googleId = gEvent.getId();
                LocalDateTime start    = toLocalDateTime(gEvent.getStart(),  zone);
                LocalDateTime end      = toLocalDateTime(gEvent.getEnd(),    zone);

                Optional<Event> existing = eventRepository.findByGoogleEventId(googleId);

                if (existing.isPresent()) {
                    Event toUpdate = existing.get();
                    toUpdate.setStartTime(start);
                    toUpdate.setEndTime(end);
                    eventRepository.save(toUpdate);
                    log.debug("[PULL] Événement {} mis à jour.", googleId);
                } else {
                    Event newEvent = new Event("Occupé", start, end, user);
                    newEvent.setGoogleEventId(googleId);
                    eventRepository.save(newEvent);
                    log.info("[PULL] Nouvel événement importé (googleId={}).", googleId);
                }
            }

        } catch (IOException | GeneralSecurityException e) {
            log.error("[PULL] Erreur lors de l'import pour l'utilisateur {} : {}",
                      user.getId(), e.getMessage(), e);
        }
    }

    private LocalDateTime toLocalDateTime(EventDateTime gdt, ZoneId zone) {
        if (gdt.getDateTime() != null) {
            // full date-time (e.g. "2026-02-03T14:00:00+01:00")
            return Instant.ofEpochMilli(gdt.getDateTime().getValue())
                          .atZone(zone)
                          .toLocalDateTime();
        }
        // date-only (all-day event) — treat as midnight in the configured zone
        return Instant.ofEpochMilli(gdt.getDate().getValue())
                      .atZone(zone)
                      .toLocalDateTime();
    }
}