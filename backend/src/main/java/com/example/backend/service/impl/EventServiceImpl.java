package com.example.backend.service.impl;

import com.example.backend.controller.EventController.EventRequest;
import com.example.backend.model.Event;
import com.example.backend.model.Location;
import com.example.backend.model.TravelTime;
import com.example.backend.model.User;
import com.example.backend.repository.EventRepository;
import com.example.backend.repository.TravelTimeRepository;
import com.example.backend.repository.UserRepository;
import com.example.backend.service.EventService;
import com.example.backend.service.TravelTimeCalculator;
import com.example.backend.service.TravelTimeService;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;

/**
 * Implémentation du service pour la gestion des événements.
 */
@Service
public class EventServiceImpl implements EventService {

    private final EventRepository eventRepository;
    private final UserRepository userRepository;
    private final TravelTimeService travelTimeService;
    private final TravelTimeRepository travelTimeRepository; // Ajout pour accès direct si besoin
    
    // On garde le calculateur principal (qui sera Google si activé, sinon Simple par défaut)
    private final TravelTimeCalculator primaryCalculator;
    // On injecte explicitement le calculateur simple pour le fallback forcé
    private final TravelTimeCalculator simpleCalculator;

    // Injection des dépendances nécessaires
    public EventServiceImpl(EventRepository eventRepository, 
                            UserRepository userRepository,
                            TravelTimeService travelTimeService,
                            TravelTimeRepository travelTimeRepository,
                            TravelTimeCalculator primaryCalculator,
                            @Qualifier("simpleTravelTimeCalculator") TravelTimeCalculator simpleCalculator) {
        this.eventRepository = eventRepository;
        this.userRepository = userRepository;
        this.travelTimeService = travelTimeService;
        this.travelTimeRepository = travelTimeRepository;
        this.primaryCalculator = primaryCalculator;
        this.simpleCalculator = simpleCalculator;
    }

    // --- Helper pour choisir le calculateur ---
    private TravelTimeCalculator getCalculator(Boolean useGoogleMaps) {
        // Si l'utilisateur demande explicitement NON (false), on utilise le simple.
        // Si null ou true, on utilise le primary (qui est Google si le profil est actif, sinon Simple)
        if (Boolean.FALSE.equals(useGoogleMaps)) {
            return simpleCalculator;
        }
        return primaryCalculator;
    }

    // --- Implémentation des méthodes ---

    /**
     * Récupère tous les événements d'un utilisateur, triés par heure de début (logique de l'optimiseur).
     */
    @Override
    public List<Event> getEventsByUserId(Long userId) {
        return eventRepository.findByUser_IdOrderByStartTime(userId);
    }

    @Override
    public Event getEventById(Long id) {
        return eventRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Event not found with id: " + id));
    }

    /**
     * Crée un nouvel événement avec vérification de faisabilité de trajet.
     */
    @Override
    @Transactional
    public Event createEvent(EventRequest eventRequest) {
        User user = userRepository.findById(eventRequest.getUserId())
                .orElseThrow(() -> new IllegalArgumentException("User not found"));

        Event event = new Event(
                eventRequest.getSummary(),
                eventRequest.getStartTime(),
                eventRequest.getEndTime(),
                user
        );

        // Ajouter la localisation si fournie
        if (eventRequest.getLocation() != null) {
            Location location = eventRequest.getLocation().toLocation();
            if (location != null) {
                event.setLocation(location);
            }
        }

        // --- VERIFICATION DE FAISABILITE ---
        if (eventRequest.getTransportMode() != null && event.getLocation() != null) {
            
            // 1. Récupérer les événements de l'utilisateur pour trouver le précédent
            List<Event> userEvents = eventRepository.findByUser_IdOrderByStartTime(user.getId());
            
            // 2. Trouver l'événement qui termine avant ou exactement au début du nouvel événement
            Event previousEvent = userEvents.stream()
                .filter(e -> e.getEndTime().isBefore(event.getStartTime()) || 
                             e.getEndTime().isEqual(event.getStartTime()))
                .max(Comparator.comparing(Event::getEndTime))
                .orElse(null);

            // 3. Si un événement précédent existe et a une localisation
            if (previousEvent != null && previousEvent.getLocation() != null) {
                try {
                    // Conversion du String en Enum
                    TravelTime.TransportMode mode = TravelTime.TransportMode.valueOf(eventRequest.getTransportMode());
                    TravelTimeCalculator calculator = getCalculator(eventRequest.getUseGoogleMaps());

                    int durationMinutes = calculator.calculateTravelTime(
                        previousEvent.getLocation(),
                        event.getLocation(),
                        mode
                    );
                    
                    // B. Vérifier si on arrive à temps
                    // Heure de départ = fin de l'événement A
                    LocalDateTime departureTime = previousEvent.getEndTime();
                    // Heure d'arrivée estimée = départ + trajet
                    LocalDateTime estimatedArrival = departureTime.plusMinutes(durationMinutes);
                    
                    // Si l'arrivée estimée est STRICTEMENT APRES le début de l'événement B
                    if (estimatedArrival.isAfter(event.getStartTime())) {
                        throw new IllegalArgumentException(
                            String.format("Impossible d'arriver à l'heure ! Fin de l'événement précédent : %s. " +
                                          "Temps de trajet (%s) : %d min. Arrivée estimée : %s. " +
                                          "Début de l'événement prévu : %s.",
                                          departureTime.toLocalTime(),
                                          mode,
                                          durationMinutes,
                                          estimatedArrival.toLocalTime(),
                                          event.getStartTime().toLocalTime())
                        );
                    }
                    
                    // C. Si c'est faisable, on sauvegarde l'événement
                    Event savedEvent = eventRepository.save(event);
                    
                    // On force la création avec la durée calculée pour être cohérent
                    // (Ici on utilise une méthode interne ou on met à jour manuellement si createTravelTime recalcule)
                    // Pour simplifier, on suppose que createTravelTime fait le job, mais pour le RECALCUL global, on sera plus explicite.
                    travelTimeService.createTravelTimeWithDuration(previousEvent, savedEvent, mode, durationMinutes);
                    
                    return savedEvent;

                } catch (java.lang.IllegalArgumentException e) {
                    // Relance l'exception pour qu'elle soit gérée par le contrôleur (retour 400)
                    throw e; 
                }
            }
        }

        // Si pas de conflit ou pas de vérification nécessaire, sauvegarde simple
        return eventRepository.save(event);
    }

