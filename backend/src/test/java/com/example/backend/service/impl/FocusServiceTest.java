package com.example.backend.service.impl;

import com.example.backend.dto.TimeSlot;
import com.example.backend.model.Event;
import com.example.backend.model.UserFocusPreference;
import com.example.backend.repository.EventRepository;
import com.example.backend.repository.UserFocusPreferenceRepository;
import com.example.backend.service.impl.FocusService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Rôle de la classe :
 * Cette classe teste la logique métier du FocusService. Elle vérifie que l'algorithme
 * identifie correctement les périodes vides dans l'emploi du temps (Gaps) et qu'il 
 * applique les filtres de personnalisation (durée minimale et période de la journée)
 * définis par l'utilisateur pour le mode "Deep Work".
 */
@ExtendWith(MockitoExtension.class)
public class FocusServiceTest {

    @Mock
    private EventRepository eventRepository;

    @Mock
    private UserFocusPreferenceRepository preferenceRepository;

    @InjectMocks
    private FocusService focusService;

    private final Long userId = 1L;
    private final LocalDate dateTest = LocalDate.of(2024, 10, 25);

    // Utilitaire pour créer des heures précises sur la date de test
    private LocalDateTime a(int heure, int minute) {
        return dateTest.atTime(heure, minute);
    }

    /**
     * Test : Vérifie que si aucun événement n'est présent, la journée entière est considérée comme libre.
     */
    @Test
    void devraitRetournerJourneeEntiereSiAucunEvenement() {
        // GIVEN
        when(eventRepository.findByUser_IdAndStartTimeBetween(any(), any(), any()))
                .thenReturn(List.of());

        // WHEN
        List<TimeSlot> trous = focusService.findFreeGaps(userId, dateTest);

        // THEN
        assertEquals(1, trous.size());
        assertEquals(a(8, 0), trous.get(0).start());
        assertEquals(a(20, 0), trous.get(0).end());
    }

    /**
     * Test : Vérifie que l'algorithme fusionne correctement deux événements qui se chevauchent 
     * pour ne pas créer de "faux" trous entre eux.
     */
    @Test
    void devraitGererLesEvenementsQuiSeChevauchent() {
        // GIVEN : Réunion de 10h à 11h et une autre qui commence avant la fin (10h30-11h30)
        Event e1 = new Event(); e1.setStartTime(a(10, 0)); e1.setEndTime(a(11, 0));
        Event e2 = new Event(); e2.setStartTime(a(10, 30)); e2.setEndTime(a(11, 30));
        
        when(eventRepository.findByUser_IdAndStartTimeBetween(any(), any(), any()))
                .thenReturn(List.of(e1, e2));

        // WHEN
        List<TimeSlot> trous = focusService.findFreeGaps(userId, dateTest);

        // THEN : Le premier trou s'arrête à 10h, le suivant reprend à 11h30
        assertEquals(2, trous.size());
        assertEquals(a(10, 0), trous.get(0).end());
        assertEquals(a(11, 30), trous.get(1).start());
    }

    /**
     * Test : Vérifie que les créneaux trop courts par rapport à la préférence 
     * de l'utilisateur sont bien ignorés.
     */
    @Test
    void devraitFiltrerLesCreneauxTropCourts() {
        // GIVEN : Un événement qui laisse un trou de 30min (8h-8h30) et un autre long
        Event e1 = new Event(); e1.setStartTime(a(8, 30)); e1.setEndTime(a(9, 0));
        
        UserFocusPreference prefs = new UserFocusPreference(userId);
        prefs.setMinFocusDuration(60); // L'utilisateur veut 1h minimum
        prefs.setPreferredFocusTime(UserFocusPreference.FocusTimePreference.MATIN);

        when(eventRepository.findByUser_IdAndStartTimeBetween(any(), any(), any()))
                .thenReturn(List.of(e1));
        when(preferenceRepository.findById(userId)).thenReturn(Optional.of(prefs));

        // WHEN
        List<TimeSlot> optimises = focusService.getOptimizedFocusSlots(userId, dateTest);

        // THEN : Le trou de 8h-8h30 (30 min) doit avoir été filtré
        for (TimeSlot slot : optimises) {
            assertTrue(slot.getDurationMinutes() >= 60);
        }
    }

