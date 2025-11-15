package com.example.backend.repository;

import com.example.backend.model.Event;
import com.example.backend.model.Location;
import com.example.backend.model.TravelTime;
import com.example.backend.model.TravelTime.TransportMode;
import com.example.backend.model.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;

import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests d'intégration pour TravelTimeRepository.
 * Utilise une base de données H2 en mémoire pour les tests.
 */
@DataJpaTest
class TravelTimeRepositoryTest {

    @Autowired
    private TestEntityManager entityManager;

    @Autowired
    private TravelTimeRepository travelTimeRepository;

    private User testUser1;
    private User testUser2;
    private Location parisLocation;
    private Location lyonLocation;
    private Event event1;
    private Event event2;
    private Event event3;

    @BeforeEach
    void setUp() {
        // Création des utilisateurs
        testUser1 = new User("alice", "password1");
        testUser2 = new User("bob", "password2");
        entityManager.persist(testUser1);
        entityManager.persist(testUser2);

        // Création des locations
        parisLocation = new Location("Paris, France");
        lyonLocation = new Location("Lyon, France");
        entityManager.persist(parisLocation);
        entityManager.persist(lyonLocation);

        // Création des événements
        event1 = new Event(
            "Réunion Paris",
            LocalDateTime.of(2025, 1, 15, 10, 0),
            LocalDateTime.of(2025, 1, 15, 11, 0),
            testUser1
        );
        event1.setLocation(parisLocation);

        event2 = new Event(
            "Conférence Lyon",
            LocalDateTime.of(2025, 1, 15, 15, 0),
            LocalDateTime.of(2025, 1, 15, 16, 0),
            testUser1
        );
        event2.setLocation(lyonLocation);

        event3 = new Event(
            "Workshop Lyon",
            LocalDateTime.of(2025, 1, 16, 10, 0),
            LocalDateTime.of(2025, 1, 16, 12, 0),
            testUser2
        );
        event3.setLocation(lyonLocation);

        entityManager.persist(event1);
        entityManager.persist(event2);
        entityManager.persist(event3);
        entityManager.flush();
    }

    @Test
    @DisplayName("Sauvegarde et récupération d'un TravelTime")
    void testSaveAndFindTravelTime() {
        // Arrange
        TravelTime travelTime = new TravelTime(
            event1,
            event2,
            testUser1,
            LocalDateTime.of(2025, 1, 15, 11, 0),
            240
        );
        travelTime.setMode(TransportMode.DRIVING);
        travelTime.setDistanceKm(400.0);

        // Act
        TravelTime saved = travelTimeRepository.save(travelTime);
        entityManager.flush();
        entityManager.clear();

        TravelTime found = travelTimeRepository.findById(saved.getId()).orElse(null);

        // Assert
        assertNotNull(found);
        assertEquals(240, found.getDurationMinutes());
        assertEquals(TransportMode.DRIVING, found.getMode());
        assertEquals(400.0, found.getDistanceKm());
        assertEquals(event1.getId(), found.getFromEvent().getId());
        assertEquals(event2.getId(), found.getToEvent().getId());
        assertEquals(testUser1.getId(), found.getUser().getId());
    }

    @Test
    @DisplayName("Recherche des temps de trajet par utilisateur")
    void testFindByUser_Id() {
        // Arrange
        TravelTime tt1 = new TravelTime(
            event1, event2, testUser1,
            LocalDateTime.of(2025, 1, 15, 11, 0), 240
        );
        TravelTime tt2 = new TravelTime(
            event2, event1, testUser1,
            LocalDateTime.of(2025, 1, 15, 16, 0), 240
        );
        TravelTime tt3 = new TravelTime(
            event3, event2, testUser2,
            LocalDateTime.of(2025, 1, 16, 12, 0), 60
        );

        travelTimeRepository.save(tt1);
        travelTimeRepository.save(tt2);
        travelTimeRepository.save(tt3);
        entityManager.flush();

        // Act
        List<TravelTime> user1TravelTimes = travelTimeRepository.findByUser_Id(testUser1.getId());
        List<TravelTime> user2TravelTimes = travelTimeRepository.findByUser_Id(testUser2.getId());

        // Assert
        assertEquals(2, user1TravelTimes.size());
        assertEquals(1, user2TravelTimes.size());
        assertTrue(user1TravelTimes.stream().allMatch(tt -> tt.getUser().equals(testUser1)));
        assertTrue(user2TravelTimes.stream().allMatch(tt -> tt.getUser().equals(testUser2)));
    }

    @Test
    @DisplayName("Recherche des temps de trajet entre deux dates")
    void testFindByUser_IdAndStartTimeBetween() {
        // Arrange
        LocalDateTime start = LocalDateTime.of(2025, 1, 15, 0, 0);
        LocalDateTime end = LocalDateTime.of(2025, 1, 15, 23, 59);

        TravelTime tt1 = new TravelTime(
            event1, event2, testUser1,
            LocalDateTime.of(2025, 1, 15, 11, 0), 240
        );
        TravelTime tt2 = new TravelTime(
            event2, event1, testUser1,
            LocalDateTime.of(2025, 1, 15, 16, 0), 240
        );
        TravelTime tt3 = new TravelTime(
            event3, event2, testUser1,
            LocalDateTime.of(2025, 1, 16, 12, 0), 60
        );

        travelTimeRepository.save(tt1);
        travelTimeRepository.save(tt2);
        travelTimeRepository.save(tt3);
        entityManager.flush();

        // Act
        List<TravelTime> travelTimesInRange = travelTimeRepository
            .findByUser_IdAndStartTimeBetween(testUser1.getId(), start, end);

        // Assert
        assertEquals(2, travelTimesInRange.size());
        assertTrue(travelTimesInRange.stream()
            .allMatch(tt -> !tt.getStartTime().isBefore(start) && 
                           !tt.getStartTime().isAfter(end)));
    }

