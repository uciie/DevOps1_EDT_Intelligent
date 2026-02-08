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
import java.util.List;

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
    /**
     * Exporte un événement local vers Google Calendar.
     * Si l'événement possède déjà un googleEventId, il sera mis à jour.
     * Sinon, un nouvel événement sera créé sur Google Calendar.
     * 
     * @param event L'événement à exporter
     */
    @Transactional
    public void pushEventToGoogle(Event event) {
        User user = event.getUser();

        if (user.getGoogleAccessToken() == null || user.getGoogleAccessToken().isBlank()) {
            log.warn("[PUSH] Utilisateur {} sans token Google — export ignoré.", user.getId());
            return;
        }

        // Vérifier si c'est une création ou une mise à jour
        boolean isUpdate = event.getGoogleEventId() != null && !event.getGoogleEventId().isBlank();
        
        com.google.api.services.calendar.model.Event googleEvent = convertToGoogleEvent(event);

        for (int attempt = 1; attempt <= MAX_RETRIES; attempt++) {
            try {
                Calendar client = buildCalendarClient(user);
                com.google.api.services.calendar.model.Event result;

                if (isUpdate) {
                    // Mise à jour d'un événement existant
                    log.debug("[PUSH] Mise à jour de l'événement '{}' (googleId={}).",
                             event.getSummary(), event.getGoogleEventId());
                    
                    result = client.events()
                            .update(CALENDAR_ID, event.getGoogleEventId(), googleEvent)
                            .execute();
                    
                    log.info("[PUSH] Événement '{}' mis à jour avec succès sur Google Calendar.",
                             event.getSummary());
                } else {
                    // Création d'un nouvel événement
                    log.debug("[PUSH] Création d'un nouveau événement '{}' sur Google Calendar.",
                             event.getSummary());
                    
                    result = client.events()
                            .insert(CALENDAR_ID, googleEvent)
                            .execute();

                    // Persist the Google-assigned ID for later deduplication
                    event.setGoogleEventId(result.getId());
                    event.setSource(Event.EventSource.LOCAL);
                    event.setSyncStatus(Event.SyncStatus.SYNCED);
                    event.setLastSyncedAt(java.time.LocalDateTime.now());
                    eventRepository.save(event);

                    log.info("[PUSH] Événement '{}' créé avec succès (googleId={}).",
                             event.getSummary(), result.getId());
                }
                
                return; // success — exit retry loop

            } catch (IOException e) {
                // Détection spécifique du manque de permissions
                if (e.getMessage() != null && e.getMessage().contains("insufficientPermissions")) {
                    log.error("[PUSH] Permissions Google insuffisantes pour l'utilisateur {}. " +
                            "Action requise : Re-connexion du compte Google avec les droits d'écriture.", user.getId());
                    // On marque l'événement pour ne plus retenter inutilement
                    event.setSyncStatus(Event.SyncStatus.FAILED);
                    eventRepository.save(event);
                    return; 
                }
                
                log.warn("[PUSH] Tentative {}/{} échouée pour '{}' : {}",
                         attempt, MAX_RETRIES, event.getSummary(), e.getMessage());
                if (attempt == MAX_RETRIES) {
                    log.error("[PUSH] Échec définitif après {} tentatives.", MAX_RETRIES, e);
                    // Marquer l'événement comme en erreur de synchronisation
                    event.setSyncStatus(Event.SyncStatus.CONFLICT);
                    eventRepository.save(event);
                }
            } catch (GeneralSecurityException e) {
                // transport-level security failure — no point retrying
                log.error("[PUSH] Erreur de sécurité (transport) : {}", e.getMessage());
                event.setSyncStatus(Event.SyncStatus.CONFLICT);
                eventRepository.save(event);
                return;
            }
        }
    }

    /**
     * Supprime un événement de Google Calendar.
     * 
     * @param event L'événement à supprimer
     */
    @Transactional
    public void deleteEventFromGoogle(Event event) {
        User user = event.getUser();

        if (user.getGoogleAccessToken() == null || user.getGoogleAccessToken().isBlank()) {
            log.warn("[DELETE] Utilisateur {} sans token Google — suppression ignorée.", user.getId());
            return;
        }

        if (event.getGoogleEventId() == null || event.getGoogleEventId().isBlank()) {
            log.debug("[DELETE] Événement '{}' n'a pas de googleEventId — déjà supprimé ou jamais synchronisé.",
                     event.getSummary());
            return;
        }

        for (int attempt = 1; attempt <= MAX_RETRIES; attempt++) {
            try {
                Calendar client = buildCalendarClient(user);

                client.events()
                        .delete(CALENDAR_ID, event.getGoogleEventId())
                        .execute();

                log.info("[DELETE] Événement '{}' supprimé de Google Calendar (googleId={}).",
                         event.getSummary(), event.getGoogleEventId());
                
                // Nettoyer le googleEventId local
                event.setGoogleEventId(null);
                event.setSyncStatus(Event.SyncStatus.SYNCED);
                eventRepository.save(event);
                
                return; // success — exit retry loop

            } catch (IOException e) {
                log.warn("[DELETE] Tentative {}/{} échouée pour '{}' : {}",
                         attempt, MAX_RETRIES, event.getSummary(), e.getMessage());
                if (attempt == MAX_RETRIES) {
                    log.error("[DELETE] Échec définitif de suppression après {} tentatives.", MAX_RETRIES, e);
                }
            } catch (GeneralSecurityException e) {
                log.error("[DELETE] Erreur de sécurité (transport) : {}", e.getMessage());
                return;
            }
        }
    }

    /**
     * Exporte une liste d'événements vers Google Calendar.
     * 
     * @param events Liste des événements à exporter
     * @param user L'utilisateur propriétaire des événements
     * @return Le nombre d'événements exportés avec succès
     */
    @Transactional
    public int pushEventsToGoogle(List<Event> events, User user) {
        int successCount = 0;
        
        for (Event event : events) {
            try {
                pushEventToGoogle(event);
                successCount++;
            } catch (Exception e) {
                log.error("[PUSH-BATCH] Erreur lors de l'export de l'événement '{}' : {}",
                         event.getSummary(), e.getMessage());
            }
        }
        
        log.info("[PUSH-BATCH] {} événements exportés sur {} pour l'utilisateur {}",
                 successCount, events.size(), user.getId());
        
        return successCount;
    }

    // ── LocalDateTime  →  Google EventDateTime ──────────────────────────────
    /**
     * Convertit un événement local en événement Google Calendar.
     * 
     * @param event L'événement local à convertir
     * @return L'événement Google Calendar correspondant
     */
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

        // Ajouter la description si présente
        if (event.getTask() != null && event.getTask().getTitle() != null) {
            gEvent.setDescription("Tâche : " + event.getTask().getTitle());
        }

        // Ajouter la localisation si présente
        if (event.getLocation() != null && event.getLocation().getAddress() != null) {
            gEvent.setLocation(event.getLocation().getAddress());
        }

        // Embed internal ID so a later pull can recognise events we already own
        if (event.getId() != null) {
            String currentDesc = gEvent.getDescription() != null ? gEvent.getDescription() + "\n" : "";
            gEvent.setDescription(currentDesc + "EDT-ID:" + event.getId());
        }

        // Définir la couleur ou la transparence selon la catégorie
        if (event.getCategory() != null) {
            switch (event.getCategory()) {
                case FOCUS:
                    gEvent.setTransparency("opaque"); // Occupé
                    gEvent.setColorId("9"); // Bleu foncé pour focus
                    break;
                case TRAVAIL:
                    gEvent.setColorId("11"); // Rouge
                    break;
                case ETUDE:
                    gEvent.setColorId("5"); // Jaune
                    break;
                case SPORT:
                    gEvent.setColorId("10"); // Vert
                    break;
                case LOISIR:
                    gEvent.setColorId("7"); // Cyan
                    break;
                default:
                    gEvent.setColorId("8"); // Gris
            }
        }

        return gEvent;
    }
}