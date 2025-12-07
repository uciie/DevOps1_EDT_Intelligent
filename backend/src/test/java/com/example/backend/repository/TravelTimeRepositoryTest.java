package com.example.backend.repository;

import com.example.backend.model.Event;
import com.example.backend.model.TravelTime;
import com.example.backend.model.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

@DataJpaTest
public class TravelTimeRepositoryTest {

    @Autowired
    private TravelTimeRepository travelTimeRepository;

    @Autowired
    private EventRepository eventRepository;

    @Autowired
    private UserRepository userRepository;

    private User user;
    private Event event1;
    private Event event2;
    private TravelTime travelTime;

    @BeforeEach
    void setUp() {
        // 1. Créer et Sauvegarder l'Utilisateur
        user = new User("John", "password");
        user = userRepository.save(user);

        // 2. Créer et Sauvegarder les Événements liés à l'utilisateur
        event1 = new Event("Départ", LocalDateTime.now(), LocalDateTime.now().plusHours(1), user);
        event2 = new Event("Arrivée", LocalDateTime.now().plusHours(2), LocalDateTime.now().plusHours(3), user);
        
        event1 = eventRepository.save(event1);
        event2 = eventRepository.save(event2);

        // 3. Créer le TravelTime (30 minutes de trajet)
        travelTime = new TravelTime(event1, event2, user, 
                                    LocalDateTime.now().plusHours(1), 30);
        travelTime = travelTimeRepository.save(travelTime);
    }

    @Test
    void testSaveAndRetrieveTravelTime() {
        Optional<TravelTime> found = travelTimeRepository.findById(travelTime.getId());
        assertTrue(found.isPresent());
        // Correction : getDurationMinutes() au lieu de getDurationSeconds()
        assertEquals(30, found.get().getDurationMinutes());
    }

    @Test
    void testDeleteTravelTime() {
        travelTimeRepository.delete(travelTime);
        Optional<TravelTime> found = travelTimeRepository.findById(travelTime.getId());
        assertFalse(found.isPresent());
    }

    @Test
    void testFindByToEvent() {
        // Correction : Le champ dans l'entité est 'toEvent', donc JPA attend 'findByToEvent'
        List<TravelTime> results = travelTimeRepository.findByToEvent(event2);
        
        assertFalse(results.isEmpty());
        // Correction : getFromEvent() au lieu de getStartEvent()
        assertEquals(event1.getId(), results.get(0).getFromEvent().getId());
    }

    @Test
    void testFindByToEvent_NotFound() {
        Event otherEvent = new Event("Autre", LocalDateTime.now(), LocalDateTime.now(), user);
        otherEvent = eventRepository.save(otherEvent); 
        
        // Correction : findByToEvent
        List<TravelTime> results = travelTimeRepository.findByToEvent(otherEvent);
        assertTrue(results.isEmpty());
    }
    
    @Test
    void testUpdateTravelTime() {
        // Correction : setDurationMinutes attend un int, pas un Long.
        // On change la durée à 60 minutes.
        travelTime.setDurationMinutes(60);
        travelTimeRepository.save(travelTime);
        
        TravelTime updated = travelTimeRepository.findById(travelTime.getId()).get();
        // Correction : getDurationMinutes
        assertEquals(60, updated.getDurationMinutes());
    }
}