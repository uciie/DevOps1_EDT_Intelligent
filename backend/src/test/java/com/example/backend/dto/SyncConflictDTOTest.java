package com.example.backend.dto;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests unitaires pour SyncConflictDTO.
 * 
 * Teste la gestion des conflits de synchronisation entre événements.
 */
@DisplayName("SyncConflictDTO - Tests unitaires")
class SyncConflictDTOTest {

    @Nested
    @DisplayName("Constructeur par défaut et état initial")
    class ConstructorTests {

        @Test
        @DisplayName("✅ Devrait créer un DTO sans conflits par défaut")
        void shouldCreateDTOWithoutConflictsByDefault() {
            // When
            SyncConflictDTO dto = new SyncConflictDTO();

            // Then
            assertThat(dto.getConflicts()).isEmpty();
            assertThat(dto.isHasConflicts()).isFalse();
            assertThat(dto.getMessage()).isEqualTo("Aucun conflit détecté");
        }

        @Test
        @DisplayName("✅ Devrait initialiser la liste de conflits vide")
        void shouldInitializeEmptyConflictsList() {
            // When
            SyncConflictDTO dto = new SyncConflictDTO();

            // Then
            assertThat(dto.getConflicts()).isNotNull();
            assertThat(dto.getConflicts()).hasSize(0);
        }
    }

    @Nested
    @DisplayName("Ajout de conflits")
    class AddConflictTests {

        private SyncConflictDTO dto;
        private LocalDateTime now;

        @BeforeEach
        void setUp() {
            dto = new SyncConflictDTO();
            now = LocalDateTime.of(2026, 2, 14, 10, 0);
        }

        @Test
        @DisplayName("✅ Devrait ajouter un conflit et mettre à jour le statut")
        void shouldAddConflictAndUpdateStatus() {
            // Given
            SyncConflictDTO.ConflictingEvent conflict = createConflict(1L, "Événement 1", now);

            // When
            dto.addConflict(conflict);

            // Then
            assertThat(dto.getConflicts()).hasSize(1);
            assertThat(dto.isHasConflicts()).isTrue();
            assertThat(dto.getMessage()).isEqualTo("1 conflit(s) détecté(s)");
        }

        @Test
        @DisplayName("✅ Devrait ajouter plusieurs conflits")
        void shouldAddMultipleConflicts() {
            // Given
            SyncConflictDTO.ConflictingEvent conflict1 = createConflict(1L, "Événement 1", now);
            SyncConflictDTO.ConflictingEvent conflict2 = createConflict(2L, "Événement 2", now.plusHours(1));
            SyncConflictDTO.ConflictingEvent conflict3 = createConflict(3L, "Événement 3", now.plusHours(2));

            // When
            dto.addConflict(conflict1);
            dto.addConflict(conflict2);
            dto.addConflict(conflict3);

            // Then
            assertThat(dto.getConflicts()).hasSize(3);
            assertThat(dto.isHasConflicts()).isTrue();
            assertThat(dto.getMessage()).isEqualTo("3 conflit(s) détecté(s)");
        }

        @Test
        @DisplayName("✅ Devrait mettre à jour le message à chaque ajout")
        void shouldUpdateMessageOnEachAdd() {
            // Given
            SyncConflictDTO.ConflictingEvent conflict1 = createConflict(1L, "Event 1", now);
            SyncConflictDTO.ConflictingEvent conflict2 = createConflict(2L, "Event 2", now);

            // When & Then
            dto.addConflict(conflict1);
            assertThat(dto.getMessage()).isEqualTo("1 conflit(s) détecté(s)");

            dto.addConflict(conflict2);
            assertThat(dto.getMessage()).isEqualTo("2 conflit(s) détecté(s)");
        }

        @Test
        @DisplayName("✅ Devrait ajouter un conflit null sans planter")
        void shouldHandleNullConflict() {
            // When
            dto.addConflict(null);

            // Then
            assertThat(dto.getConflicts()).hasSize(1);
            assertThat(dto.getConflicts().get(0)).isNull();
            assertThat(dto.isHasConflicts()).isTrue();
        }
    }

    @Nested
    @DisplayName("ConflictingEvent - Classe interne")
    class ConflictingEventTests {

        private LocalDateTime now;

        @BeforeEach
        void setUp() {
            now = LocalDateTime.of(2026, 2, 14, 14, 30);
        }