    /**
     * Test : Vérifie que les créneaux proposés correspondent bien à la période 
     * de la journée choisie par l'utilisateur (Matin, Après-midi ou Soir).
     */
    @Test
    void devraitRespecterLaPeriodeDeJourneePreferee() {
        // GIVEN : Journée vide mais préférence pour l'après-midi (14h-17h)
        UserFocusPreference prefs = new UserFocusPreference(userId);
        prefs.setPreferredFocusTime(UserFocusPreference.FocusTimePreference.APRES_MIDI);
        prefs.setMinFocusDuration(30);

        when(eventRepository.findByUser_IdAndStartTimeBetween(any(), any(), any()))
                .thenReturn(List.of());
        when(preferenceRepository.findById(userId)).thenReturn(Optional.of(prefs));

        // WHEN
        List<TimeSlot> optimises = focusService.getOptimizedFocusSlots(userId, dateTest);

        // THEN
        assertFalse(optimises.isEmpty());
        assertEquals(14, optimises.get(0).start().getHour());
        assertEquals(17, optimises.get(0).end().getHour());
    }

    /**
     * Test : Vérifie que les événements qui se terminent avant le début de la journée (08h)
     * n'impactent pas le calcul des trous.
     */
    @Test
    void devraitIgnorerLesEvenementsSeTerminantAvantLeDebutDeJournee() {
        // GIVEN : Un événement de nuit qui finit à 07h30
        Event eNuit = new Event(); 
        eNuit.setStartTime(a(0, 0)); eNuit.setEndTime(a(7, 30));
        
        when(eventRepository.findByUser_IdAndStartTimeBetween(any(), any(), any()))
                .thenReturn(List.of(eNuit));

        // WHEN
        List<TimeSlot> trous = focusService.findFreeGaps(userId, dateTest);

        // THEN : Le premier trou doit quand même commencer à 08h00
        assertFalse(trous.isEmpty());
        assertEquals(a(8, 0), trous.get(0).start());
    }

    /**
     * Test : Vérifie que si un événement couvre toute la plage horaire (08h-20h),
     * la liste des trous retournée est vide.
     */
    @Test
    void devraitRetournerListeVideSiJourneeCompleteOccupee() {
        // GIVEN : Un événement de 08h00 à 20h00
        Event grosEvent = new Event(); 
        grosEvent.setStartTime(a(8, 0)); grosEvent.setEndTime(a(20, 0));
        
        when(eventRepository.findByUser_IdAndStartTimeBetween(any(), any(), any()))
                .thenReturn(List.of(grosEvent));

        // WHEN
        List<TimeSlot> trous = focusService.findFreeGaps(userId, dateTest);

        // THEN : Aucun trou possible
        assertTrue(trous.isEmpty(), "La liste des trous devrait être vide pour une journée pleine.");
    }

    /**
     * Test : Vérifie le respect de la période préférée pour le SOIR (18h-21h).
     */
    @Test
    void devraitProposerDesCreneauxUniquementLeSoir() {
        // GIVEN : Journée vide, préférence SOIR
        UserFocusPreference prefs = new UserFocusPreference(userId);
        prefs.setPreferredFocusTime(UserFocusPreference.FocusTimePreference.SOIR);
        prefs.setMinFocusDuration(30);

        when(eventRepository.findByUser_IdAndStartTimeBetween(any(), any(), any()))
                .thenReturn(List.of());
        when(preferenceRepository.findById(userId)).thenReturn(Optional.of(prefs));

        // WHEN
        List<TimeSlot> optimises = focusService.getOptimizedFocusSlots(userId, dateTest);

        // THEN : Le créneau doit commencer à 18h
        assertFalse(optimises.isEmpty());
        assertEquals(18, optimises.get(0).start().getHour());
        // Comme la journée s'arrête à 20h dans findFreeGaps, le trou sera 18h-20h
        assertEquals(20, optimises.get(0).end().getHour());
    }

