package com.example.backend.service;

import com.example.backend.exception.GoogleApiException;
import com.example.backend.model.Event;
import com.example.backend.model.User;
import com.example.backend.repository.EventRepository;
import com.example.backend.repository.UserRepository;

import com.google.api.client.googleapis.auth.oauth2.GoogleCredential;
import com.google.api.client.googleapis.auth.oauth2.GoogleRefreshTokenRequest;
import com.google.api.client.googleapis.auth.oauth2.GoogleTokenResponse;
import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport;
import com.google.api.client.http.javanet.NetHttpTransport;
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

/**
 * Service pour la gestion des interactions avec Google Calendar API.
 * 
 * AMÉLIORATION : Gestion automatique du rafraîchissement du token d'accès
 * quand il expire (401 Unauthorized).
 */
@Service
public class GoogleCalendarService {

    private static final Logger log        = LoggerFactory.getLogger(GoogleCalendarService.class);
    private static final String CALENDAR_ID = "primary";

    @Value("${google.calendar.timezone:Europe/Paris}")
    private String defaultTimezone;

    @Value("${google.client.id}")
    private String clientId;

    @Value("${google.client.secret}")
    private String clientSecret;

    private final EventRepository eventRepository;
    private final UserRepository userRepository;

    public GoogleCalendarService(EventRepository eventRepository, UserRepository userRepository) {
        this.eventRepository = eventRepository;
        this.userRepository = userRepository;
    }

    /**
     * AMÉLIORATION : Construit un client Calendar avec gestion automatique du token expiré.
     * 
     * Si le token est expiré, cette méthode tentera de le rafraîchir automatiquement
     * avant de créer le client.
     */
    public Calendar buildCalendarClient(User user) throws IOException, GeneralSecurityException {
        return buildCalendarClient(user, false);
    }

    /**
     * Version interne avec support du retry après refresh token.
     * 
     * CORRECTION CRITIQUE : Utilisation du Builder pour GoogleCredential
     * 
     * @param user L'utilisateur
     * @param isRetry true si c'est un retry après refresh du token
     */
    private Calendar buildCalendarClient(User user, boolean isRetry) throws IOException, GeneralSecurityException {
        // Récupérer la version la plus récente de l'utilisateur depuis la DB
        User freshUser = userRepository.findById(user.getId())
                .orElseThrow(() -> new RuntimeException("Utilisateur introuvable"));

        NetHttpTransport httpTransport = GoogleNetHttpTransport.newTrustedTransport();
        JacksonFactory jsonFactory = JacksonFactory.getDefaultInstance();
        
        GoogleCredential.Builder credentialBuilder = new GoogleCredential.Builder()
                .setTransport(httpTransport)
                .setJsonFactory(jsonFactory)
                .setClientSecrets(clientId, clientSecret);

        // Construire le credential
        GoogleCredential credential = credentialBuilder.build();
        
        // Configurer les tokens APRÈS la construction
        credential.setAccessToken(freshUser.getGoogleAccessToken());
        
        // Si on a un refresh token, le configurer aussi
        if (freshUser.getGoogleRefreshToken() != null && !freshUser.getGoogleRefreshToken().isBlank()) {
            credential.setRefreshToken(freshUser.getGoogleRefreshToken());
        }
        
        log.debug("[BUILDER] Client Calendar construit avec succès pour l'utilisateur {}", user.getId());

        return new Calendar.Builder(httpTransport, jsonFactory, credential)
                .setApplicationName("EDT-Intelligent")
                .build();
    }

    /**
     * Wrapper pour exécuter une opération Google Calendar avec retry automatique.
     * 
     * Si l'opération échoue avec un 401, on tente de rafraîchir le token et de réessayer.
     */
    private <T> T executeWithRetry(User user, CalendarOperation<T> operation) throws IOException, GeneralSecurityException {
        try {
            Calendar client = buildCalendarClient(user, false);
            return operation.execute(client);
            
        } catch (IOException e) {
            // Si erreur 401 (token expiré), tenter de rafraîchir
            if (e.getMessage() != null && e.getMessage().contains("401")) {
                log.warn("[GOOGLE-API] Token expiré détecté, tentative de rafraîchissement...");
                
                if (refreshAccessToken(user)) {
                    // Token rafraîchi, réessayer l'opération
                    log.info("[GOOGLE-API] Nouvelle tentative après rafraîchissement du token");
                    Calendar client = buildCalendarClient(user, true);
                    return operation.execute(client);
                } else {
                    // Impossible de rafraîchir le token
                    throw new GoogleApiException(
                        "Token Google expiré et impossible de le rafraîchir. L'utilisateur doit se reconnecter.",
                        e,
                        "TOKEN_EXPIRED",
                        false
                    );
                }
            }
            
            // Autres erreurs : relancer
            throw e;
        }
    }

    /**
     * Interface fonctionnelle pour les opérations Google Calendar.
     */
    @FunctionalInterface
    private interface CalendarOperation<T> {
        T execute(Calendar client) throws IOException;
    }

