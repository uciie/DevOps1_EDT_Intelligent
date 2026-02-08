package com.example.backend.service;

import com.example.backend.model.Event;
import com.example.backend.model.ActivityCategory;
import com.example.backend.model.User;
import com.example.backend.repository.EventRepository;
import com.example.backend.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class CalendarSyncService {

    private static final Logger log = LoggerFactory.getLogger(CalendarSyncService.class);

    private final UserRepository userRepository;
    private final EventRepository eventRepository;
    private final GoogleCalendarService googleCalendarService;
    private final CalendarImportService calendarImportService;

    public CalendarSyncService(UserRepository userRepository, 
                               EventRepository eventRepository,
                               GoogleCalendarService googleCalendarService, 
                               CalendarImportService calendarImportService) {
        this.userRepository = userRepository;
        this.eventRepository = eventRepository;
        this.googleCalendarService = googleCalendarService;
        this.calendarImportService = calendarImportService;
    }

    /**
     * Synchronise un utilisateur avec Google Calendar (bidirectionnel).
     * 
     * Étapes :
     * 1. Import : Récupère les événements Google vers la base locale
     * 2. Export : Envoie les événements locaux modifiés vers Google
     * 
     * @param userId L'identifiant de l'utilisateur
     * @throws Exception en cas d'erreur de synchronisation
     */
    @Transactional
    public void syncUser(Long userId) throws Exception {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("Utilisateur non trouvé"));

        if (user.getGoogleAccessToken() == null || user.getGoogleAccessToken().isBlank()) {
            throw new RuntimeException("Le compte Google n'est pas lié.");
        }

        log.info("[SYNC] Début de la synchronisation bidirectionnelle pour l'utilisateur {}", userId);

        // ── ÉTAPE 1 : IMPORT (Google → Local) ──────────────────────────────
        log.debug("[SYNC] Étape 1/2 : Import des événements Google");
        int importedCount = calendarImportService.pullEventsFromGoogle(user);
        log.info("[SYNC] {} événements importés depuis Google Calendar", importedCount);

        // ── ÉTAPE 2 : EXPORT (Local → Google) ──────────────────────────────
        log.debug("[SYNC] Étape 2/2 : Export des événements locaux");
        int exportedCount = pushLocalEventsToGoogle(user);
        log.info("[SYNC] {} événements exportés vers Google Calendar", exportedCount);

        log.info("[SYNC] Synchronisation terminée : {} importés, {} exportés", 
                 importedCount, exportedCount);
    }

    /**
     * Détecte et exporte les événements locaux qui nécessitent une synchronisation vers Google.
     * 
     * Critères de synchronisation :
     * - Événements créés localement (source = LOCAL)
     * - Événements avec statut PENDING (modifications non synchronisées)
     * - Événements sans googleEventId (jamais envoyés à Google)
     * - Événements modifiés après leur dernière synchronisation
     * 
     * @param user L'utilisateur dont on synchronise les événements
     * @return Le nombre d'événements exportés
     */
    @Transactional
    public int pushLocalEventsToGoogle(User user) {
        // Récupérer tous les événements locaux qui doivent être synchronisés
        List<Event> eventsToSync = getLocalEventsNeedingSync(user);

        if (eventsToSync.isEmpty()) {
            log.debug("[EXPORT] Aucun événement local à synchroniser pour l'utilisateur {}", user.getId());
            return 0;
        }

        log.info("[EXPORT] {} événements locaux à synchroniser pour l'utilisateur {}", 
                 eventsToSync.size(), user.getId());

        int successCount = 0;

        for (Event event : eventsToSync) {
            try {
                // Vérifier si l'événement doit être supprimé de Google
                if (event.getStatus() == Event.EventStatus.PENDING_DELETION) {
                    if (event.getGoogleEventId() != null) {
                        googleCalendarService.deleteEventFromGoogle(event);
                        log.debug("[EXPORT] Événement '{}' supprimé de Google", event.getSummary());
                    }
                    // Supprimer l'événement local
                    eventRepository.delete(event);
                } else {
                    // Créer ou mettre à jour l'événement sur Google
                    googleCalendarService.pushEventToGoogle(event);
                    successCount++;
                    log.debug("[EXPORT] Événement '{}' synchronisé vers Google", event.getSummary());
                }
            } catch (Exception e) {
                log.error("[EXPORT] Erreur lors de la synchronisation de l'événement '{}' : {}", 
                         event.getSummary(), e.getMessage());
                // Marquer l'événement en conflit
                event.setSyncStatus(Event.SyncStatus.CONFLICT);
                eventRepository.save(event);
            }
        }

        return successCount;
    }

    /**
     * Récupère les événements locaux qui nécessitent une synchronisation vers Google.
     * 
     * @param user L'utilisateur concerné
     * @return Liste des événements à synchroniser
     */
    private List<Event> getLocalEventsNeedingSync(User user) {
        // Récupérer tous les événements de l'utilisateur
        List<Event> allEvents = eventRepository.findByUser_Id(user.getId());

        return allEvents.stream()
                .filter(event -> needsSyncToGoogle(event))
                .collect(Collectors.toList());
    }

    /**
     * Détermine si un événement doit être synchronisé vers Google.
     * 
     * @param event L'événement à évaluer
     * @return true si l'événement doit être synchronisé
     */
    private boolean needsSyncToGoogle(Event event) {
        // Événements en attente de suppression
        if (event.getStatus() == Event.EventStatus.PENDING_DELETION) {
            return true;
        }

        // Événements créés localement sans googleEventId
        if (event.getSource() == Event.EventSource.LOCAL && event.getGoogleEventId() == null) {
            return true;
        }

        // Événements avec statut de synchronisation PENDING
        if (event.getSyncStatus() == Event.SyncStatus.PENDING) {
            return true;
        }

        // Événements modifiés après leur dernière synchronisation
        // (On considère qu'un événement modifié dans les dernières 5 minutes doit être re-synchronisé)
        if (event.getLastSyncedAt() != null) {
            LocalDateTime fiveMinutesAgo = LocalDateTime.now().minusMinutes(5);
            // Si l'événement a été créé/modifié APRÈS la dernière synchro, il faut le re-synchroniser
            // Note: Vous devrez peut-être ajouter un champ `lastModifiedAt` pour tracker ça précisément
            return false; // Pour l'instant, on fait confiance au syncStatus
        }

        return false;
    }

    /**
     * Marque un événement comme devant être synchronisé.
     * Utile après une modification locale d'un événement.
     * 
     * @param eventId L'identifiant de l'événement
     */
    @Transactional
    public void markEventForSync(Long eventId) {
        Event event = eventRepository.findById(eventId)
                .orElseThrow(() -> new IllegalArgumentException("Événement non trouvé"));
        
        event.setSyncStatus(Event.SyncStatus.PENDING);
        eventRepository.save(event);
        
        log.debug("[SYNC] Événement '{}' marqué pour synchronisation", event.getSummary());
    }

    /**
     * Force une synchronisation immédiate d'un événement spécifique.
     * 
     * @param eventId L'identifiant de l'événement
     * @throws Exception en cas d'erreur
     */
    @Transactional
    public void syncEvent(Long eventId) throws Exception {
        Event event = eventRepository.findById(eventId)
                .orElseThrow(() -> new IllegalArgumentException("Événement non trouvé"));

        User user = event.getUser();
        
        if (user.getGoogleAccessToken() == null || user.getGoogleAccessToken().isBlank()) {
            throw new RuntimeException("Le compte Google n'est pas lié.");
        }

        log.info("[SYNC-EVENT] Synchronisation de l'événement '{}' pour l'utilisateur {}", 
                 event.getSummary(), user.getId());

        googleCalendarService.pushEventToGoogle(event);
        
        log.info("[SYNC-EVENT] Événement synchronisé avec succès");
    }

    // Dans CalendarSyncService.java, méthode convertToGoogleEvent
    /**
     * Convertit un événement interne en événement Google Calendar.
     * 
     * @param internalEvent L'événement interne
     * @return L'événement Google Calendar correspondant
     */
    public com.google.api.services.calendar.model.Event convertToGoogleEvent(Event internalEvent) {
        var gEvent = new com.google.api.services.calendar.model.Event()
            .setSummary(internalEvent.getSummary()) 
            .setDescription("Synchronisé depuis Smart Scheduler");

        // Correction de l'erreur : utilisez ActivityCategory au lieu de EventType
        if (internalEvent.getCategory() != null && ActivityCategory.FOCUS.equals(internalEvent.getCategory())) {
            gEvent.setTransparency("opaque"); // "opaque" = Occupé dans Google Calendar
        }
        
        return gEvent;
    }
}