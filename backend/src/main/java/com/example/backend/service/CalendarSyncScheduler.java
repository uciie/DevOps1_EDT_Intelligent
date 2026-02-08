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

    private final UserRepository userRepository;
    private final CalendarSyncService calendarSyncService;

    public CalendarSyncScheduler(UserRepository userRepository,
                                  CalendarSyncService calendarSyncService) {
        this.userRepository = userRepository;
        this.calendarSyncService = calendarSyncService;
    }

    /**
     * Job planifié toutes les 15 min (configurable via app.sync.rate).
     * Périmètre : uniquement les utilisateurs avec un token OAuth2 valide.
     * 
     * Effectue une synchronisation bidirectionnelle :
     * - Import des événements Google → Local
     * - Export des événements Local → Google
     */
    @Scheduled(fixedDelayString = "${app.sync.rate:900000}", initialDelay = 10000)
    public void syncAllUsers() {
        log.info("[SYNC-SCHEDULER] Démarrage du cycle de synchronisation bidirectionnelle.");

        List<User> eligibleUsers = userRepository.findAll().stream()
                .filter(u -> u.getGoogleAccessToken() != null
                          && !u.getGoogleAccessToken().isBlank())
                .collect(Collectors.toList());

        log.info("[SYNC-SCHEDULER] {} utilisateur(s) éligible(s).", eligibleUsers.size());

        int totalSuccess = 0;
        int totalFailures = 0;

        for (User user : eligibleUsers) {
            try {
                // Synchronisation bidirectionnelle complète
                calendarSyncService.syncUser(user.getId());
                totalSuccess++;
                
            } catch (Exception e) {
                // Isolation : une erreur sur un utilisateur ne bloque pas les autres
                log.error("[SYNC-SCHEDULER] Erreur pour l'utilisateur {} : {}",
                          user.getId(), e.getMessage(), e);
                totalFailures++;
            }
        }

        log.info("[SYNC-SCHEDULER] Cycle de synchronisation terminé. " +
                 "Succès : {}, Échecs : {}", totalSuccess, totalFailures);
    }
}