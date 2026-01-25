package com.example.backend.repository;

import com.example.backend.model.Event;
import com.example.backend.model.User;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests d'intégration pour EventRepository.
 * Utilise une base de données H2 en mémoire pour tester les opérations CRUD.
 */
@DataJpaTest
class EventRepositoryTest {

    @Autowired
    private TestEntityManager entityManager;

    @Autowired
    private EventRepository eventRepository;

    @Test
    void testSaveEvent() {
        // Arrange
        User user = new User("testuser", "password");
        entityManager.persist(user);
        entityManager.flush();

        Event event = new Event(
            "Test Event",
            LocalDateTime.of(2025, 1, 15, 10, 0),
            LocalDateTime.of(2025, 1, 15, 11, 0),
            user
        );

        // Act
        Event savedEvent = eventRepository.save(event);

        // Assert
        assertNotNull(savedEvent.getId());
        assertEquals("Test Event", savedEvent.getSummary());
        assertEquals(user.getId(), savedEvent.getUser().getId());
    }

    @Test
    void testFindById() {
        // Arrange
        User user = new User("alice", "password");
        entityManager.persist(user);

        Event event = new Event(
            "Meeting",
            LocalDateTime.of(2025, 2, 10, 14, 0),
            LocalDateTime.of(2025, 2, 10, 15, 0),
            user
        );
        entityManager.persist(event);
        entityManager.flush();

        // Act
        Optional<Event> foundEvent = eventRepository.findById(event.getId());

        // Assert
        assertTrue(foundEvent.isPresent());
        assertEquals("Meeting", foundEvent.get().getSummary());
        assertEquals(event.getId(), foundEvent.get().getId());
    }

    @Test
    void testFindById_NotFound() {
        // Act
        Optional<Event> foundEvent = eventRepository.findById(999L);

        // Assert
        assertFalse(foundEvent.isPresent());
    }

    @Test
    void testFindAll() {
        // Arrange
        User user = new User("bob", "password");
        entityManager.persist(user);

        Event event1 = new Event(
            "Event 1",
            LocalDateTime.of(2025, 1, 15, 10, 0),
            LocalDateTime.of(2025, 1, 15, 11, 0),
            user
        );
        Event event2 = new Event(
            "Event 2",
            LocalDateTime.of(2025, 1, 16, 10, 0),
            LocalDateTime.of(2025, 1, 16, 11, 0),
            user
        );

        entityManager.persist(event1);
        entityManager.persist(event2);
        entityManager.flush();

        // Act
        List<Event> events = eventRepository.findAll();

        // Assert
        assertTrue(events.size() >= 2);
        assertTrue(events.stream().anyMatch(e -> e.getSummary().equals("Event 1")));
        assertTrue(events.stream().anyMatch(e -> e.getSummary().equals("Event 2")));
    }

    @Test
    void testUpdateEvent() {
        // Arrange
        User user = new User("charlie", "password");
        entityManager.persist(user);

        Event event = new Event(
            "Original Title",
            LocalDateTime.of(2025, 3, 1, 9, 0),
            LocalDateTime.of(2025, 3, 1, 10, 0),
            user
        );
        entityManager.persist(event);
        entityManager.flush();

        // Act
        event.setSummary("Updated Title");
        event.setStatus(Event.EventStatus.CONFIRMED);
        Event updatedEvent = eventRepository.save(event);

        // Assert
        assertEquals("Updated Title", updatedEvent.getSummary());
        assertEquals(Event.EventStatus.CONFIRMED, updatedEvent.getStatus());
    }

    @Test
    void testDeleteEvent() {
        // Arrange
        User user = new User("dave", "password");
        entityManager.persist(user);

        Event event = new Event(
            "To Delete",
            LocalDateTime.of(2025, 4, 1, 10, 0),
            LocalDateTime.of(2025, 4, 1, 11, 0),
            user
        );
        entityManager.persist(event);
        entityManager.flush();

        Long eventId = event.getId();

        // Act
        eventRepository.deleteById(eventId);
        entityManager.flush();

        // Assert
        Optional<Event> deletedEvent = eventRepository.findById(eventId);
        assertFalse(deletedEvent.isPresent());
    }

    @Test
    void testSaveEventWithStatus() {
        // Arrange
        User user = new User("eve", "password");
        entityManager.persist(user);

        Event event = new Event(
            "Cancelled Event",
            LocalDateTime.of(2025, 5, 1, 10, 0),
            LocalDateTime.of(2025, 5, 1, 11, 0),
            user
        );
        event.setStatus(Event.EventStatus.PENDING_DELETION);

        // Act
        Event savedEvent = eventRepository.save(event);

        // Assert
        assertNotNull(savedEvent.getId());
        assertEquals(Event.EventStatus.PENDING_DELETION, savedEvent.getStatus());
    }

    @Test
    void testCount() {
        // Arrange
        User user = new User("frank", "password");
        entityManager.persist(user);

        long initialCount = eventRepository.count();

        Event event = new Event(
            "Count Test",
            LocalDateTime.now(),
            LocalDateTime.now().plusHours(1),
            user
        );
        entityManager.persist(event);
        entityManager.flush();

        // Act
        long newCount = eventRepository.count();

        // Assert
        assertEquals(initialCount + 1, newCount);
    }

    @Test
    void testExistsById() {
        // Arrange
        User user = new User("grace", "password");
        entityManager.persist(user);

        Event event = new Event(
            "Exists Test",
            LocalDateTime.now(),
            LocalDateTime.now().plusHours(1),
            user
        );
        entityManager.persist(event);
        entityManager.flush();

        // Act
        boolean exists = eventRepository.existsById(event.getId());
        boolean notExists = eventRepository.existsById(999L);

        // Assert
        assertTrue(exists);
        assertFalse(notExists);
    }
}