        @Test
        @DisplayName("✅ Devrait créer un événement en conflit avec le constructeur par défaut")
        void shouldCreateConflictingEventWithDefaultConstructor() {
            // When
            SyncConflictDTO.ConflictingEvent event = new SyncConflictDTO.ConflictingEvent();

            // Then
            assertThat(event).isNotNull();
            assertThat(event.getEventId()).isNull();
            assertThat(event.getTitle()).isNull();
            assertThat(event.getStartTime()).isNull();
            assertThat(event.getEndTime()).isNull();
            assertThat(event.getSource()).isNull();
        }

        @Test
        @DisplayName("✅ Devrait créer un événement en conflit avec le constructeur paramétré")
        void shouldCreateConflictingEventWithParameterizedConstructor() {
            // Given
            Long eventId = 123L;
            String title = "Réunion importante";
            LocalDateTime start = now;
            LocalDateTime end = now.plusHours(1);
            String source = "GOOGLE";

            // When
            SyncConflictDTO.ConflictingEvent event = new SyncConflictDTO.ConflictingEvent(
                    eventId, title, start, end, source
            );

            // Then
            assertThat(event.getEventId()).isEqualTo(eventId);
            assertThat(event.getTitle()).isEqualTo(title);
            assertThat(event.getStartTime()).isEqualTo(start);
            assertThat(event.getEndTime()).isEqualTo(end);
            assertThat(event.getSource()).isEqualTo(source);
        }

        @Test
        @DisplayName("✅ Devrait définir les informations de conflit")
        void shouldSetConflictInformation() {
            // Given
            SyncConflictDTO.ConflictingEvent event = new SyncConflictDTO.ConflictingEvent();
            Long conflictingId = 456L;
            String conflictingTitle = "Autre événement";
            String conflictingSource = "LOCAL";

            // When
            event.setConflictingWithId(conflictingId);
            event.setConflictingWithTitle(conflictingTitle);
            event.setConflictingWithSource(conflictingSource);

            // Then
            assertThat(event.getConflictingWithId()).isEqualTo(conflictingId);
            assertThat(event.getConflictingWithTitle()).isEqualTo(conflictingTitle);
            assertThat(event.getConflictingWithSource()).isEqualTo(conflictingSource);
        }

        @Test
        @DisplayName("✅ Devrait créer un événement LOCAL en conflit avec un événement GOOGLE")
        void shouldCreateLocalEventConflictingWithGoogleEvent() {
            // Given
            SyncConflictDTO.ConflictingEvent localEvent = new SyncConflictDTO.ConflictingEvent(
                    10L,
                    "Événement local",
                    now,
                    now.plusMinutes(30),
                    "LOCAL"
            );

            // When
            localEvent.setConflictingWithId(20L);
            localEvent.setConflictingWithTitle("Événement Google");
            localEvent.setConflictingWithSource("GOOGLE");

            // Then
            assertThat(localEvent.getSource()).isEqualTo("LOCAL");
            assertThat(localEvent.getConflictingWithSource()).isEqualTo("GOOGLE");
        }

        @Test
        @DisplayName("✅ Devrait gérer des valeurs null dans le constructeur paramétré")
        void shouldHandleNullValuesInParameterizedConstructor() {
            // When
            SyncConflictDTO.ConflictingEvent event = new SyncConflictDTO.ConflictingEvent(
                    null, null, null, null, null
            );

            // Then
            assertThat(event.getEventId()).isNull();
            assertThat(event.getTitle()).isNull();
            assertThat(event.getStartTime()).isNull();
            assertThat(event.getEndTime()).isNull();
            assertThat(event.getSource()).isNull();
        }

        @Test
        @DisplayName("✅ Devrait utiliser les setters correctement")
        void shouldUseSettersCorrectly() {
            // Given
            SyncConflictDTO.ConflictingEvent event = new SyncConflictDTO.ConflictingEvent();

            // When
            event.setEventId(999L);
            event.setTitle("Nouveau titre");
            event.setStartTime(now);
            event.setEndTime(now.plusHours(2));
            event.setSource("GOOGLE");

            // Then
            assertThat(event.getEventId()).isEqualTo(999L);
            assertThat(event.getTitle()).isEqualTo("Nouveau titre");
            assertThat(event.getStartTime()).isEqualTo(now);
            assertThat(event.getEndTime()).isEqualTo(now.plusHours(2));
            assertThat(event.getSource()).isEqualTo("GOOGLE");
        }
    }

    @Nested
    @DisplayName("Setters et getters")
    class SettersGettersTests {

        private SyncConflictDTO dto;

        @BeforeEach
        void setUp() {
            dto = new SyncConflictDTO();
        }

