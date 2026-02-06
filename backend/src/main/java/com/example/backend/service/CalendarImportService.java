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

    @Value("${google.calendar.sync.window.days:2}")
    private int syncWindowDays;

    private final EventRepository eventRepository;
    private final ICalendarParser  parser;

    public CalendarImportService(EventRepository eventRepository, ICalendarParser parser) {
        this.eventRepository = eventRepository;
        this.parser          = parser;
    }

    // ── existing ICS file import (unchanged) ────────────────────────────────
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
    /**
     * Synchronise les événements Google Calendar vers la base de données locale.
     * 
     * @param user L'utilisateur dont on synchronise les événements
     * @return Le nombre d'événements importés/mis à jour
     * @throws RuntimeException en cas d'erreur de synchronisation
     */
    @Transactional
    public int pullEventsFromGoogle(User user) {
        if (user.getGoogleAccessToken() == null || user.getGoogleAccessToken().isBlank()) {
            log.debug("[PULL] Utilisateur {} sans token – ignoré.", user.getId());
            throw new RuntimeException("Token Google non disponible");
        }

        // 1. Définir une date de début loin dans le passé (ex: il y a 1 an ou 30 jours)
        // Cela permet de récupérer les événements même si l'horloge du serveur est déréglée (2026)
        long pastTime = System.currentTimeMillis() - (365L * 24 * 60 * 60 * 1000); // -1 an
        
        ZoneId  zone        = ZoneId.of(defaultTimezone);
        Instant windowStart = Instant.now();
        Instant windowEnd   = Instant.now().plusSeconds(syncWindowDays * 24L * 60 * 60);

        int importedCount = 0;
        int updatedCount = 0;

        try {
            Calendar client = buildCalendarClient(user);

            Events events = client.events().list(CALENDAR_ID)
                    .setMaxResults(20) // on prend les 20 événements les plus récents pour limiter la charge
                    .setTimeMin(new DateTime(pastTime))
                    .setOrderBy("startTime")
                    .setSingleEvents(true)
                    .execute();

            List<com.google.api.services.calendar.model.Event> items = events.getItems();
            log.info("[PULL] Requête envoyée avec timeMin = {}", new DateTime(pastTime));
            log.info("[PULL] Nombre d'événements trouvés : {}", items.size());

            if (events.getItems() == null || events.getItems().isEmpty()) {
                log.info("[PULL] Aucun événement Google pour l'utilisateur {}.", user.getId());
                return 0;
            }

            log.info("[PULL] {} événement(s) trouvé(s) dans Google Calendar pour l'utilisateur {}", 
                    events.getItems().size(), user.getId());
            
            for (var gEvent : events.getItems()) {
                String        googleId = gEvent.getId();
                String        summary  = gEvent.getSummary() != null ? gEvent.getSummary() : "Sans titre";
                LocalDateTime start    = toLocalDateTime(gEvent.getStart(),  zone);
                LocalDateTime end      = toLocalDateTime(gEvent.getEnd(),    zone);

                Optional<Event> existing = eventRepository.findByGoogleEventId(googleId);

                if (existing.isPresent()) {
                    Event toUpdate = existing.get();
                    boolean hasChanged = false;

                    if (!toUpdate.getStartTime().equals(start)) {
                        toUpdate.setStartTime(start);
                        hasChanged = true;
                    }
                    if (!toUpdate.getEndTime().equals(end)) {
                        toUpdate.setEndTime(end);
                        hasChanged = true;
                    }
                    if (!toUpdate.getSummary().equals(summary)) {
                        toUpdate.setSummary(summary);
                        hasChanged = true;
                    }

                    if (hasChanged) {
                        eventRepository.save(toUpdate);
                        updatedCount++;
                        log.debug("[PULL] Événement {} mis à jour.", googleId);
                    }
                } else {
                    Event newEvent = new Event(summary, start, end, user);
                    newEvent.setGoogleEventId(googleId);
                    eventRepository.save(newEvent);
                    importedCount++;
                    log.debug("[PULL] Nouvel événement importé (googleId={}, titre={}).", googleId, summary);
                }
            }

            log.info("[PULL] Synchronisation terminée pour l'utilisateur {} : {} nouveaux, {} mis à jour", 
                    user.getId(), importedCount, updatedCount);

            return importedCount + updatedCount;

        } catch (IOException e) {
            log.error("[PULL] Erreur I/O lors de l'import pour l'utilisateur {} : {}",
                      user.getId(), e.getMessage());
            throw new RuntimeException("Erreur de communication avec Google Calendar: " + e.getMessage(), e);
            
        } catch (GeneralSecurityException e) {
            log.error("[PULL] Erreur de sécurité lors de l'import pour l'utilisateur {} : {}",
                      user.getId(), e.getMessage());
            throw new RuntimeException("Erreur de sécurité lors de la connexion à Google", e);
            
        } catch (Exception e) {
            log.error("[PULL] Erreur inattendue lors de l'import pour l'utilisateur {} : {}",
                      user.getId(), e.getMessage(), e);
            throw new RuntimeException("Erreur inattendue lors de la synchronisation", e);
        }
    }

    /**
     * Convertit un EventDateTime Google en LocalDateTime.
     */
    private LocalDateTime toLocalDateTime(EventDateTime gdt, ZoneId zone) {
        if (gdt == null) {
            log.warn("[PULL] EventDateTime null rencontré");
            return LocalDateTime.now();
        }

        if (gdt.getDateTime() != null) {
            // full date-time (e.g. "2026-02-03T14:00:00+01:00")
            return Instant.ofEpochMilli(gdt.getDateTime().getValue())
                          .atZone(zone)
                          .toLocalDateTime();
        }
        
        // date-only (all-day event) – treat as midnight in the configured zone
        if (gdt.getDate() != null) {
            return Instant.ofEpochMilli(gdt.getDate().getValue())
                          .atZone(zone)
                          .toLocalDateTime();
        }

        log.warn("[PULL] EventDateTime sans date ni dateTime, utilisation de l'heure actuelle");
        return LocalDateTime.now();
    }
}