package com.example.backend.controller;

import com.example.backend.dto.SyncConflictDTO;
import com.example.backend.exception.GoogleApiException;
import com.example.backend.exception.SyncConflictException;
import com.example.backend.model.User;
import com.example.backend.repository.UserRepository;
import com.example.backend.service.CalendarSyncService;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

/**
 * Contrôleur pour la synchronisation manuelle du calendrier Google.
 */
@RestController
@RequestMapping("/api/calendar")
public class CalendarSyncController {

    private static final Logger log = LoggerFactory.getLogger(CalendarSyncController.class);

    private final UserRepository userRepository;
    private final CalendarSyncService calendarSyncService;

    public CalendarSyncController(UserRepository userRepository,
                                  CalendarSyncService calendarSyncService) {
        this.userRepository = userRepository;
        this.calendarSyncService = calendarSyncService;
    }

    /**
     * Force une synchronisation immédiate bidirectionnelle (Google ↔ Local).
     * 
     * Endpoint: POST /api/calendar/sync/pull/{userId}
     * 
     * Cette méthode effectue :
     * 1. Détection des conflits de créneaux
     * 2. Import des événements Google → Local (si pas de conflits)
     * 3. Export des événements Local → Google (si pas de conflits)
     * 
     * @param userId L'identifiant de l'utilisateur
     * @return Réponse JSON avec le statut de la synchronisation ou les conflits détectés
     */
    @PostMapping("/sync/pull/{userId}")
    public ResponseEntity<Map<String, Object>> pullNow(@PathVariable Long userId) {
        Map<String, Object> response = new HashMap<>();
        
        try {
            log.info("[SYNC-MANUAL] Début de la synchronisation manuelle pour l'utilisateur {}", userId);
            
            // Vérification de l'existence de l'utilisateur
            User user = userRepository.findById(userId)
                    .orElseThrow(() -> new RuntimeException("Utilisateur introuvable : " + userId));

            // Vérification du token Google
            if (user.getGoogleAccessToken() == null || user.getGoogleAccessToken().isBlank()) {
                log.warn("[SYNC-MANUAL] Utilisateur {} sans token Google", userId);
                response.put("success", false);
                response.put("message", "Compte Google non lié. Veuillez vous connecter à Google Calendar.");
                response.put("errorCode", "NO_GOOGLE_TOKEN");
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(response);
            }

            // Exécution de la synchronisation BIDIRECTIONNELLE complète avec détection de conflits
            calendarSyncService.syncUser(userId);

            log.info("[SYNC-MANUAL] Synchronisation bidirectionnelle réussie pour l'utilisateur {}", userId);
            response.put("success", true);
            response.put("message", "Synchronisation Google Calendar effectuée avec succès (bidirectionnelle)");
            response.put("userId", userId);
            response.put("timestamp", System.currentTimeMillis());
            return ResponseEntity.ok(response);

        } catch (SyncConflictException e) {
            // ── GESTION DES CONFLITS DE CRÉNEAUX ─────────────────────────────
            log.warn("[SYNC-MANUAL] Conflits détectés pour l'utilisateur {} : {}", 
                     userId, e.getMessage());
            
            SyncConflictDTO conflicts = e.getConflictDetails();
            
            response.put("success", false);
            response.put("message", "Des conflits de créneaux ont été détectés");
            response.put("errorCode", "SCHEDULE_CONFLICTS");
            response.put("conflicts", conflicts.getConflicts());
            response.put("conflictCount", conflicts.getConflicts().size());
            
            return ResponseEntity.status(HttpStatus.CONFLICT).body(response);
            
        } catch (GoogleApiException e) {
            // ── GESTION DES ERREURS API GOOGLE ───────────────────────────────
            log.error("[SYNC-MANUAL] Erreur API Google pour l'utilisateur {} : {} (Code: {})", 
                     userId, e.getMessage(), e.getErrorCode());
            
            response.put("success", false);
            response.put("message", e.getMessage());
            response.put("errorCode", e.getErrorCode());
            response.put("retryable", e.isRetryable());
            
            // Message utilisateur adapté selon le type d'erreur
            String userMessage;
            switch (e.getErrorCode()) {
                case "SERVICE_UNAVAILABLE":
                    userMessage = "Le service Google Calendar est temporairement indisponible. Réessayez dans quelques minutes.";
                    break;
                case "NETWORK_ERROR":
                    userMessage = "Problème de connexion Internet. Vérifiez votre connexion et réessayez.";
                    break;
                case "UNAUTHORIZED":
                    userMessage = "Votre connexion Google a expiré. Veuillez vous reconnecter.";
                    break;
                case "INSUFFICIENT_PERMISSIONS":
                    userMessage = "Permissions insuffisantes. Veuillez autoriser l'accès à votre calendrier Google.";
                    break;
                default:
                    userMessage = "Une erreur est survenue lors de la connexion à Google Calendar.";
            }
            response.put("userMessage", userMessage);
            
            HttpStatus status = e.isRetryable() ? HttpStatus.SERVICE_UNAVAILABLE : HttpStatus.INTERNAL_SERVER_ERROR;
            return ResponseEntity.status(status).body(response);
            
        } catch (RuntimeException e) {
            log.error("[SYNC-MANUAL] Erreur lors de la synchronisation pour l'utilisateur {} : {}", 
                     userId, e.getMessage());
            response.put("success", false);
            response.put("message", "Erreur : " + e.getMessage());
            response.put("errorCode", "SYNC_FAILED");
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
            
        } catch (Exception e) {
            log.error("[SYNC-MANUAL] Erreur inattendue pour l'utilisateur {} : {}", 
                     userId, e.getMessage(), e);
            response.put("success", false);
            response.put("message", "Une erreur inattendue s'est produite");
            response.put("errorCode", "UNEXPECTED_ERROR");
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }

    /**
     * Endpoint de synchronisation simplifié (sans userId dans l'URL).
     * Utilise l'utilisateur authentifié du contexte de sécurité.
     * 
     * Note: Nécessite l'intégration de Spring Security pour récupérer
     * l'utilisateur courant via @AuthenticationPrincipal ou SecurityContextHolder.
     */
    @PostMapping("/sync")
    public ResponseEntity<Map<String, Object>> syncCurrentUser(
            @RequestParam(required = false) Long userId) {
        
        if (userId == null) {
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", "userId requis");
            return ResponseEntity.badRequest().body(response);
        }
        
        // Redirection vers l'endpoint principal
        return pullNow(userId);
    }

    /**
     * Vérifie le statut de connexion Google pour un utilisateur.
     * 
     * Endpoint: GET /api/calendar/status/{userId}
     */
    @GetMapping("/status/{userId}")
    public ResponseEntity<Map<String, Object>> getGoogleStatus(@PathVariable Long userId) {
        Map<String, Object> response = new HashMap<>();
        
        try {
            User user = userRepository.findById(userId)
                    .orElseThrow(() -> new RuntimeException("Utilisateur introuvable : " + userId));

            boolean isConnected = user.getGoogleAccessToken() != null 
                               && !user.getGoogleAccessToken().isBlank();

            response.put("connected", isConnected);
            response.put("userId", userId);
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            log.error("[STATUS] Erreur lors de la vérification du statut pour l'utilisateur {} : {}", 
                     userId, e.getMessage());
            response.put("connected", false);
            response.put("error", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }
}