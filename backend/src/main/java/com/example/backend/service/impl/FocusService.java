package com.example.backend.service.impl;

import com.example.backend.dto.TimeSlot;
import com.example.backend.model.Event;
import com.example.backend.model.UserFocusPreference;
import com.example.backend.repository.EventRepository;
import com.example.backend.repository.UserFocusPreferenceRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

@Service
public class FocusService {

    @Autowired
    private EventRepository eventRepository;

    @Autowired
    private UserFocusPreferenceRepository preferenceRepository;

    private record ScheduleBlock(LocalDateTime start, LocalDateTime end) {}

    // =========================================================================
    // PARTIE PAUL : GESTION DES PRÉFÉRENCES ET VALIDATION DE CHARGE
    // =========================================================================

    public UserFocusPreference getPreferences(Long userId) {
        return preferenceRepository.findById(userId)
                .orElse(new UserFocusPreference(userId)); 
                // .orElse crée l'objet en mémoire SANS essayer de le sauvegarder
    }

    public UserFocusPreference updatePreferences(Long userId, UserFocusPreference newPrefs) {
        UserFocusPreference existing = getPreferences(userId);
        existing.setMaxEventsPerDay(newPrefs.getMaxEventsPerDay());
        existing.setMinFocusDuration(newPrefs.getMinFocusDuration());
        existing.setPreferredFocusTime(newPrefs.getPreferredFocusTime());
        existing.setFocusModeEnabled(newPrefs.isFocusModeEnabled());
        return preferenceRepository.save(existing);
    }

    public void validateDayNotOverloaded(Long userId, LocalDateTime dateTime) {
        if (dateTime == null) return;
        UserFocusPreference pref = getPreferences(userId);
        
        LocalDateTime start = dateTime.toLocalDate().atStartOfDay();
        LocalDateTime end = dateTime.toLocalDate().atTime(23, 59, 59);

        long currentCount = eventRepository.countEventsForDay(userId, start, end);

        if (currentCount >= pref.getMaxEventsPerDay()) {
            throw new ResponseStatusException(
                HttpStatus.BAD_REQUEST, 
                "Limite de " + pref.getMaxEventsPerDay() + " événements atteinte pour aujourd'hui."
            );
        }
    }

    // =========================================================================
    // PARTIE MANDA : ALGORITHMES DE RECHERCHE ET OPTIMISATION
    // =========================================================================

    public List<TimeSlot> findFreeGaps(Long userId, LocalDate date) {
        LocalDateTime dayStart = date.atTime(8, 0);
        LocalDateTime dayEnd = date.atTime(20, 0);
        
        List<Event> events = eventRepository.findByUser_IdAndStartTimeBetween(userId, date.atStartOfDay(), date.atTime(23, 59, 59));

        List<ScheduleBlock> busyBlocks = events.stream()
                .map(e -> new ScheduleBlock(e.getStartTime(), e.getEndTime()))
                .sorted(Comparator.comparing(ScheduleBlock::start))
                .toList();

        List<TimeSlot> gaps = new ArrayList<>();
        LocalDateTime currentPointer = dayStart;

        for (ScheduleBlock block : busyBlocks) {
            if (block.end().isBefore(dayStart)) continue;
            if (block.start().isAfter(currentPointer)) {
                gaps.add(new TimeSlot(currentPointer, block.start()));
            }
            if (block.end().isAfter(currentPointer)) {
                currentPointer = block.end();
            }
        }

        if (currentPointer.isBefore(dayEnd)) {
            gaps.add(new TimeSlot(currentPointer, dayEnd));
        }
        return gaps;
    }

    public List<TimeSlot> getOptimizedFocusSlots(Long userId, LocalDate date) {
        UserFocusPreference prefs = getPreferences(userId);
        List<TimeSlot> allGaps = findFreeGaps(userId, date);
        List<TimeSlot> optimized = new ArrayList<>();

        // Logique de conversion des Enums en heures
        int startPref = switch (prefs.getPreferredFocusTime()) {
            case MATIN -> 9;
            case APRES_MIDI -> 14;
            case SOIR -> 18;
        };
        int endPref = switch (prefs.getPreferredFocusTime()) {
            case MATIN -> 12;
            case APRES_MIDI -> 17;
            case SOIR -> 21;
        };

        for (TimeSlot gap : allGaps) {
            LocalDateTime actualStart = gap.start().isBefore(date.atTime(startPref, 0)) 
                    ? date.atTime(startPref, 0) : gap.start();
            LocalDateTime actualEnd = gap.end().isAfter(date.atTime(endPref, 0)) 
                    ? date.atTime(endPref, 0) : gap.end();

            if (actualStart.isBefore(actualEnd)) {
                TimeSlot optimizedSlot = new TimeSlot(actualStart, actualEnd);
                if (optimizedSlot.getDurationMinutes() >= prefs.getMinFocusDuration()) {
                    optimized.add(optimizedSlot);
                }
            }
        }
        return optimized;
    } 

    public boolean estBloqueParLeFocus(Long userId, LocalDateTime debut, LocalDateTime fin) {
        UserFocusPreference prefs = getPreferences(userId);
        if (!prefs.isFocusModeEnabled()) {
            return false;
        }

        List<TimeSlot> focusSlots = getOptimizedFocusSlots(userId, debut.toLocalDate());
        for (TimeSlot slot : focusSlots) {
            if (debut.isBefore(slot.end()) && fin.isAfter(slot.start())) {
                return true;
            }
        }
        return false;
    }
}