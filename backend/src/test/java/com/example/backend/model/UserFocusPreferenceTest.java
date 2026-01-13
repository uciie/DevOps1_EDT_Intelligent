package com.example.backend.model;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class UserFocusPreferenceTest {

    @Test
    void testDefaultValues() {
        // When
        UserFocusPreference prefs = new UserFocusPreference();

        // Then - Vérification des valeurs par défaut demandées par le sujet
        assertEquals(5, prefs.getMaxEventsPerDay()); // Point (a)
        assertEquals(60, prefs.getMinFocusDuration()); // Point (b)
        assertTrue(prefs.isFocusModeEnabled());
        assertEquals(UserFocusPreference.FocusTimePreference.MATIN, prefs.getPreferredFocusTime());
    }

    @Test
    void testSettersAndGetters() {
        // Given
        UserFocusPreference prefs = new UserFocusPreference(10L);

        // When
        prefs.setMaxEventsPerDay(10);
        prefs.setMinFocusDuration(120);
        prefs.setFocusModeEnabled(false);
        prefs.setPreferredFocusTime(UserFocusPreference.FocusTimePreference.SOIR);

        // Then
        assertEquals(10L, prefs.getUserId());
        assertEquals(10, prefs.getMaxEventsPerDay());
        assertEquals(120, prefs.getMinFocusDuration());
        assertFalse(prefs.isFocusModeEnabled());
        assertEquals(UserFocusPreference.FocusTimePreference.SOIR, prefs.getPreferredFocusTime());
    }
}