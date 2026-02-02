package com.example.backend.service;

import com.example.backend.model.User;
import com.example.backend.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;


@Service
public class CalendarSyncService {

    private final UserRepository userRepository;
    private final GoogleCalendarService googleCalendarService;
    private final CalendarImportService calendarImportService;

    public CalendarSyncService(UserRepository userRepository, 
                               GoogleCalendarService googleCalendarService, 
                               CalendarImportService calendarImportService) {
        this.userRepository = userRepository;
        this.googleCalendarService = googleCalendarService;
        this.calendarImportService = calendarImportService;
    }

    @Transactional
    public void syncUser(Long userId) throws Exception {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("Utilisateur non trouvé"));

        if (user.getGoogleAccessToken() == null || user.getGoogleAccessToken().isBlank()) {
            throw new RuntimeException("Le compte Google n'est pas lié.");
        }

        // 1. Import des événements Google vers la base locale
        calendarImportService.pullEventsFromGoogle(user);

        // 2. Export des nouveaux événements locaux vers Google
        user.getEvents().stream()
            .filter(e -> e.getGoogleEventId() == null)
            .forEach(googleCalendarService::pushEventToGoogle);
    }
}