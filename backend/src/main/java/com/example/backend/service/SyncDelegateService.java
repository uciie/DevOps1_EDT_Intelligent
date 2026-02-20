package com.example.backend.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/**
 * Bean séparé pour exécuter la synchronisation Google dans sa propre transaction.
 *
 * POURQUOI ce bean existe :
 * Spring AOP ne peut intercepter @Transactional que si l'appel passe par le proxy.
 * Un appel `this.methode()` depuis la même classe contourne le proxy et ignore la
 * propagation REQUIRES_NEW. En déplaçant la méthode dans un bean distinct, l'appel
 * passe obligatoirement par le proxy, et REQUIRES_NEW est respecté.
 */
@Service
public class SyncDelegateService {

    private static final Logger log = LoggerFactory.getLogger(SyncDelegateService.class);

    private final CalendarSyncService calendarSyncService;

    public SyncDelegateService(CalendarSyncService calendarSyncService) {
        this.calendarSyncService = calendarSyncService;
    }

    /**
     * Synchronise Google Calendar dans une transaction totalement indépendante.
     *
     * La propagation REQUIRES_NEW garantit que :
     * 1. Si cette méthode échoue → son rollback N'AFFECTE PAS la transaction appelante
     * 2. La sauvegarde locale (eventRepository.save) est déjà committée avant l'appel
     *
     * @param userId ID de l'utilisateur à synchroniser
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW, noRollbackFor = Exception.class)
    public void syncGoogleCalendarInNewTransaction(Long userId) {
        try {
            log.debug("[SYNC-DELEGATE] Synchronisation Google dans nouvelle transaction pour userId={}", userId);
            calendarSyncService.syncUser(userId);
            log.info("[SYNC-DELEGATE] Synchronisation Google réussie pour userId={}", userId);
        } catch (Exception e) {
            log.warn("[SYNC-DELEGATE] Échec synchronisation Google (non-bloquant) pour userId={} : {}",
                     userId, e.getMessage());
            if (log.isDebugEnabled()) {
                log.debug("[SYNC-DELEGATE] Stack trace:", e);
            }
        }
    }
}