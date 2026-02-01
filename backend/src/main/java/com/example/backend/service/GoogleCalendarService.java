package com.example.backend.service;

import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.JsonFactory;
import com.google.api.client.json.gson.GsonFactory;
import com.google.api.client.util.DateTime;
import com.google.api.services.calendar.Calendar;
import com.google.api.services.calendar.model.Event;
import com.google.api.services.calendar.model.EventDateTime;
import com.google.auth.http.HttpCredentialsAdapter;
import com.google.auth.oauth2.AccessToken;
import com.google.auth.oauth2.GoogleCredentials;
import com.example.backend.model.User;
import com.example.backend.repository.UserRepository;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.security.GeneralSecurityException;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

@Service
public class GoogleCalendarService {

    private static final String APPLICATION_NAME = "EDT Intelligent";
    private static final JsonFactory JSON_FACTORY = GsonFactory.getDefaultInstance();

    private final UserRepository userRepository;

    public GoogleCalendarService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    /**
     * Crée un client Google Calendar pour un utilisateur donné
     */
    private Calendar getCalendarService(User user) throws GeneralSecurityException, IOException {
        if (user.getGoogleAccessToken() == null) {
            throw new IllegalStateException("Utilisateur non connecté à Google");
        }

        // Créer les credentials à partir du token stocké
        AccessToken accessToken = new AccessToken(user.getGoogleAccessToken(), null);
        GoogleCredentials credentials = GoogleCredentials.create(accessToken);

        final NetHttpTransport HTTP_TRANSPORT = GoogleNetHttpTransport.newTrustedTransport();
        return new Calendar.Builder(HTTP_TRANSPORT, JSON_FACTORY, new HttpCredentialsAdapter(credentials))
                .setApplicationName(APPLICATION_NAME)
                .build();
    }

    /**
     * Exporte un événement local vers Google Calendar
     */
    public String pushEventToGoogle(com.example.backend.model.Event localEvent) 
            throws GeneralSecurityException, IOException {
        
        User user = localEvent.getUser();
        Calendar service = getCalendarService(user);

        // Conversion de l'événement local vers le format Google
        Event googleEvent = new Event()
                .setSummary(localEvent.getSummary())
                .setDescription(localEvent.getSummary());

        // Conversion des dates
        EventDateTime start = new EventDateTime()
                .setDateTime(toGoogleDateTime(localEvent.getStartTime()))
                .setTimeZone("Europe/Paris");
        googleEvent.setStart(start);

        EventDateTime end = new EventDateTime()
                .setDateTime(toGoogleDateTime(localEvent.getEndTime()))
                .setTimeZone("Europe/Paris");
        googleEvent.setEnd(end);

        // Ajout de la localisation si présente
        if (localEvent.getLocation() != null && localEvent.getLocation().getAddress() != null) {
            googleEvent.setLocation(localEvent.getLocation().getAddress());
        }

        // Création dans Google Calendar
        Event createdEvent = service.events()
                .insert("primary", googleEvent)
                .execute();

        return createdEvent.getId();
    }

    /**
     * Importe les événements Google Calendar d'un utilisateur
     */
    public List<com.example.backend.model.Event> fetchGoogleEvents(User user, LocalDateTime since) 
            throws GeneralSecurityException, IOException {
        
        Calendar service = getCalendarService(user);
        List<com.example.backend.model.Event> events = new ArrayList<>();

        // Récupération des événements depuis une date donnée
        DateTime timeMin = toGoogleDateTime(since);
        
        com.google.api.services.calendar.model.Events eventsResult = service.events()
                .list("primary")
                .setTimeMin(timeMin)
                .setOrderBy("startTime")
                .setSingleEvents(true)
                .execute();

        List<Event> items = eventsResult.getItems();
        
        for (Event googleEvent : items) {
            com.example.backend.model.Event localEvent = convertToLocalEvent(googleEvent, user);
            events.add(localEvent);
        }

        return events;
    }

    /**
     * Met à jour un événement existant dans Google Calendar
     */
    public void updateGoogleEvent(com.example.backend.model.Event localEvent) 
            throws GeneralSecurityException, IOException {
        
        if (localEvent.getGoogleEventId() == null) {
            throw new IllegalArgumentException("Cet événement n'est pas synchronisé avec Google");
        }

        User user = localEvent.getUser();
        Calendar service = getCalendarService(user);

        // Récupérer l'événement existant
        Event googleEvent = service.events()
                .get("primary", localEvent.getGoogleEventId())
                .execute();

        // Mettre à jour les champs
        googleEvent.setSummary(localEvent.getSummary());
        
        EventDateTime start = new EventDateTime()
                .setDateTime(toGoogleDateTime(localEvent.getStartTime()))
                .setTimeZone("Europe/Paris");
        googleEvent.setStart(start);

        EventDateTime end = new EventDateTime()
                .setDateTime(toGoogleDateTime(localEvent.getEndTime()))
                .setTimeZone("Europe/Paris");
        googleEvent.setEnd(end);

        // Envoyer la mise à jour
        service.events()
                .update("primary", googleEvent.getId(), googleEvent)
                .execute();
    }

    /**
     * Supprime un événement de Google Calendar
     */
    public void deleteGoogleEvent(String googleEventId, User user) 
            throws GeneralSecurityException, IOException {
        
        Calendar service = getCalendarService(user);
        service.events().delete("primary", googleEventId).execute();
    }

    // --- Méthodes utilitaires de conversion ---

    private DateTime toGoogleDateTime(LocalDateTime localDateTime) {
        // Utilise le fuseau par défaut du serveur ou celui configuré
        return new DateTime(localDateTime.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli());
    }

    private LocalDateTime fromGoogleDateTime(EventDateTime googleDate) {
        if (googleDate.getDateTime() != null) {
            return LocalDateTime.ofInstant(
                Instant.ofEpochMilli(googleDate.getDateTime().getValue()), 
                ZoneId.systemDefault()
            );
        }
        // Cas des événements "Toute la journée"
        return LocalDateTime.parse(googleDate.getDate().toString() + "T00:00:00");
    }

    private com.example.backend.model.Event convertToLocalEvent(Event googleEvent, User user) {
        com.example.backend.model.Event localEvent = new com.example.backend.model.Event();
        
        localEvent.setSummary(googleEvent.getSummary() + "\n" + googleEvent.getDescription());
        localEvent.setUser(user);
        localEvent.setGoogleEventId(googleEvent.getId());
        localEvent.setSource(com.example.backend.model.Event.EventSource.GOOGLE);
        localEvent.setLastSyncedAt(LocalDateTime.now());

        if (googleEvent.getStart() != null) {
            localEvent.setStartTime(fromGoogleDateTime(googleEvent.getStart()));
        }
        if (googleEvent.getEnd() != null) {
            localEvent.setEndTime(fromGoogleDateTime(googleEvent.getEnd()));
        }

        return localEvent;
    }
}