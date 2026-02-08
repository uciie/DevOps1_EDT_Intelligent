package com.example.backend.service;

import com.example.backend.model.Event;
import com.example.backend.model.Location;
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
import java.util.stream.Collectors;

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
                
                // ── GESTION DES SUPPRESSIONS même si la liste est vide ──────────────────────
                // Si Google ne retourne aucun événement, tous les événements locaux de source GOOGLE
                // doivent être considérés comme supprimés
                int deletedCount = deleteOrphanedGoogleEvents(user, events.getItems());
                if (deletedCount > 0) {
                    log.info("[PULL] {} événement(s) supprimé(s) car absents de Google Calendar", deletedCount);
                }
                
                return 0;
            }

            log.info("[PULL] {} événement(s) trouvé(s) dans Google Calendar pour l'utilisateur {}", 
                    events.getItems().size(), user.getId());
            
            for (var gEvent : events.getItems()) {
                String        googleId = gEvent.getId();
                String        summary  = gEvent.getSummary() != null ? gEvent.getSummary() : "Sans titre";
                LocalDateTime start    = toLocalDateTime(gEvent.getStart(),  zone);
                LocalDateTime end      = toLocalDateTime(gEvent.getEnd(),    zone);

                // Récupération de la localisation
                String googleLocation = gEvent.getLocation();

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

                    // Mise à jour de la localisation
                    if (updateLocationIfNeeded(toUpdate, googleLocation)) {
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
                    newEvent.setSource(Event.EventSource.GOOGLE);
                    
                    // Mise à jour de la localisation lors de la création
                    if (googleLocation != null && !googleLocation.trim().isEmpty()) {
                        Location location = new Location();
                        location.setAddress(googleLocation);
                        newEvent.setLocation(location);
                        log.debug("[PULL] Localisation définie : {}", googleLocation);
                    }
                    
                    eventRepository.save(newEvent);
                    importedCount++;
                    log.debug("[PULL] Nouvel événement importé (googleId={}, titre={}).", googleId, summary);
                }
            }

            log.info("[PULL] Synchronisation terminée pour l'utilisateur {} : {} nouveaux, {} mis à jour", 
                    user.getId(), importedCount, updatedCount);

            // ── GESTION DES SUPPRESSIONS (Google → Local) ───────────────────────────────
            // Si un événement a été supprimé sur Google, il ne figure plus dans la liste retournée.
            // On doit donc identifier et supprimer les événements locaux orphelins.
            int deletedCount = deleteOrphanedGoogleEvents(user, events.getItems());
            
            if (deletedCount > 0) {
                log.info("[PULL] {} événement(s) supprimé(s) car absents de Google Calendar", deletedCount);
            }

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
     * Supprime les événements locaux qui proviennent de Google mais qui n'existent plus sur Google Calendar.
     * 
     * Cette méthode compare les googleEventId présents en base locale avec ceux retournés par l'API Google.
     * Les événements locaux orphelins (source = GOOGLE mais absents de la liste Google) sont supprimés.
     * 
     * @param user L'utilisateur concerné
     * @param googleEvents La liste des événements actuellement présents sur Google Calendar
     * @return Le nombre d'événements supprimés
     */
    @Transactional
    private int deleteOrphanedGoogleEvents(User user, List<com.google.api.services.calendar.model.Event> googleEvents) {
        // 1. Extraire les googleEventId actuellement sur Google
        List<String> currentGoogleIds = googleEvents != null ? googleEvents.stream()
                .map(com.google.api.services.calendar.model.Event::getId)
                .filter(id -> id != null && !id.trim().isEmpty())
                .collect(Collectors.toList()) : List.of();

        // 2. Récupérer tous les événements locaux de l'utilisateur ayant une source GOOGLE
        List<Event> localGoogleEvents = eventRepository.findByUser_Id(user.getId()).stream()
                .filter(e -> e.getSource() == Event.EventSource.GOOGLE)
                .filter(e -> e.getGoogleEventId() != null && !e.getGoogleEventId().trim().isEmpty())
                .collect(Collectors.toList());

        // 3. Identifier les événements locaux qui ne sont plus sur Google
        List<Event> orphanedEvents = localGoogleEvents.stream()
                .filter(localEvent -> !currentGoogleIds.contains(localEvent.getGoogleEventId()))
                .collect(Collectors.toList());

        // 4. Supprimer les événements orphelins
        int deletedCount = 0;
        for (Event orphan : orphanedEvents) {
            try {
                log.debug("[PULL] Suppression de l'événement orphelin '{}' (googleId={})", 
                         orphan.getSummary(), orphan.getGoogleEventId());
                eventRepository.delete(orphan);
                deletedCount++;
            } catch (Exception e) {
                log.error("[PULL] Erreur lors de la suppression de l'événement orphelin {} : {}", 
                         orphan.getId(), e.getMessage());
            }
        }

        return deletedCount;
    }

    /**
     * Met à jour la localisation d'un événement si nécessaire.
     * 
     * @param event L'événement à mettre à jour
     * @param googleLocation La localisation provenant de Google Calendar
     * @return true si la localisation a été modifiée, false sinon
     */
    private boolean updateLocationIfNeeded(Event event, String googleLocation) {
        // Cas 1: Google a une localisation mais pas l'événement local
        if (googleLocation != null && !googleLocation.trim().isEmpty()) {
            if (event.getLocation() == null) {
                Location location = new Location();
                location.setAddress(googleLocation);
                event.setLocation(location);
                log.debug("[PULL] Localisation ajoutée : {}", googleLocation);
                return true;
            }
            // Cas 2: Les deux ont une localisation mais elle a changé
            else if (!googleLocation.equals(event.getLocation().getAddress())) {
                event.getLocation().setAddress(googleLocation);
                log.debug("[PULL] Localisation mise à jour : {} → {}", 
                         event.getLocation().getAddress(), googleLocation);
                return true;
            }
        }
        // Cas 3: Google n'a plus de localisation mais l'événement local en a une
        else if (event.getLocation() != null) {
            event.setLocation(null);
            log.debug("[PULL] Localisation supprimée");
            return true;
        }
        
        return false;
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