        @Test
        @DisplayName("✅ Devrait définir et récupérer la liste de conflits")
        void shouldSetAndGetConflicts() {
            // Given
            List<SyncConflictDTO.ConflictingEvent> conflicts = new ArrayList<>();
            conflicts.add(createConflict(1L, "Event 1", LocalDateTime.now()));
            conflicts.add(createConflict(2L, "Event 2", LocalDateTime.now()));

            // When
            dto.setConflicts(conflicts);

            // Then
            assertThat(dto.getConflicts()).hasSize(2);
            assertThat(dto.getConflicts()).isEqualTo(conflicts);
        }

        @Test
        @DisplayName("✅ Devrait définir et récupérer le message")
        void shouldSetAndGetMessage() {
            // Given
            String message = "Message personnalisé";

            // When
            dto.setMessage(message);

            // Then
            assertThat(dto.getMessage()).isEqualTo(message);
        }

        @Test
        @DisplayName("✅ Devrait définir et récupérer hasConflicts")
        void shouldSetAndGetHasConflicts() {
            // When
            dto.setHasConflicts(true);

            // Then
            assertThat(dto.isHasConflicts()).isTrue();

            // When
            dto.setHasConflicts(false);

            // Then
            assertThat(dto.isHasConflicts()).isFalse();
        }

        @Test
        @DisplayName("✅ Devrait remplacer une liste existante")
        void shouldReplaceExistingList() {
            // Given
            dto.addConflict(createConflict(1L, "Event 1", LocalDateTime.now()));
            assertThat(dto.getConflicts()).hasSize(1);

            List<SyncConflictDTO.ConflictingEvent> newConflicts = new ArrayList<>();
            newConflicts.add(createConflict(2L, "Event 2", LocalDateTime.now()));
            newConflicts.add(createConflict(3L, "Event 3", LocalDateTime.now()));

            // When
            dto.setConflicts(newConflicts);

            // Then
            assertThat(dto.getConflicts()).hasSize(2);
            assertThat(dto.getConflicts().get(0).getEventId()).isEqualTo(2L);
        }
    }

    @Nested
    @DisplayName("Scénarios réalistes")
    class RealisticScenariosTests {

        private LocalDateTime baseTime;

        @BeforeEach
        void setUp() {
            baseTime = LocalDateTime.of(2026, 2, 14, 9, 0);
        }

        @Test
        @DisplayName("✅ Devrait gérer un conflit entre deux événements Google")
        void shouldHandleConflictBetweenTwoGoogleEvents() {
            // Given
            SyncConflictDTO dto = new SyncConflictDTO();
            SyncConflictDTO.ConflictingEvent event = new SyncConflictDTO.ConflictingEvent(
                    1L,
                    "Réunion d'équipe",
                    baseTime,
                    baseTime.plusHours(1),
                    "GOOGLE"
            );
            event.setConflictingWithId(2L);
            event.setConflictingWithTitle("Présentation client");
            event.setConflictingWithSource("GOOGLE");

            // When
            dto.addConflict(event);

            // Then
            assertThat(dto.getConflicts()).hasSize(1);
            assertThat(dto.isHasConflicts()).isTrue();
            assertThat(dto.getConflicts().get(0).getSource()).isEqualTo("GOOGLE");
            assertThat(dto.getConflicts().get(0).getConflictingWithSource()).isEqualTo("GOOGLE");
        }

        @Test
        @DisplayName("✅ Devrait gérer un conflit entre événement local et Google")
        void shouldHandleConflictBetweenLocalAndGoogleEvent() {
            // Given
            SyncConflictDTO dto = new SyncConflictDTO();
            SyncConflictDTO.ConflictingEvent event = new SyncConflictDTO.ConflictingEvent(
                    10L,
                    "Cours de sport",
                    baseTime.withHour(14),
                    baseTime.withHour(15),
                    "LOCAL"
            );
            event.setConflictingWithId(5L);
            event.setConflictingWithTitle("Rendez-vous médecin");
            event.setConflictingWithSource("GOOGLE");

            // When
            dto.addConflict(event);

            // Then
            assertThat(dto.getConflicts().get(0).getSource()).isEqualTo("LOCAL");
            assertThat(dto.getConflicts().get(0).getConflictingWithSource()).isEqualTo("GOOGLE");
        }

