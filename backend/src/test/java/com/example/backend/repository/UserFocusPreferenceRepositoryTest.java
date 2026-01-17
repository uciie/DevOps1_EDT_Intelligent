package com.example.backend.repository;

import com.example.backend.model.UserFocusPreference;
import com.example.backend.model.UserFocusPreference.FocusTimePreference;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import java.util.Optional;
import static org.junit.jupiter.api.Assertions.*;
@DataJpaTest
public class UserFocusPreferenceRepositoryTest {

    @Autowired
    private UserFocusPreferenceRepository preferenceRepository;

    @Test
    void testSaveAndRetrievePreferences() {
        // GIVEN
        UserFocusPreference prefs = new UserFocusPreference(1L);
        prefs.setMaxEventsPerDay(8);
        prefs.setMinFocusDuration(90);
        prefs.setPreferredFocusTime(UserFocusPreference.FocusTimePreference.APRES_MIDI);

        // WHEN
        preferenceRepository.save(prefs);
        Optional<UserFocusPreference> found = preferenceRepository.findById(1L);

        // THEN
        assertTrue(found.isPresent());
        assertEquals(8, found.get().getMaxEventsPerDay());
        assertEquals(90, found.get().getMinFocusDuration());
        assertEquals(UserFocusPreference.FocusTimePreference.APRES_MIDI, found.get().getPreferredFocusTime());
    }

    @Test
    void testUpdatePreferences() {
        // GIVEN
        UserFocusPreference prefs = new UserFocusPreference(1L);
        preferenceRepository.save(prefs);

        // WHEN
        UserFocusPreference existing = preferenceRepository.findById(1L).get();
        existing.setFocusModeEnabled(false);
        existing.setMaxEventsPerDay(3);
        preferenceRepository.save(existing);

        // THEN
        UserFocusPreference updated = preferenceRepository.findById(1L).get();
        assertFalse(updated.isFocusModeEnabled());
        assertEquals(3, updated.getMaxEventsPerDay());
    }
}
