package com.example.backend.service;

import com.example.backend.model.User;
import com.example.backend.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.stream.Collectors;

@Component
public class CalendarSyncScheduler {

    private static final Logger log = LoggerFactory.getLogger(CalendarSyncScheduler.class);

    private final UserRepository         userRepository;
    private final GoogleCalendarService   googleCalendarService;
    private final CalendarImportService   calendarImportService;

    public CalendarSyncScheduler(UserRepository userRepository,
                                  GoogleCalendarService googleCalendarService,
                                  CalendarImportService calendarImportService) {
        this.userRepository          = userRepository;
        this.googleCalendarService   = googleCalendarService;
        this.calendarImportService   = calendarImportService;
    }

    /**
     * Job planifié toutes les 15 min (configurable via app.sync.rate).
     * Périmètre : uniquement les utilisateurs avec un token OAuth2 valide.
     */
    @Scheduled(fixedDelayString = "${app.sync.rate:900000}", initialDelay = 10000)
    public void syncAllUsers() {
        log.info("[SYNC] Démarrage du cycle de synchronisation.");

        List<User> eligibleUsers = userRepository.findAll().stream()
                .filter(u -> u.getGoogleAccessToken() != null
                          && !u.getGoogleAccessToken().isBlank())
                .collect(Collectors.toList());

        log.info("[SYNC] {} utilisateur(s) éligible(s).", eligibleUsers.size());

        for (User user : eligibleUsers) {
            try {
                // 1. Import des événements Google vers le backend
                calendarImportService.pullEventsFromGoogle(user);

                // 2. Export des événements locaux vers Google
                // On ne pousse les événements qui n'ont PAS encore de googleEventId
                user.getEvents().stream()
        .filter(e -> e.getGoogleEventId() == null)
        .forEach(googleCalendarService::pushEventToGoogle);

            } catch (Exception e) {
                // Isolation : une erreur sur un utilisateur ne bloque pas les autres
                log.error("[SYNC] Erreur pour l'utilisateur {} : {}",
                          user.getId(), e.getMessage(), e);
            }
        }

        log.info("[SYNC] Cycle de synchronisation terminé.");
    }
}