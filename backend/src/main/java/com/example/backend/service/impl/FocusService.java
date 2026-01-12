package com.example.backend.service.impl;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.example.backend.dto.TimeSlot;
import com.example.backend.model.Event;
import com.example.backend.model.UserFocusPreference;
import com.example.backend.repository.EventRepository;
import com.example.backend.repository.UserFocusPreferenceRepository;

@Service
public class FocusService {

    @Autowired
    private EventRepository eventRepository;

    @Autowired
    private UserFocusPreferenceRepository preferenceRepository;

    private record ScheduleBlock(LocalDateTime start, LocalDateTime end) {}

    public List<TimeSlot> findFreeGaps(Long userId, LocalDate date) {
        LocalDateTime dayStart = date.atTime(8, 0);
        LocalDateTime dayEnd = date.atTime(20, 0);
        
        LocalDateTime startOfDay = date.atStartOfDay();
        LocalDateTime endOfDay = date.atTime(23, 59, 59);

        List<Event> events = eventRepository.findByUser_IdAndStartTimeBetween(userId, startOfDay, endOfDay);

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
        UserFocusPreference prefs = preferenceRepository.findById(userId)
                .orElse(new UserFocusPreference(userId));

        List<TimeSlot> allGaps = findFreeGaps(userId, date);
        List<TimeSlot> optimized = new ArrayList<>();

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
        UserFocusPreference prefs = preferenceRepository.findById(userId)
                .orElse(new UserFocusPreference(userId));

        // Utilise maintenant le getter isFocusModeEnabled() ajouté ci-dessus
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