    /**
     * Met à jour un événement existant.
     */
    @Override
    @Transactional
    public Event updateEvent(Long id, EventRequest eventRequest) {
        Event event = getEventById(id);

        if (eventRequest.getSummary() != null) {
            event.setSummary(eventRequest.getSummary());
        }
        if (eventRequest.getStartTime() != null) {
            event.setStartTime(eventRequest.getStartTime());
        }
        if (eventRequest.getEndTime() != null) {
            event.setEndTime(eventRequest.getEndTime());
        }
        
        // --- CORRECTION MAJEURE ICI ---
        // Si un objet LocationRequest est fourni (même vide), on traite la mise à jour.
        if (eventRequest.getLocation() != null) {
            Location location = eventRequest.getLocation().toLocation();
            // On assigne la location DIRECTEMENT. Si 'location' est null (car l'adresse était vide),
            // cela supprimera correctement la location de l'événement.
            event.setLocation(location);
        }

        // --- VERIFICATION DE FAISABILITE AVANT SAUVEGARDE (AJOUT POUR UPDATE) ---
        // Si l'utilisateur a spécifié un mode de transport et que l'événement a une localisation
        if (eventRequest.getTransportMode() != null && event.getLocation() != null) {
            
            // 1. Récupérer les événements de l'utilisateur pour trouver le précédent
            List<Event> userEvents = eventRepository.findByUser_IdOrderByStartTime(event.getUser().getId());
            
            // 2. Trouver l'événement qui termine avant ou exactement au début de l'événement modifié
            // IMPORTANT : On exclut l'événement lui-même (id) pour ne pas le comparer avec sa propre ancienne version en mémoire
            Event previousEvent = userEvents.stream()
                .filter(e -> !e.getId().equals(event.getId())) 
                .filter(e -> e.getEndTime().isBefore(event.getStartTime()) || 
                             e.getEndTime().isEqual(event.getStartTime()))
                .max(Comparator.comparing(Event::getEndTime))
                .orElse(null);

            // 3. Si un événement précédent existe et a une localisation
            if (previousEvent != null && previousEvent.getLocation() != null) {
                TravelTime.TransportMode mode = TravelTime.TransportMode.valueOf(eventRequest.getTransportMode());
                TravelTimeCalculator calculator = getCalculator(eventRequest.getUseGoogleMaps());

                int durationMinutes = calculator.calculateTravelTime(
                    previousEvent.getLocation(),
                    event.getLocation(),
                    mode
                );
                
                LocalDateTime estimatedArrival = previousEvent.getEndTime().plusMinutes(durationMinutes);
                
                if (estimatedArrival.isAfter(event.getStartTime())) {
                    throw new IllegalArgumentException("Impossible d'arriver à l'heure (trajet de " + durationMinutes + " min).");
                }
                
                Event savedEvent = eventRepository.save(event);
                travelTimeService.createTravelTimeWithDuration(previousEvent, savedEvent, mode, durationMinutes);
                return savedEvent;
            }
        }

        return eventRepository.save(event);
    }