        @Test
        @DisplayName("✅ Devrait gérer plusieurs conflits dans la même journée")
        void shouldHandleMultipleConflictsInSameDay() {
            // Given
            SyncConflictDTO dto = new SyncConflictDTO();

            SyncConflictDTO.ConflictingEvent conflict1 = new SyncConflictDTO.ConflictingEvent(
                    1L, "Event matin", baseTime.withHour(9), baseTime.withHour(10), "GOOGLE"
            );
            conflict1.setConflictingWithId(2L);
            conflict1.setConflictingWithTitle("Autre event matin");
            conflict1.setConflictingWithSource("LOCAL");

            SyncConflictDTO.ConflictingEvent conflict2 = new SyncConflictDTO.ConflictingEvent(
                    3L, "Event après-midi", baseTime.withHour(14), baseTime.withHour(15), "LOCAL"
            );
            conflict2.setConflictingWithId(4L);
            conflict2.setConflictingWithTitle("Autre event après-midi");
            conflict2.setConflictingWithSource("GOOGLE");

            // When
            dto.addConflict(conflict1);
            dto.addConflict(conflict2);

            // Then
            assertThat(dto.getConflicts()).hasSize(2);
            assertThat(dto.getMessage()).isEqualTo("2 conflit(s) détecté(s)");
        }

        @Test
        @DisplayName("✅ Devrait permettre la réinitialisation après traitement des conflits")
        void shouldAllowResetAfterConflictResolution() {
            // Given
            SyncConflictDTO dto = new SyncConflictDTO();
            dto.addConflict(createConflict(1L, "Event", baseTime));
            assertThat(dto.isHasConflicts()).isTrue();

            // When - Réinitialisation
            dto.setConflicts(new ArrayList<>());
            dto.setHasConflicts(false);
            dto.setMessage("Aucun conflit détecté");

            // Then
            assertThat(dto.getConflicts()).isEmpty();
            assertThat(dto.isHasConflicts()).isFalse();
            assertThat(dto.getMessage()).isEqualTo("Aucun conflit détecté");
        }
    }

    @Nested
    @DisplayName("Tests de cas limites")
    class EdgeCaseTests {

        @Test
        @DisplayName("✅ Devrait gérer une liste vide explicitement définie")
        void shouldHandleExplicitlySetEmptyList() {
            // Given
            SyncConflictDTO dto = new SyncConflictDTO();
            dto.setConflicts(new ArrayList<>());

            // When/Then
            assertThat(dto.getConflicts()).isEmpty();
        }

        @Test
        @DisplayName("✅ Devrait gérer un titre d'événement très long")
        void shouldHandleVeryLongEventTitle() {
            // Given
            String longTitle = "A".repeat(10000);
            SyncConflictDTO.ConflictingEvent event = new SyncConflictDTO.ConflictingEvent(
                    1L,
                    longTitle,
                    LocalDateTime.now(),
                    LocalDateTime.now().plusHours(1),
                    "GOOGLE"
            );

            // When/Then
            assertThat(event.getTitle()).hasSize(10000);
        }

        @Test
        @DisplayName("✅ Devrait gérer des caractères spéciaux dans les titres")
        void shouldHandleSpecialCharactersInTitles() {
            // Given
            String specialTitle = "Événement é à ç ñ 中文 🎉";
            SyncConflictDTO.ConflictingEvent event = new SyncConflictDTO.ConflictingEvent();
            event.setTitle(specialTitle);
            event.setConflictingWithTitle("Autre événement 特殊");

            // When/Then
            assertThat(event.getTitle()).contains("é", "à", "🎉", "中文");
            assertThat(event.getConflictingWithTitle()).contains("特殊");
        }

        @Test
        @DisplayName("✅ Devrait gérer un grand nombre de conflits")
        void shouldHandleLargeNumberOfConflicts() {
            // Given
            SyncConflictDTO dto = new SyncConflictDTO();
            LocalDateTime time = LocalDateTime.now();

            // When
            for (int i = 0; i < 1000; i++) {
                dto.addConflict(createConflict((long) i, "Event " + i, time.plusMinutes(i)));
            }

            // Then
            assertThat(dto.getConflicts()).hasSize(1000);
            assertThat(dto.getMessage()).isEqualTo("1000 conflit(s) détecté(s)");
        }
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // MÉTHODES UTILITAIRES
    // ═══════════════════════════════════════════════════════════════════════════

    private SyncConflictDTO.ConflictingEvent createConflict(Long id, String title, LocalDateTime start) {
        return new SyncConflictDTO.ConflictingEvent(
                id,
                title,
                start,
                start.plusHours(1),
                "GOOGLE"
        );
    }
}