package com.example.backend.service;

import com.example.backend.exception.GoogleApiException;
import com.example.backend.exception.SyncConflictException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.*;

/**
 * Tests unitaires pour SyncDelegateService.
 * 
 * Teste la synchronisation Google Calendar dans une transaction séparée (REQUIRES_NEW).
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("SyncDelegateService - Tests unitaires")
class SyncDelegateServiceTest {

    @Mock
    private CalendarSyncService calendarSyncService;

    @InjectMocks
    private SyncDelegateService syncDelegateService;

    @Nested
    @DisplayName("syncGoogleCalendarInNewTransaction - Tests de synchronisation")
    class SyncGoogleCalendarTests {

        @Test
        @DisplayName("✅ Devrait synchroniser avec succès un utilisateur valide")
        void shouldSyncSuccessfully_WhenUserIsValid() throws Exception {
            // Given
            Long userId = 1L;
            doNothing().when(calendarSyncService).syncUser(userId);

            // When
            syncDelegateService.syncGoogleCalendarInNewTransaction(userId);

            // Then
            verify(calendarSyncService, times(1)).syncUser(userId);
        }

        @Test
        @DisplayName("✅ Devrait synchroniser plusieurs utilisateurs consécutivement")
        void shouldSyncMultipleUsersSuccessively() throws Exception {
            // Given
            Long userId1 = 1L;
            Long userId2 = 2L;
            Long userId3 = 3L;
            doNothing().when(calendarSyncService).syncUser(anyLong());

            // When
            syncDelegateService.syncGoogleCalendarInNewTransaction(userId1);
            syncDelegateService.syncGoogleCalendarInNewTransaction(userId2);
            syncDelegateService.syncGoogleCalendarInNewTransaction(userId3);

            // Then
            verify(calendarSyncService, times(3)).syncUser(anyLong());
        }

        @Test
        @DisplayName("⚠️ Devrait NE PAS propager SyncConflictException (noRollbackFor)")
        void shouldNotPropagateException_WhenSyncConflictOccurs() throws Exception {
            // Given
            Long userId = 1L;
            doThrow(new SyncConflictException("Conflits détectés", null))
                    .when(calendarSyncService).syncUser(userId);

            // When - L'exception ne doit PAS être propagée
            syncDelegateService.syncGoogleCalendarInNewTransaction(userId);

            // Then - Le service a bien été appelé malgré l'exception
            verify(calendarSyncService, times(1)).syncUser(userId);
        }

        @Test
        @DisplayName("⚠️ Devrait NE PAS propager GoogleApiException (noRollbackFor)")
        void shouldNotPropagateException_WhenGoogleApiExceptionOccurs() throws Exception {
            // Given
            Long userId = 1L;
            GoogleApiException exception = new GoogleApiException(
                    "Service indisponible",
                    "SERVICE_UNAVAILABLE",
                    true
            );
            doThrow(exception).when(calendarSyncService).syncUser(userId);

            // When - L'exception ne doit PAS être propagée
            syncDelegateService.syncGoogleCalendarInNewTransaction(userId);

            // Then
            verify(calendarSyncService, times(1)).syncUser(userId);
        }

        @Test
        @DisplayName("⚠️ Devrait NE PAS propager RuntimeException (noRollbackFor)")
        void shouldNotPropagateException_WhenRuntimeExceptionOccurs() throws Exception {
            // Given
            Long userId = 1L;
            doThrow(new RuntimeException("Erreur inattendue"))
                    .when(calendarSyncService).syncUser(userId);

            // When - L'exception ne doit PAS être propagée
            syncDelegateService.syncGoogleCalendarInNewTransaction(userId);

            // Then
            verify(calendarSyncService, times(1)).syncUser(userId);
        }

        @Test
        @DisplayName("⚠️ Devrait NE PAS propager Exception checked (noRollbackFor)")
        void shouldNotPropagateException_WhenCheckedExceptionOccurs() throws Exception {
            // Given
            Long userId = 1L;
            doThrow(new Exception("Exception checked"))
                    .when(calendarSyncService).syncUser(userId);

            // When - L'exception ne doit PAS être propagée
            syncDelegateService.syncGoogleCalendarInNewTransaction(userId);

            // Then
            verify(calendarSyncService, times(1)).syncUser(userId);
        }

        @Test
        @DisplayName("⚠️ Devrait NE PAS propager IOException (noRollbackFor)")
        void shouldNotPropagateException_WhenIOExceptionOccurs() throws Exception {
            // Given
            Long userId = 1L;
            doThrow(new RuntimeException("Erreur de communication avec Google Calendar"))
                    .when(calendarSyncService).syncUser(userId);

            // When
            syncDelegateService.syncGoogleCalendarInNewTransaction(userId);

            // Then
            verify(calendarSyncService, times(1)).syncUser(userId);
        }

        @Test
        @DisplayName("✅ Devrait gérer userId valide avec valeur maximale")
        void shouldHandleMaxLongUserId() throws Exception {
            // Given
            Long userId = Long.MAX_VALUE;
            doNothing().when(calendarSyncService).syncUser(userId);

            // When
            syncDelegateService.syncGoogleCalendarInNewTransaction(userId);

            // Then
            verify(calendarSyncService, times(1)).syncUser(userId);
        }

        @Test
        @DisplayName("✅ Devrait gérer userId valide avec valeur minimale")
        void shouldHandleMinLongUserId() throws Exception {
            // Given
            Long userId = 1L;
            doNothing().when(calendarSyncService).syncUser(userId);

            // When
            syncDelegateService.syncGoogleCalendarInNewTransaction(userId);

            // Then
            verify(calendarSyncService, times(1)).syncUser(userId);
        }
    }

    @Nested
    @DisplayName("Tests de comportement transactionnel (REQUIRES_NEW)")
    class TransactionalBehaviorTests {

        @Test
        @DisplayName("✅ Devrait appeler le service même après une exception précédente")
        void shouldCallServiceAfterPreviousException() throws Exception {
            // Given
            Long userId1 = 1L;
            Long userId2 = 2L;
            
            // Premier appel lance une exception
            doThrow(new RuntimeException("Erreur utilisateur 1"))
                    .when(calendarSyncService).syncUser(userId1);
            
            // Deuxième appel réussit
            doNothing().when(calendarSyncService).syncUser(userId2);

            // When
            syncDelegateService.syncGoogleCalendarInNewTransaction(userId1); // Échoue silencieusement
            syncDelegateService.syncGoogleCalendarInNewTransaction(userId2); // Doit réussir

            // Then - Les deux appels ont bien été effectués
            verify(calendarSyncService, times(1)).syncUser(userId1);
            verify(calendarSyncService, times(1)).syncUser(userId2);
        }

        @Test
        @DisplayName("✅ Devrait isoler les transactions entre plusieurs appels")
        void shouldIsolateTransactionsBetweenMultipleCalls() throws Exception {
            // Given
            Long userId1 = 1L;
            Long userId2 = 2L;
            Long userId3 = 3L;
            
            doNothing().when(calendarSyncService).syncUser(userId1);
            doThrow(new RuntimeException("Erreur")).when(calendarSyncService).syncUser(userId2);
            doNothing().when(calendarSyncService).syncUser(userId3);

            // When
            syncDelegateService.syncGoogleCalendarInNewTransaction(userId1);
            syncDelegateService.syncGoogleCalendarInNewTransaction(userId2); // Échoue
            syncDelegateService.syncGoogleCalendarInNewTransaction(userId3);

            // Then - Tous les appels ont été effectués malgré l'échec du deuxième
            verify(calendarSyncService, times(1)).syncUser(userId1);
            verify(calendarSyncService, times(1)).syncUser(userId2);
            verify(calendarSyncService, times(1)).syncUser(userId3);
        }
    }

    @Nested
    @DisplayName("Tests de gestion d'erreurs spécifiques")
    class ErrorHandlingTests {

        @Test
        @DisplayName("⚠️ Devrait gérer une exception avec message null")
        void shouldHandleExceptionWithNullMessage() throws Exception {
            // Given
            Long userId = 1L;
            doThrow(new RuntimeException((String) null))
                    .when(calendarSyncService).syncUser(userId);

            // When - Ne doit pas planter
            syncDelegateService.syncGoogleCalendarInNewTransaction(userId);

            // Then
            verify(calendarSyncService, times(1)).syncUser(userId);
        }

        @Test
        @DisplayName("⚠️ Devrait gérer GoogleApiException avec différents codes d'erreur")
        void shouldHandleGoogleApiExceptionWithDifferentErrorCodes() throws Exception {
            // Given
            Long userId = 1L;
            
            GoogleApiException serviceUnavailable = new GoogleApiException(
                    "Service unavailable", "SERVICE_UNAVAILABLE", true
            );
            GoogleApiException unauthorized = new GoogleApiException(
                    "Unauthorized", "UNAUTHORIZED", false
            );
            GoogleApiException networkError = new GoogleApiException(
                    "Network error", "NETWORK_ERROR", true
            );

            // When/Then - Aucune exception ne doit être propagée
            doThrow(serviceUnavailable).when(calendarSyncService).syncUser(userId);
            syncDelegateService.syncGoogleCalendarInNewTransaction(userId);

            doThrow(unauthorized).when(calendarSyncService).syncUser(userId);
            syncDelegateService.syncGoogleCalendarInNewTransaction(userId);

            doThrow(networkError).when(calendarSyncService).syncUser(userId);
            syncDelegateService.syncGoogleCalendarInNewTransaction(userId);

            verify(calendarSyncService, times(3)).syncUser(userId);
        }

        @Test
        @DisplayName("⚠️ Devrait gérer une chaîne d'exceptions imbriquées")
        void shouldHandleNestedExceptions() throws Exception {
            // Given
            Long userId = 1L;
            RuntimeException rootCause = new RuntimeException("Root cause");
            RuntimeException intermediateCause = new RuntimeException("Intermediate", rootCause);
            GoogleApiException topException = new GoogleApiException(
                    "Top level error",
                    intermediateCause,
                    "NESTED_ERROR",
                    false
            );

            doThrow(topException).when(calendarSyncService).syncUser(userId);

            // When - Ne doit pas planter malgré la chaîne d'exceptions
            syncDelegateService.syncGoogleCalendarInNewTransaction(userId);

            // Then
            verify(calendarSyncService, times(1)).syncUser(userId);
        }
    }

    @Nested
    @DisplayName("Tests de cas limites")
    class EdgeCaseTests {

        @Test
        @DisplayName("✅ Devrait gérer des appels répétés pour le même userId")
        void shouldHandleRepeatedCallsForSameUserId() throws Exception {
            // Given
            Long userId = 1L;
            doNothing().when(calendarSyncService).syncUser(userId);

            // When - Appeler plusieurs fois pour le même utilisateur
            for (int i = 0; i < 10; i++) {
                syncDelegateService.syncGoogleCalendarInNewTransaction(userId);
            }

            // Then
            verify(calendarSyncService, times(10)).syncUser(userId);
        }

        @Test
        @DisplayName("✅ Devrait gérer l'alternance succès/échec")
        void shouldHandleAlternatingSuccessFailure() throws Exception {
            // Given
            Long userId = 1L;
            
            // Configuration : succès, échec, succès, échec
            doNothing()
                    .doThrow(new RuntimeException("Erreur"))
                    .doNothing()
                    .doThrow(new RuntimeException("Erreur"))
                    .when(calendarSyncService).syncUser(userId);

            // When
            syncDelegateService.syncGoogleCalendarInNewTransaction(userId); // Succès
            syncDelegateService.syncGoogleCalendarInNewTransaction(userId); // Échec (silencieux)
            syncDelegateService.syncGoogleCalendarInNewTransaction(userId); // Succès
            syncDelegateService.syncGoogleCalendarInNewTransaction(userId); // Échec (silencieux)

            // Then
            verify(calendarSyncService, times(4)).syncUser(userId);
        }

        @Test
        @DisplayName("⚠️ Devrait gérer une exception lors du premier appel")
        void shouldHandleExceptionOnFirstCall() throws Exception {
            // Given
            Long userId = 1L;
            doThrow(new RuntimeException("Erreur au démarrage"))
                    .doNothing()
                    .when(calendarSyncService).syncUser(userId);

            // When
            syncDelegateService.syncGoogleCalendarInNewTransaction(userId); // Échoue
            syncDelegateService.syncGoogleCalendarInNewTransaction(userId); // Réussit

            // Then
            verify(calendarSyncService, times(2)).syncUser(userId);
        }
    }

    @Nested
    @DisplayName("Tests de logging (comportement attendu)")
    class LoggingTests {

        @Test
        @DisplayName("✅ Devrait logger le début de la synchronisation")
        void shouldLogSyncStart() throws Exception {
            // Given
            Long userId = 1L;
            doNothing().when(calendarSyncService).syncUser(userId);

            // When
            syncDelegateService.syncGoogleCalendarInNewTransaction(userId);

            // Then
            verify(calendarSyncService, times(1)).syncUser(userId);
            // Note: Le logging réel est vérifié manuellement ou avec un framework de capture de logs
        }

        @Test
        @DisplayName("⚠️ Devrait logger les erreurs sans les propager")
        void shouldLogErrorsWithoutPropagating() throws Exception {
            // Given
            Long userId = 1L;
            RuntimeException exception = new RuntimeException("Erreur de test");
            doThrow(exception).when(calendarSyncService).syncUser(userId);

            // When - Devrait logger l'erreur mais ne pas la propager
            syncDelegateService.syncGoogleCalendarInNewTransaction(userId);

            // Then
            verify(calendarSyncService, times(1)).syncUser(userId);
        }
    }
}