    /**
     * Supprime un événement (logique de 5512fe3).
     */
    @Override
    @Transactional
    public void deleteEvent(Long id) {
        if (!eventRepository.existsById(id)) {
            throw new IllegalArgumentException("Event not found with id: " + id);
        }
        // Grâce aux cascades ajoutées dans Event.java, ceci supprimera aussi la Location liée
        eventRepository.deleteById(id);
    }

    /**
     * Récupère les événements d'un utilisateur dans une période donnée (logique de 5512fe3).
     */
    @Override
    public List<Event> getEventsByUserIdAndPeriod(Long userId, LocalDateTime start, LocalDateTime end) {
        return eventRepository.findByUser_IdAndStartTimeBetween(userId, start, end);
    }

    /**
     * Recalcule tous les temps de trajets pour les événements futurs d'un utilisateur.
     * Cette méthode est appelée lors du changement de configuration (Google vs Simple).
     */
    @Override
    @Transactional
    public void recalculateAllTravelTimes(Long userId, Boolean useGoogleMaps) {
        // 1. Récupérer tous les événements de l'utilisateur triés
        List<Event> events = eventRepository.findByUser_IdOrderByStartTime(userId);
        
        if (events.isEmpty()) return;

        // Choix du calculateur
        TravelTimeCalculator calculator = getCalculator(useGoogleMaps);

        // 2. Parcourir les événements pour trouver les paires consécutives
        for (int i = 0; i < events.size() - 1; i++) {
            Event current = events.get(i);
            Event next = events.get(i + 1);

            try {
                // On vérifie que les objets Location ne sont pas nulls
                if (current.getLocation() != null && next.getLocation() != null) {
                    
                    TravelTime existingTt = travelTimeRepository.findByFromEventAndToEvent(current, next)
                            .orElse(null);

                    // --- CORRECTION 500 : GESTION DU MODE NULL ---
                    TravelTime.TransportMode mode = TravelTime.TransportMode.DRIVING; // Valeur par défaut
                    
                    // Si un trajet existe mais que son mode est corrompu (null), on garde DRIVING
                    if (existingTt != null && existingTt.getMode() != null) {
                        mode = existingTt.getMode();
                    }

                    // Calcul du temps (protégé par le try-catch autour)
                    int newDuration = calculator.calculateTravelTime(current.getLocation(), next.getLocation(), mode);

                    if (existingTt != null) {
                        existingTt.setDurationMinutes(newDuration);
                        travelTimeRepository.save(existingTt);
                    } else {
                        // Optionnel : Si vous vouliez créer les liens manquants, ce serait ici.
                        // Pour l'instant on ne touche qu'aux existants comme demandé.
                    }
                }
            } catch (Exception e) {
                //--- LOG L'ERREUR AU LIEU DE PLANTER TOUT LE SERVEUR ---
                System.err.println("ERREUR NON BLOQUANTE lors du recalcul event " + current.getId() + " -> " + next.getId());
                System.err.println("Cause : " + e.getMessage());
                e.printStackTrace();
            }
        }
    }

    /**
     * RM-05 : Consultation du calendrier partagé (Lecture Seule)
     */
    @Override
    public List<Event> getTeammateEvents(Long requesterId, Long targetUserId) {
        User requester = userRepository.findById(requesterId)
                .orElseThrow(() -> new IllegalArgumentException("Utilisateur requérant introuvable"));
        
        User target = userRepository.findById(targetUserId)
                .orElseThrow(() -> new IllegalArgumentException("Utilisateur cible introuvable"));

        // 1. Vérification de sécurité : Sont-ils dans la même équipe ?
        // On regarde l'intersection des listes d'équipes
        boolean sharedTeam = requester.getTeams().stream()
                .anyMatch(team -> target.getTeams().contains(team));

        if (!sharedTeam) {
            throw new SecurityException("Accès refusé : Vous n'êtes pas dans la même équipe que cet utilisateur.");
        }

        // 2. Récupération des événements
        List<Event> events = eventRepository.findByUser_IdOrderByStartTime(targetUserId);

        // 3. (Optionnel) Confidentialité : Masquer les détails privés
        // Si vous voulez implémenter le mode "Occupé" seulement :
        /*
        return events.stream().map(e -> {
            Event publicView = new Event();
            publicView.setStartTime(e.getStartTime());
            publicView.setEndTime(e.getEndTime());
            publicView.setSummary("Occupé"); // Masque le titre réel
            return publicView;
        }).toList();
        */

        // Pour l'instant, on retourne tout (transparence d'équipe)
        return events;
    }
}