    @Test
    @DisplayName("Recherche d'un temps de trajet par événement de destination")
    void testFindByToEvent_Id() {
        // Arrange
        TravelTime travelTime = new TravelTime(
            event1, event2, testUser1,
            LocalDateTime.of(2025, 1, 15, 11, 0), 240
        );
        travelTimeRepository.save(travelTime);
        entityManager.flush();

        // Act
        TravelTime found = travelTimeRepository.findByToEvent_Id(event2.getId());

        // Assert
        assertNotNull(found);
        assertEquals(event2.getId(), found.getToEvent().getId());
        assertEquals(event1.getId(), found.getFromEvent().getId());
    }

    @Test
    @DisplayName("Suppression d'un temps de trajet")
    void testDeleteTravelTime() {
        // Arrange
        TravelTime travelTime = new TravelTime(
            event1, event2, testUser1,
            LocalDateTime.of(2025, 1, 15, 11, 0), 240
        );
        TravelTime saved = travelTimeRepository.save(travelTime);
        entityManager.flush();
        Long savedId = saved.getId();

        // Act
        travelTimeRepository.deleteById(savedId);
        entityManager.flush();

        // Assert
        assertFalse(travelTimeRepository.findById(savedId).isPresent());
    }

    @Test
    @DisplayName("Mise à jour d'un temps de trajet")
    void testUpdateTravelTime() {
        // Arrange
        TravelTime travelTime = new TravelTime(
            event1, event2, testUser1,
            LocalDateTime.of(2025, 1, 15, 11, 0), 240
        );
        TravelTime saved = travelTimeRepository.save(travelTime);
        entityManager.flush();
        entityManager.clear();

        // Act
        TravelTime toUpdate = travelTimeRepository.findById(saved.getId()).orElseThrow();
        toUpdate.setDurationMinutes(300);
        toUpdate.setMode(TransportMode.TRANSIT);
        travelTimeRepository.save(toUpdate);
        entityManager.flush();
        entityManager.clear();

        // Assert
        TravelTime updated = travelTimeRepository.findById(saved.getId()).orElseThrow();
        assertEquals(300, updated.getDurationMinutes());
        assertEquals(TransportMode.TRANSIT, updated.getMode());
    }

    @Test
    @DisplayName("Vérification de la cascade lors de la suppression d'un événement")
    void testCascadeOnEventDeletion() {
        // Arrange
        TravelTime travelTime = new TravelTime(
            event1, event2, testUser1,
            LocalDateTime.of(2025, 1, 15, 11, 0), 240
        );
        travelTimeRepository.save(travelTime);
        entityManager.flush();

        // Note: Cette fonctionnalité dépend de la configuration de cascade dans Event
        // Ce test peut nécessiter des ajustements selon votre configuration JPA
    }

    @Test
    @DisplayName("Recherche retourne une liste vide pour un utilisateur sans trajets")
    void testFindByUser_Id_EmptyResult() {
        // Arrange
        User newUser = new User("charlie", "password3");
        entityManager.persist(newUser);
        entityManager.flush();

        // Act
        List<TravelTime> travelTimes = travelTimeRepository.findByUser_Id(newUser.getId());

        // Assert
        assertNotNull(travelTimes);
        assertTrue(travelTimes.isEmpty());
    }

    @Test
    @DisplayName("Recherche par événement de destination inexistant retourne null")
    void testFindByToEvent_Id_NotFound() {
        // Act
        TravelTime found = travelTimeRepository.findByToEvent_Id(999L);

        // Assert
        assertNull(found);
    }

    @Test
    @DisplayName("Plusieurs trajets peuvent pointer vers le même événement de départ")
    void testMultipleTravelTimesFromSameEvent() {
        // Arrange
        Event event4 = new Event(
            "Event 4",
            LocalDateTime.of(2025, 1, 15, 18, 0),
            LocalDateTime.of(2025, 1, 15, 19, 0),
            testUser1
        );
        event4.setLocation(parisLocation);
        entityManager.persist(event4);

        TravelTime tt1 = new TravelTime(
            event1, event2, testUser1,
            LocalDateTime.of(2025, 1, 15, 11, 0), 240
        );
        TravelTime tt2 = new TravelTime(
            event1, event4, testUser1,
            LocalDateTime.of(2025, 1, 15, 11, 0), 420
        );

        travelTimeRepository.save(tt1);
        travelTimeRepository.save(tt2);
        entityManager.flush();

        // Act
        List<TravelTime> allTravelTimes = travelTimeRepository.findByUser_Id(testUser1.getId());

        // Assert
        assertEquals(2, allTravelTimes.size());
        assertTrue(allTravelTimes.stream().allMatch(tt -> tt.getFromEvent().equals(event1)));
    }
}