    /**
     * Exporte un événement local vers Google Calendar.
     * 
     * AMÉLIORATION : Utilise le mécanisme de retry automatique en cas de token expiré.
     */
    @Transactional
    public void pushEventToGoogle(Event event) {
        User user = event.getUser();

        if (user.getGoogleAccessToken() == null || user.getGoogleAccessToken().isBlank()) {
            log.warn("[PUSH] Utilisateur {} sans token Google — export ignoré.", user.getId());
            return;
        }

        try {
            com.google.api.services.calendar.model.Event googleEvent = convertToGoogleEvent(event);
            boolean isUpdate = (event.getGoogleEventId() != null && !event.getGoogleEventId().isBlank());

            // Utiliser executeWithRetry pour gérer automatiquement le token expiré
            com.google.api.services.calendar.model.Event result = executeWithRetry(user, client -> {
                if (isUpdate) {
                    log.debug("[PUSH] Mise à jour de l'événement '{}' sur Google Calendar (googleId={}).",
                             event.getSummary(), event.getGoogleEventId());
                    
                    return client.events()
                            .update(CALENDAR_ID, event.getGoogleEventId(), googleEvent)
                            .execute();
                    
                } else {
                    // Création d'un nouvel événement
                    log.debug("[PUSH] Création d'un nouveau événement '{}' sur Google Calendar.",
                             event.getSummary());
                    
                    return client.events()
                            .insert(CALENDAR_ID, googleEvent)
                            .execute();
                }
            });

            // Mise à jour des métadonnées après succès
            if (!isUpdate) {
                event.setGoogleEventId(result.getId());
                event.setSource(Event.EventSource.LOCAL);
            }
            
            event.setSyncStatus(Event.SyncStatus.SYNCED);
            event.setLastSyncedAt(java.time.LocalDateTime.now());
            eventRepository.save(event);

            log.info("[PUSH] Événement '{}' {} avec succès (googleId={})",
                     event.getSummary(), 
                     isUpdate ? "mis à jour" : "créé",
                     result.getId());

        } catch (GoogleApiException e) {
            // Erreur spécifique (token expiré, permissions, etc.)
            log.error("[PUSH] Erreur Google API : {}", e.getMessage());
            event.setSyncStatus(Event.SyncStatus.FAILED);
            eventRepository.save(event);
            throw e;
            
        } catch (IOException e) {
            String errorMsg = e.getMessage() != null ? e.getMessage() : "Erreur réseau inconnue";
            log.error("[PUSH] Erreur I/O lors de la communication avec Google : {}", errorMsg);
            
            event.setSyncStatus(Event.SyncStatus.FAILED);
            eventRepository.save(event);
            
            throw new GoogleApiException(
                "Erreur de communication avec Google Calendar",
                e,
                "IO_ERROR",
                true
            );
            
        } catch (GeneralSecurityException e) {
            log.error("[PUSH] Erreur de sécurité : {}", e.getMessage());
            throw new GoogleApiException(
                "Erreur de sécurité lors de la connexion à Google",
                e,
                "SECURITY_ERROR",
                false
            );
        }
    }

    /**
     * Supprime un événement de Google Calendar.
     * 
     * AMÉLIORATION : Utilise le mécanisme de retry automatique.
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

        try {
            // Utiliser executeWithRetry
            executeWithRetry(user, client -> {
                client.events()
                        .delete(CALENDAR_ID, event.getGoogleEventId())
                        .execute();
                return null; // Void operation
            });

            log.info("[DELETE] Événement '{}' supprimé de Google Calendar (googleId={}).",
                     event.getSummary(), event.getGoogleEventId());
            
            // Nettoyer le googleEventId local
            event.setGoogleEventId(null);
            event.setSyncStatus(Event.SyncStatus.SYNCED);
            eventRepository.save(event);

        } catch (GoogleApiException e) {
            log.error("[DELETE] Erreur Google API : {}", e.getMessage());
            throw e;
            
        } catch (IOException | GeneralSecurityException e) {
            log.error("[DELETE] Erreur lors de la suppression : {}", e.getMessage());
            throw new GoogleApiException(
                "Erreur lors de la suppression sur Google Calendar",
                e,
                "DELETE_ERROR",
                true
            );
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

        // Embed internal ID
        if (event.getId() != null) {
            String currentDesc = gEvent.getDescription() != null ? gEvent.getDescription() + "\n" : "";
            gEvent.setDescription(currentDesc + "EDT-ID:" + event.getId());
        }

        // Définir la couleur selon la catégorie
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

     /**
     * NOUVELLE MÉTHODE : Rafraîchit automatiquement le token d'accès Google.
     * 
     * @param user L'utilisateur dont on doit rafraîchir le token
     * @return true si le rafraîchissement a réussi, false sinon
     */
    @Transactional
    protected boolean refreshAccessToken(User user) {
        if (user.getGoogleRefreshToken() == null || user.getGoogleRefreshToken().isBlank()) {
            log.warn("[TOKEN-REFRESH] Pas de refresh token disponible pour l'utilisateur {}", user.getId());
            return false;
        }

        try {
            log.info("[TOKEN-REFRESH] Rafraîchissement du token pour l'utilisateur {}", user.getId());
            
            GoogleTokenResponse response = new GoogleRefreshTokenRequest(
                new NetHttpTransport(),
                JacksonFactory.getDefaultInstance(),
                user.getGoogleRefreshToken(),
                clientId,
                clientSecret
            ).execute();

            // Mise à jour du token d'accès
            user.setGoogleAccessToken(response.getAccessToken());
            
            // Certains refresh peuvent aussi renouveler le refresh token
            if (response.getRefreshToken() != null) {
                user.setGoogleRefreshToken(response.getRefreshToken());
                log.debug("[TOKEN-REFRESH] Refresh token également renouvelé");
            }
            
            userRepository.save(user);
            log.info("[TOKEN-REFRESH] Token rafraîchi avec succès pour l'utilisateur {}", user.getId());
            return true;
            
        } catch (IOException e) {
            log.error("[TOKEN-REFRESH] Échec du rafraîchissement du token : {}", e.getMessage());
            return false;
        }
    }
}