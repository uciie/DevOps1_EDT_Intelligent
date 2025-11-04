package com.example.backend.service.parser;

import com.example.backend.model.Event;
import biweekly.Biweekly;
import biweekly.ICalendar;
import biweekly.component.VEvent;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.io.InputStream;
import java.time.ZoneId;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Implémentation du parser pour fichiers ICS (.ics) utilisant Biweekly.
 * Respecte le principe SOLID : responsabilité unique de parser un flux ICS en Event.
 */
@Component
public class BiweeklyCalendarParser implements ICalendarParser {

    @Override
    public List<Event> parse(InputStream inputStream) throws IOException{
        List<Event> events = new ArrayList<>();

        // Parse tous les calendriers contenus dans le fichier ICS
        List<ICalendar> calendars = Biweekly.parse(inputStream).all();

        for (ICalendar calendar : calendars) {
            for (VEvent vevent : calendar.getEvents()) {
                // Vérifie que l’événement contient les informations essentielles
                if (vevent.getSummary() == null || vevent.getDateStart() == null) {
                    continue;
                }

                // Récupère la date de début
                LocalDateTime start = LocalDateTime.ofInstant(
                        vevent.getDateStart().getValue().toInstant(),
                        ZoneId.systemDefault()
                );

                // Récupère la date de fin si existante, sinon ajoute 1h par défaut
                LocalDateTime end;
                if (vevent.getDateEnd() != null) {
                    end = LocalDateTime.ofInstant(
                            vevent.getDateEnd().getValue().toInstant(),
                            ZoneId.systemDefault()
                    );
                } else {
                    end = start.plusHours(1);
                }

                // Crée et ajoute l’événement à la liste
                Event event = new Event(vevent.getSummary().getValue(), start, end);
                events.add(event);
            }
        }

        return events;
    }
}