    /**
     * Test : Vérifie que le service utilise des valeurs par défaut si aucune 
     * préférence n'existe en base de données pour cet utilisateur.
     */
    @Test
    void devraitUtiliserValeursParDefautSiPreferencesInexistantes() {
        // 1. On définit la date qui manquait
        LocalDate dateTest = LocalDate.now(); 

        // 2. On configure le mock
        when(preferenceRepository.findById(userId)).thenReturn(Optional.empty());

        // 3. Appel de la méthode (on utilise dateTest ici)
        List<TimeSlot> results = focusService.getOptimizedFocusSlots(userId, dateTest);

        // 4. Assertions
        assertNotNull(results);
    }
    @Test
    void devraitSignalerBloqueUniquementSiModeFocusEstActif() {
        // GIVEN : Un utilisateur avec le mode Focus DESACTIVE
        UserFocusPreference prefs = new UserFocusPreference(userId);
        prefs.setFocusModeEnabled(false); 
        when(preferenceRepository.findById(userId)).thenReturn(Optional.of(prefs));

        // WHEN : On vérifie si c'est bloqué sur un créneau qui est normalement un créneau de focus
        boolean estBloque = focusService.estBloqueParLeFocus(userId, a(10, 0), a(11, 0));

        // THEN : Ça ne doit PAS être bloqué car le mode est OFF
        assertFalse(estBloque);
    }

    // =========================================================================
    // TESTS DE PAUL : VALIDATION DE CHARGE ET PRÉFÉRENCES
    // =========================================================================

    /**
     * Test : Vérifie que la validation passe si le nombre d'événements est inférieur à la limite.
     */
    @Test
    void devraitValiderSiMoinsDeMaxEvents() {
        // GIVEN
        UserFocusPreference prefs = new UserFocusPreference(userId);
        prefs.setMaxEventsPerDay(5);
        when(preferenceRepository.findById(userId)).thenReturn(Optional.of(prefs));
        
        // On simule 3 événements déjà présents
        when(eventRepository.countEventsForDay(eq(userId), any(), any())).thenReturn(3L);

        // WHEN & THEN : Ne doit pas lever d'exception
        assertDoesNotThrow(() -> focusService.validateDayNotOverloaded(userId, a(10, 0)));
    }

    /**
     * Test : Vérifie qu'une exception est levée si la limite d'événements est atteinte.
     */
    @Test
    void devraitLeverExceptionSiTropDEvents() {
        // GIVEN
        UserFocusPreference prefs = new UserFocusPreference(userId);
        prefs.setMaxEventsPerDay(5);
        when(preferenceRepository.findById(userId)).thenReturn(Optional.of(prefs));
        
        // On simule 5 événements (limite atteinte)
        when(eventRepository.countEventsForDay(eq(userId), any(), any())).thenReturn(5L);

        // WHEN & THEN
        ResponseStatusException exception = assertThrows(ResponseStatusException.class, () -> {
            focusService.validateDayNotOverloaded(userId, a(10, 0));
        });

        assertEquals(HttpStatus.BAD_REQUEST, exception.getStatusCode());
        assertTrue(exception.getReason().contains("Limite de 5 événements atteinte"));
    }

    /**
     * Test : Vérifie la mise à jour des préférences.
     */
    @Test
    void devraitMettreAJourLesPreferences() {
        // GIVEN
        UserFocusPreference anciennesPrefs = new UserFocusPreference(userId);
        UserFocusPreference nouvellesPrefs = new UserFocusPreference(userId);
        nouvellesPrefs.setMaxEventsPerDay(10);
        nouvellesPrefs.setFocusModeEnabled(false);

        when(preferenceRepository.findById(userId)).thenReturn(Optional.of(anciennesPrefs));
        when(preferenceRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        // WHEN
        UserFocusPreference result = focusService.updatePreferences(userId, nouvellesPrefs);

        // THEN
        assertEquals(10, result.getMaxEventsPerDay());
        assertFalse(result.isFocusModeEnabled());
        verify(preferenceRepository).save(any());
    }
}