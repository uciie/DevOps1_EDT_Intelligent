package com.example.backend.service;

import com.example.backend.model.UserFocusPreference;
import com.example.backend.repository.EventRepository;
import com.example.backend.repository.UserFocusPreferenceRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;
import java.time.LocalDateTime;

@Service
public class FocusService {

    @Autowired
    private UserFocusPreferenceRepository prefRepository;

    @Autowired
    private EventRepository eventRepository;

    /**
     * Récupère les préférences d'un utilisateur ou crée des valeurs par défaut
     */
    public UserFocusPreference getPreferences(Long userId) {
        return prefRepository.findById(userId)
                .orElseGet(() -> prefRepository.save(new UserFocusPreference(userId)));
    }

    /**
     * Met à jour les préférences (Point b: personnalisable)
     */
    public UserFocusPreference updatePreferences(Long userId, UserFocusPreference newPrefs) {
        UserFocusPreference existing = getPreferences(userId);
        existing.setMaxEventsPerDay(newPrefs.getMaxEventsPerDay());
        existing.setMinFocusDuration(newPrefs.getMinFocusDuration());
        existing.setPreferredFocusTime(newPrefs.getPreferredFocusTime());
        return prefRepository.save(existing);
    }

    /**
     * Vérifie si on peut encore ajouter un événement aujourd'hui (Point a)
     */
    public void validateDayNotOverloaded(Long userId, LocalDateTime dateTime) {
        if (dateTime == null) return; // Si pas de date, ce n'est pas encore un événement

        UserFocusPreference pref = getPreferences(userId);
        
        // Définir les bornes de la journée (00h00 à 23h59)
        LocalDateTime start = dateTime.toLocalDate().atStartOfDay();
        LocalDateTime end = dateTime.toLocalDate().atTime(23, 59, 59);

        long currentCount = eventRepository.countEventsForDay(userId, start, end);

        if (currentCount >= pref.getMaxEventsPerDay()) {
            throw new ResponseStatusException(
                HttpStatus.BAD_REQUEST, 
                "Action impossible : La limite de " + pref.getMaxEventsPerDay() + " événements pour cette journée est atteinte."
            );
        }
    }
}