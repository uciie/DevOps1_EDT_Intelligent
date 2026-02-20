package com.example.backend.service;

import com.example.backend.dto.SyncConflictDTO;
import com.example.backend.exception.GoogleApiException;
import com.example.backend.exception.SyncConflictException;
import com.example.backend.model.Event;
import com.example.backend.model.ActivityCategory;
import com.example.backend.model.User;
import com.example.backend.repository.EventRepository;
import com.example.backend.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.*;

/**
 * Tests unitaires pour CalendarSyncService.
 * 
 * Teste la synchronisation bidirectionnelle avec Google Calendar,
 * la détection de conflits et l'export d'événements.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("CalendarSyncService - Tests unitaires")
class CalendarSyncServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private EventRepository eventRepository;

    @Mock
    private GoogleCalendarService googleCalendarService;

    @Mock
    private CalendarImportService calendarImportService;

    @Spy
    @InjectMocks
    private CalendarSyncService calendarSyncService;

    private User validUser;
    private User userWithoutToken;

    @BeforeEach
    void setUp() {
        // Arrange - Utilisateur avec token Google valide
        validUser = new User();
        validUser.setId(1L);
        validUser.setGoogleAccessToken("valid-access-token");
        validUser.setGoogleRefreshToken("valid-refresh-token");

        // Utilisateur sans token
        userWithoutToken = new User();
        userWithoutToken.setId(2L);
        userWithoutToken.setGoogleAccessToken(null);
    }

	@Nested
	@DisplayName("Export vers Google - pushLocalEventsToGoogle & utilitaires")
	class ExportTests {

		@Test
		@DisplayName("✅ Ne fait rien quand aucun événement local à synchroniser")
		void shouldReturnZero_WhenNoLocalEventsToSync() {
			// Given
			Long userId = 1L;
			when(eventRepository.findByUser_Id(userId)).thenReturn(new ArrayList<>());

			// When
			int result = calendarSyncService.pushLocalEventsToGoogle(validUser);

			// Then
			assertThat(result).isZero();
			verify(googleCalendarService, never()).pushEventToGoogle(any());
		}

		@Test
		@DisplayName("✅ Supprime et propage la suppression quand status PENDING_DELETION avec googleEventId")
		void shouldDeleteEventOnGoogle_WhenPendingDeletionAndHasGoogleId() throws Exception {
			// Given
			Long userId = 1L;
			Event ev = createEvent(10L, "ToDelete", LocalDateTime.now(), LocalDateTime.now().plusHours(1), Event.EventSource.LOCAL);
			ev.setStatus(Event.EventStatus.PENDING_DELETION);
			ev.setGoogleEventId("g-123");

			when(eventRepository.findByUser_Id(userId)).thenReturn(Arrays.asList(ev));

			// When
			int result = calendarSyncService.pushLocalEventsToGoogle(validUser);

			// Then
			assertThat(result).isZero();
			verify(googleCalendarService, times(1)).deleteEventFromGoogle(ev);
			verify(eventRepository, times(1)).delete(ev);
		}

		@Test
		@DisplayName("✅ Pousse un événement local vers Google et incrémente le compteur")
		void shouldPushLocalEventToGoogle_AndCountSuccess() throws Exception {
			// Given
			Long userId = 1L;
			Event ev = createEvent(11L, "LocalCreate", LocalDateTime.now(), LocalDateTime.now().plusHours(1), Event.EventSource.LOCAL);
			ev.setGoogleEventId(null); // jamais poussé

			when(eventRepository.findByUser_Id(userId)).thenReturn(Arrays.asList(ev));
			doNothing().when(googleCalendarService).pushEventToGoogle(ev);

			// When
			int result = calendarSyncService.pushLocalEventsToGoogle(validUser);

			// Then
			assertThat(result).isEqualTo(1);
			verify(googleCalendarService, times(1)).pushEventToGoogle(ev);
		}

		@Test
		@DisplayName("⚠️ Devrait propager GoogleApiException lors du push")
		void shouldPropagateGoogleApiException_WhenPushFails() throws Exception {
			// Given
			Long userId = 1L;
			Event ev = createEvent(12L, "LocalCreate", LocalDateTime.now(), LocalDateTime.now().plusHours(1), Event.EventSource.LOCAL);
			ev.setGoogleEventId(null);

			when(eventRepository.findByUser_Id(userId)).thenReturn(Arrays.asList(ev));
				doThrow(new GoogleApiException("err", "CODE", true)).when(googleCalendarService).pushEventToGoogle(ev);

			// When/Then
			assertThatThrownBy(() -> calendarSyncService.pushLocalEventsToGoogle(validUser))
							.isInstanceOf(GoogleApiException.class);
		}

		@Test
		@DisplayName("⚠️ Devrait marquer l'événement en CONFLICT quand push lève une exception inattendue")
		void shouldMarkEventConflict_WhenPushThrowsUnexpectedException() throws Exception {
			// Given
			Long userId = 1L;
			Event ev = createEvent(13L, "LocalCreate", LocalDateTime.now(), LocalDateTime.now().plusHours(1), Event.EventSource.LOCAL);
			ev.setGoogleEventId(null);

			when(eventRepository.findByUser_Id(userId)).thenReturn(Arrays.asList(ev));
			doThrow(new RuntimeException("boom")).when(googleCalendarService).pushEventToGoogle(ev);

			// When
			int result = calendarSyncService.pushLocalEventsToGoogle(validUser);

			// Then
			assertThat(result).isZero();
			verify(eventRepository, times(1)).save(argThat(saved -> saved.getSyncStatus() == Event.SyncStatus.CONFLICT));
		}
	}

	@Nested
	@DisplayName("Utilitaires - conversion et règles de sync")
	class UtilityTests {

		@Test
		@DisplayName("✅ convertToGoogleEvent définit transparency 'opaque' pour catégorie FOCUS")
		void convertToGoogleEvent_SetsOpaqueForFocusCategory() {
			// Given
			Event ev = createEvent(20L, "FocusEvent", LocalDateTime.now(), LocalDateTime.now().plusHours(1), Event.EventSource.LOCAL);
			ev.setCategory(ActivityCategory.FOCUS);

			// When
			var gEvent = calendarSyncService.convertToGoogleEvent(ev);

			// Then
			assertThat(gEvent.getTransparency()).isEqualTo("opaque");
		}
	}

    // ═══════════════════════════════════════════════════════════════════════════
    // TESTS POUR : syncUser(Long userId)
    // ═══════════════════════════════════════════════════════════════════════════

    @Nested
    @DisplayName("syncUser - Synchronisation bidirectionnelle")
    class SyncUserTests {

        @Test
        @DisplayName("✅ Devrait synchroniser avec succès quand aucun conflit détecté")
        void shouldSyncSuccessfully_WhenNoConflictsDetected() throws Exception {
            // Given
            Long userId = 1L;
            when(userRepository.findById(userId)).thenReturn(Optional.of(validUser));
            when(eventRepository.findByUser_Id(userId)).thenReturn(new ArrayList<>());
            when(calendarImportService.pullEventsFromGoogle(validUser)).thenReturn(3);
            doReturn(2).when(calendarSyncService).pushLocalEventsToGoogle(validUser);

            // When
            calendarSyncService.syncUser(userId);

            // Then
            verify(userRepository, times(1)).findById(userId);
            verify(eventRepository, atLeastOnce()).findByUser_Id(userId);
            verify(calendarImportService, times(1)).pullEventsFromGoogle(validUser);
            // Note: pushLocalEventsToGoogle is called but we can't verify it directly as it's in the same class
        }

        @Test
        @DisplayName("❌ Devrait lever RuntimeException quand utilisateur non trouvé")
        void shouldThrowException_WhenUserNotFound() {
            // Given
            Long userId = 999L;
            when(userRepository.findById(userId)).thenReturn(Optional.empty());

            // When/Then
            assertThatThrownBy(() -> calendarSyncService.syncUser(userId))
                    .isInstanceOf(RuntimeException.class)
                    .hasMessageContaining("Utilisateur non trouvé");

            verify(userRepository, times(1)).findById(userId);
            verify(eventRepository, never()).findByUser_Id(anyLong());
        }

        @Test
        @DisplayName("❌ Devrait lever RuntimeException quand token Google est null")
        void shouldThrowException_WhenGoogleTokenIsNull() {
            // Given
            Long userId = 2L;
            when(userRepository.findById(userId)).thenReturn(Optional.of(userWithoutToken));

            // When/Then
            assertThatThrownBy(() -> calendarSyncService.syncUser(userId))
                    .isInstanceOf(RuntimeException.class)
                    .hasMessageContaining("Le compte Google n'est pas lié");

            verify(userRepository, times(1)).findById(userId);
        }

        @Test
        @DisplayName("❌ Devrait lever RuntimeException quand token Google est vide")
        void shouldThrowException_WhenGoogleTokenIsBlank() {
            // Given
            Long userId = 2L;
            userWithoutToken.setGoogleAccessToken("   ");
            when(userRepository.findById(userId)).thenReturn(Optional.of(userWithoutToken));

            // When/Then
            assertThatThrownBy(() -> calendarSyncService.syncUser(userId))
                    .isInstanceOf(RuntimeException.class)
                    .hasMessageContaining("Le compte Google n'est pas lié");
        }

        @Test
        @DisplayName("⚠️ Devrait lever SyncConflictException quand conflits détectés")
        void shouldThrowSyncConflictException_WhenConflictsDetected() {
            // Given
            Long userId = 1L;
            when(userRepository.findById(userId)).thenReturn(Optional.of(validUser));

            // Créer deux événements qui se chevauchent
            Event event1 = createEvent(1L, "Event 1", 
                    LocalDateTime.of(2026, 2, 14, 10, 0),
                    LocalDateTime.of(2026, 2, 14, 11, 0),
                    Event.EventSource.GOOGLE);
            
            Event event2 = createEvent(2L, "Event 2",
                    LocalDateTime.of(2026, 2, 14, 10, 30),
                    LocalDateTime.of(2026, 2, 14, 11, 30),
                    Event.EventSource.LOCAL);
            
            when(eventRepository.findByUser_Id(userId))
                    .thenReturn(Arrays.asList(event1, event2));

            // When/Then
            assertThatThrownBy(() -> calendarSyncService.syncUser(userId))
                    .isInstanceOf(SyncConflictException.class)
                    .hasMessageContaining("conflits de créneaux");

            verify(userRepository, times(1)).findById(userId);
            verify(eventRepository, atLeastOnce()).findByUser_Id(userId);
            // Import et export ne doivent pas être appelés en cas de conflit
            verify(calendarImportService, never()).pullEventsFromGoogle(any());
        }

        @Test
        @DisplayName("⚠️ Devrait propager GoogleApiException en cas d'erreur API Google")
        void shouldPropagateGoogleApiException_WhenApiError() throws Exception {
            // Given
            Long userId = 1L;
            when(userRepository.findById(userId)).thenReturn(Optional.of(validUser));
            when(eventRepository.findByUser_Id(userId)).thenReturn(new ArrayList<>());
            
            GoogleApiException apiException = new GoogleApiException(
                    "Service unavailable",
                    "SERVICE_UNAVAILABLE",
                    true
            );
            when(calendarImportService.pullEventsFromGoogle(validUser))
                    .thenThrow(apiException);

            // When/Then
            assertThatThrownBy(() -> calendarSyncService.syncUser(userId))
                    .isInstanceOf(GoogleApiException.class)
                    .hasMessageContaining("Service unavailable");
        }

        @Test
        @DisplayName("⚠️ Devrait wrapper exceptions inattendues dans RuntimeException")
        void shouldWrapUnexpectedExceptions() throws Exception {
            // Given
            Long userId = 1L;
            when(userRepository.findById(userId)).thenReturn(Optional.of(validUser));
            when(eventRepository.findByUser_Id(userId)).thenReturn(new ArrayList<>());
            when(calendarImportService.pullEventsFromGoogle(validUser))
                    .thenThrow(new NullPointerException("Unexpected error"));

            // When/Then
            assertThatThrownBy(() -> calendarSyncService.syncUser(userId))
                    .isInstanceOf(RuntimeException.class)
                    .hasMessageContaining("Erreur lors de la synchronisation");
        }
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // TESTS POUR : detectScheduleConflicts(User user)
    // ═══════════════════════════════════════════════════════════════════════════

    @Nested
    @DisplayName("Détection de conflits de créneaux")
    class ConflictDetectionTests {

        @Test
        @DisplayName("✅ Devrait ne détecter aucun conflit quand liste vide")
        void shouldDetectNoConflicts_WhenEventListIsEmpty() throws Exception {
            // Given
            Long userId = 1L;
            when(userRepository.findById(userId)).thenReturn(Optional.of(validUser));
            when(eventRepository.findByUser_Id(userId)).thenReturn(new ArrayList<>());
            when(calendarImportService.pullEventsFromGoogle(validUser)).thenReturn(0);

            // When
            assertDoesNotThrow(() -> calendarSyncService.syncUser(userId));

            // Then
            verify(eventRepository, atLeastOnce()).findByUser_Id(userId);
        }

        @Test
        @DisplayName("✅ Devrait ne détecter aucun conflit quand un seul événement")
        void shouldDetectNoConflicts_WhenOnlyOneEvent() throws Exception {
            // Given
            Long userId = 1L;
            Event event = createEvent(1L, "Event 1",
                    LocalDateTime.of(2026, 2, 14, 10, 0),
                    LocalDateTime.of(2026, 2, 14, 11, 0),
                    Event.EventSource.GOOGLE);

            when(userRepository.findById(userId)).thenReturn(Optional.of(validUser));
            when(eventRepository.findByUser_Id(userId)).thenReturn(Arrays.asList(event));
            when(calendarImportService.pullEventsFromGoogle(validUser)).thenReturn(0);

            // When
            assertDoesNotThrow(() -> calendarSyncService.syncUser(userId));

            // Then
            verify(eventRepository, atLeastOnce()).findByUser_Id(userId);
        }

        @Test
        @DisplayName("✅ Devrait ne détecter aucun conflit quand événements consécutifs")
        void shouldDetectNoConflicts_WhenEventsAreConsecutive() throws Exception {
            // Given
            Long userId = 1L;
            Event event1 = createEvent(1L, "Event 1",
                    LocalDateTime.of(2026, 2, 14, 10, 0),
                    LocalDateTime.of(2026, 2, 14, 11, 0),
                    Event.EventSource.GOOGLE);
            
            Event event2 = createEvent(2L, "Event 2",
                    LocalDateTime.of(2026, 2, 14, 11, 0),
                    LocalDateTime.of(2026, 2, 14, 12, 0),
                    Event.EventSource.LOCAL);

            when(userRepository.findById(userId)).thenReturn(Optional.of(validUser));
            when(eventRepository.findByUser_Id(userId)).thenReturn(Arrays.asList(event1, event2));
            when(calendarImportService.pullEventsFromGoogle(validUser)).thenReturn(0);

            // When
            assertDoesNotThrow(() -> calendarSyncService.syncUser(userId));

            // Then
            verify(eventRepository, atLeastOnce()).findByUser_Id(userId);
        }

        @Test
        @DisplayName("⚠️ Devrait détecter un conflit quand événements se chevauchent")
        void shouldDetectConflict_WhenEventsOverlap() {
            // Given
            Long userId = 1L;
            Event event1 = createEvent(1L, "Event 1",
                    LocalDateTime.of(2026, 2, 14, 10, 0),
                    LocalDateTime.of(2026, 2, 14, 11, 0),
                    Event.EventSource.GOOGLE);
            
            Event event2 = createEvent(2L, "Event 2",
                    LocalDateTime.of(2026, 2, 14, 10, 30),
                    LocalDateTime.of(2026, 2, 14, 11, 30),
                    Event.EventSource.LOCAL);

            when(userRepository.findById(userId)).thenReturn(Optional.of(validUser));
            when(eventRepository.findByUser_Id(userId)).thenReturn(Arrays.asList(event1, event2));

            // When/Then
            assertThatThrownBy(() -> calendarSyncService.syncUser(userId))
                    .isInstanceOf(SyncConflictException.class);
        }

        @Test
        @DisplayName("⚠️ Devrait détecter conflit quand événement commence avant fin de l'autre")
        void shouldDetectConflict_WhenEventStartsBeforeOtherEnds() {
            // Given
            Long userId = 1L;
            Event event1 = createEvent(1L, "Event 1",
                    LocalDateTime.of(2026, 2, 14, 10, 0),
                    LocalDateTime.of(2026, 2, 14, 12, 0),
                    Event.EventSource.GOOGLE);
            
            Event event2 = createEvent(2L, "Event 2",
                    LocalDateTime.of(2026, 2, 14, 11, 0),
                    LocalDateTime.of(2026, 2, 14, 13, 0),
                    Event.EventSource.LOCAL);

            when(userRepository.findById(userId)).thenReturn(Optional.of(validUser));
            when(eventRepository.findByUser_Id(userId)).thenReturn(Arrays.asList(event1, event2));

            // When/Then
            assertThatThrownBy(() -> calendarSyncService.syncUser(userId))
                    .isInstanceOf(SyncConflictException.class);
        }

        @Test
        @DisplayName("⚠️ Devrait détecter conflit quand événement englobe complètement un autre")
        void shouldDetectConflict_WhenOneEventEnglobesAnother() {
            // Given
            Long userId = 1L;
            Event event1 = createEvent(1L, "Event 1",
                    LocalDateTime.of(2026, 2, 14, 10, 0),
                    LocalDateTime.of(2026, 2, 14, 14, 0),
                    Event.EventSource.GOOGLE);
            
            Event event2 = createEvent(2L, "Event 2",
                    LocalDateTime.of(2026, 2, 14, 11, 0),
                    LocalDateTime.of(2026, 2, 14, 12, 0),
                    Event.EventSource.LOCAL);

            when(userRepository.findById(userId)).thenReturn(Optional.of(validUser));
            when(eventRepository.findByUser_Id(userId)).thenReturn(Arrays.asList(event1, event2));

            // When/Then
            assertThatThrownBy(() -> calendarSyncService.syncUser(userId))
                    .isInstanceOf(SyncConflictException.class);
        }

        @Test
        @DisplayName("✅ Devrait ignorer événements en attente de suppression")
        void shouldIgnoreEventsMarkedForDeletion() throws Exception {
            // Given
            Long userId = 1L;
            Event event1 = createEvent(1L, "Event 1",
                    LocalDateTime.of(2026, 2, 14, 10, 0),
                    LocalDateTime.of(2026, 2, 14, 11, 0),
                    Event.EventSource.GOOGLE);
            
            Event event2 = createEvent(2L, "Event 2",
                    LocalDateTime.of(2026, 2, 14, 10, 30),
                    LocalDateTime.of(2026, 2, 14, 11, 30),
                    Event.EventSource.LOCAL);
            event2.setStatus(Event.EventStatus.PENDING_DELETION);

            when(userRepository.findById(userId)).thenReturn(Optional.of(validUser));
            when(eventRepository.findByUser_Id(userId)).thenReturn(Arrays.asList(event1, event2));
            when(calendarImportService.pullEventsFromGoogle(validUser)).thenReturn(0);

            // When - Ne devrait pas détecter de conflit car event2 est en attente de suppression
            assertDoesNotThrow(() -> calendarSyncService.syncUser(userId));
        }

        @Test
        @DisplayName("⚠️ Devrait détecter plusieurs conflits dans une même liste")
        void shouldDetectMultipleConflicts() {
            // Given
            Long userId = 1L;
            Event event1 = createEvent(1L, "Event 1",
                    LocalDateTime.of(2026, 2, 14, 10, 0),
                    LocalDateTime.of(2026, 2, 14, 11, 0),
                    Event.EventSource.GOOGLE);
            
            Event event2 = createEvent(2L, "Event 2",
                    LocalDateTime.of(2026, 2, 14, 10, 30),
                    LocalDateTime.of(2026, 2, 14, 11, 30),
                    Event.EventSource.LOCAL);
            
            Event event3 = createEvent(3L, "Event 3",
                    LocalDateTime.of(2026, 2, 14, 14, 0),
                    LocalDateTime.of(2026, 2, 14, 15, 0),
                    Event.EventSource.GOOGLE);
            
            Event event4 = createEvent(4L, "Event 4",
                    LocalDateTime.of(2026, 2, 14, 14, 30),
                    LocalDateTime.of(2026, 2, 14, 15, 30),
                    Event.EventSource.LOCAL);

            when(userRepository.findById(userId)).thenReturn(Optional.of(validUser));
            when(eventRepository.findByUser_Id(userId))
                    .thenReturn(Arrays.asList(event1, event2, event3, event4));

            // When/Then
            assertThatThrownBy(() -> calendarSyncService.syncUser(userId))
                    .isInstanceOf(SyncConflictException.class)
                    .extracting(e -> ((SyncConflictException) e).getConflictDetails().getConflicts())
                    .satisfies(conflicts -> {
                        assertThat(conflicts).hasSizeGreaterThanOrEqualTo(2);
                    });
        }
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // MÉTHODES UTILITAIRES
    // ═══════════════════════════════════════════════════════════════════════════

    private Event createEvent(Long id, String title, LocalDateTime start, LocalDateTime end, Event.EventSource source) {
        Event event = new Event();
        event.setId(id);
        event.setSummary(title);
        event.setStartTime(start);
        event.setEndTime(end);
        event.setSource(source);
        event.setUser(validUser);
        event.setStatus(Event.EventStatus.CONFIRMED);
        event.setSyncStatus(Event.SyncStatus.SYNCED);
        return event;
    }
}