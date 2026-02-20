package com.example.backend.controller;

import com.example.backend.exception.GoogleApiException;
import com.example.backend.exception.SyncConflictException;
import com.example.backend.service.CalendarSyncService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.containsString;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Tests unitaires pour SyncController.
 * 
 * Couvre :
 * - POST /api/sync/user/{userId} (synchronisation manuelle)
 */
@WebMvcTest(SyncController.class)
@DisplayName("SyncController - Tests unitaires")
class SyncControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private CalendarSyncService syncService;

    // ═══════════════════════════════════════════════════════════════════════════
    // TESTS POUR : POST /api/sync/user/{userId}
    // ═══════════════════════════════════════════════════════════════════════════

    @Nested
    @DisplayName("POST /api/sync/user/{userId} - Synchronisation manuelle d'un utilisateur")
    class SyncUserTests {

        @Test
        @DisplayName("✅ Devrait synchroniser avec succès un utilisateur valide")
        void shouldSyncSuccessfully_WhenUserIsValid() throws Exception {
            // Given
            Long userId = 1L;
            doNothing().when(syncService).syncUser(userId);

            // When & Then
            mockMvc.perform(post("/api/sync/user/{userId}", userId)
                            .contentType(MediaType.APPLICATION_JSON))
                    .andExpect(status().isOk())
                    .andExpect(content().string("Synchronisation réussie !"));

            // Vérification de l'appel au service
            verify(syncService, times(1)).syncUser(userId);
        }

        @Test
        @DisplayName("✅ Devrait synchroniser plusieurs utilisateurs consécutivement")
        void shouldSyncMultipleUsersSuccessively() throws Exception {
            // Given
            Long userId1 = 1L;
            Long userId2 = 2L;
            doNothing().when(syncService).syncUser(anyLong());

            // When & Then - Premier utilisateur
            mockMvc.perform(post("/api/sync/user/{userId}", userId1)
                            .contentType(MediaType.APPLICATION_JSON))
                    .andExpect(status().isOk())
                    .andExpect(content().string("Synchronisation réussie !"));

            // When & Then - Second utilisateur
            mockMvc.perform(post("/api/sync/user/{userId}", userId2)
                            .contentType(MediaType.APPLICATION_JSON))
                    .andExpect(status().isOk())
                    .andExpect(content().string("Synchronisation réussie !"));

            // Vérification
            verify(syncService, times(2)).syncUser(anyLong());
        }

        @Test
        @DisplayName("❌ Devrait retourner 500 quand l'utilisateur n'existe pas")
        void shouldReturn500_WhenUserNotFound() throws Exception {
            // Given
            Long userId = 999L;
            doThrow(new RuntimeException("Utilisateur non trouvé"))
                    .when(syncService).syncUser(userId);

            // When & Then
            mockMvc.perform(post("/api/sync/user/{userId}", userId)
                            .contentType(MediaType.APPLICATION_JSON))
                    .andExpect(status().isInternalServerError())
                    .andExpect(content().string(containsString("Erreur : Utilisateur non trouvé")));

            verify(syncService, times(1)).syncUser(userId);
        }

        @Test
        @DisplayName("❌ Devrait retourner 500 quand l'utilisateur n'a pas de token Google")
        void shouldReturn500_WhenUserHasNoGoogleToken() throws Exception {
            // Given
            Long userId = 1L;
            doThrow(new RuntimeException("Le compte Google n'est pas lié."))
                    .when(syncService).syncUser(userId);

            // When & Then
            mockMvc.perform(post("/api/sync/user/{userId}", userId)
                            .contentType(MediaType.APPLICATION_JSON))
                    .andExpect(status().isInternalServerError())
                    .andExpect(content().string(containsString("Erreur : Le compte Google n'est pas lié.")));
        }

        @Test
        @DisplayName("❌ Devrait retourner 500 en cas de SyncConflictException")
        void shouldReturn500_WhenSyncConflictExceptionOccurs() throws Exception {
            // Given
            Long userId = 1L;
            doThrow(new SyncConflictException("Conflits détectés", null))
                    .when(syncService).syncUser(userId);

            // When & Then
            mockMvc.perform(post("/api/sync/user/{userId}", userId)
                            .contentType(MediaType.APPLICATION_JSON))
                    .andExpect(status().isInternalServerError())
                    .andExpect(content().string(containsString("Erreur : Conflits détectés")));
        }

        @Test
        @DisplayName("❌ Devrait retourner 500 en cas de GoogleApiException")
        void shouldReturn500_WhenGoogleApiExceptionOccurs() throws Exception {
            // Given
            Long userId = 1L;
            doThrow(new GoogleApiException("Service Google indisponible", "SERVICE_UNAVAILABLE", true))
                    .when(syncService).syncUser(userId);

            // When & Then
            mockMvc.perform(post("/api/sync/user/{userId}", userId)
                            .contentType(MediaType.APPLICATION_JSON))
                    .andExpect(status().isInternalServerError())
                    .andExpect(content().string(containsString("Erreur : Service Google indisponible")));
        }

        @Test
        @DisplayName("❌ Devrait retourner 500 en cas d'IOException")
        void shouldReturn500_WhenIOExceptionOccurs() throws Exception {
            // Given
            Long userId = 1L;
            doThrow(new RuntimeException("Erreur de communication avec Google Calendar"))
                    .when(syncService).syncUser(userId);

            // When & Then
            mockMvc.perform(post("/api/sync/user/{userId}", userId)
                            .contentType(MediaType.APPLICATION_JSON))
                    .andExpect(status().isInternalServerError())
                    .andExpect(content().string(containsString("Erreur : Erreur de communication avec Google Calendar")));
        }

        @Test
        @DisplayName("❌ Devrait retourner 500 en cas d'exception générique")
        void shouldReturn500_WhenGenericExceptionOccurs() throws Exception {
            // Given
            Long userId = 1L;
            doThrow(new RuntimeException("Erreur inattendue"))
                    .when(syncService).syncUser(userId);

            // When & Then
            mockMvc.perform(post("/api/sync/user/{userId}", userId)
                            .contentType(MediaType.APPLICATION_JSON))
                    .andExpect(status().isInternalServerError())
                    .andExpect(content().string(containsString("Erreur : Erreur inattendue")));
        }

        @Test
        @DisplayName("✅ Devrait gérer correctement un userId de type Long maximum")
        void shouldHandleMaxLongUserId() throws Exception {
            // Given
            Long userId = Long.MAX_VALUE;
            doNothing().when(syncService).syncUser(userId);

            // When & Then
            mockMvc.perform(post("/api/sync/user/{userId}", userId)
                            .contentType(MediaType.APPLICATION_JSON))
                    .andExpect(status().isOk())
                    .andExpect(content().string("Synchronisation réussie !"));
        }

        @Test
        @DisplayName("❌ Devrait retourner 400 en cas d'userId invalide (non numérique)")
        void shouldReturn400_WhenUserIdIsInvalid() throws Exception {
            // When & Then
            mockMvc.perform(post("/api/sync/user/{userId}", "invalid")
                            .contentType(MediaType.APPLICATION_JSON))
                    .andExpect(status().isBadRequest());

            // Le service ne doit jamais être appelé
            verify(syncService, never()).syncUser(anyLong());
        }

        @Test
        @DisplayName("❌ Devrait retourner 500 quand le service lève une exception checked")
        void shouldReturn500_WhenCheckedExceptionOccurs() throws Exception {
            // Given
            Long userId = 1L;
            doThrow(new Exception("Exception checked"))
                    .when(syncService).syncUser(userId);

            // When & Then
            mockMvc.perform(post("/api/sync/user/{userId}", userId)
                            .contentType(MediaType.APPLICATION_JSON))
                    .andExpect(status().isInternalServerError())
                    .andExpect(content().string(containsString("Erreur :")));
        }
    }
}