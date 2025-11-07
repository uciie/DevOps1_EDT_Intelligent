package com.example.backend.model;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Tests unitaires pour la classe Event.
 * Vérifie le bon fonctionnement des constructeurs, getters/setters
 * et des relations entre Event, User et Task.
 */
class EventTest {

    /**
     * Teste le constructeur principal de Event ainsi que ses getters.
     * Vérifie que les valeurs sont correctement initialisées et que l'ID est nul avant la persistance.
     */
    @Test
    void testEventConstructorAndGetters() {
        // Création d'un utilisateur associé à l'événement
        User user = new User("alice", "password");
        // Création d'une plage horaire
        LocalDateTime start = LocalDateTime.now();
        LocalDateTime end = start.plusHours(2);

        // Création d'un événement
        Event event = new Event("Project meeting", start, end, user);

        // Vérifications des valeurs du constructeur
        assertNull(event.getId()); // L'ID doit être null avant la sauvegarde JPA
        assertEquals("Project meeting", event.getSummary());
        assertEquals(start, event.getStartTime());
        assertEquals(end, event.getEndTime());
        assertEquals(user, event.getUser());
        assertEquals("PLANNED", event.getStatus()); // Statut par défaut
    }

    /**
     * Teste les méthodes alias (constructeur alternatif) sans utilisateur.
     * Vérifie que les valeurs passées sont bien affectées.
     */
    @Test
    void testEventAliasMethods() {
        LocalDateTime start = LocalDateTime.now();
        LocalDateTime end = start.plusHours(1);

        // Utilisation du constructeur simplifié sans user
        Event event = new Event("Alias test", start, end);

        // Vérification des attributs
        assertEquals("Alias test", event.getSummary());
        assertEquals(start, event.getStartTime());
        assertEquals(end, event.getEndTime());
    }

    /**
     * Teste la relation entre Event, User et Task.
     * Vérifie que les tâches liées à un événement sont bien associées
     * et que le statut peut être modifié correctement.
     */
    @Test
    void testEventRelationsAndStatus() {
        // Création de l'utilisateur et de l'événement
        User user = new User("bob", "pwd");
        Event event = new Event("Workshop", LocalDateTime.now(), LocalDateTime.now().plusHours(1), user);

        // Création de deux tâches liées à l'événement
        Task task1 = new Task("Prepare slides", 30, 2, false, user, event);
        Task task2 = new Task("Send invites", 15, 1, true, user, event);

        // Association des tâches à l'événement
        event.setTasks(List.of(task1, task2));

        // Modification du statut
        event.setStatus("DONE");

        // Vérifications des relations et du statut
        assertEquals("DONE", event.getStatus());
        assertEquals(2, event.getTasks().size());
        assertEquals(user, event.getUser());
        assertEquals(user.getId(), event.getUserId()); // Vérifie la cohérence user ↔ event
    }

    @Test
    void testEventDefaultConstructor() {
        // Act
        Event event = new Event();

        // Assert
        assertNull(event.getId());
        assertNull(event.getSummary());
        assertNull(event.getStartTime());
        assertNull(event.getEndTime());
        assertEquals("PLANNED", event.getStatus());
        assertNull(event.getUser());
        assertNull(event.getTasks());
    }

    @Test
    void testEventSetSummary() {
        // Arrange
        Event event = new Event();

        // Act
        event.setSummary("New Summary");

        // Assert
        assertEquals("New Summary", event.getSummary());
    }

    @Test
    void testEventSetStartTime() {
        // Arrange
        Event event = new Event();
        LocalDateTime newStart = LocalDateTime.of(2025, 3, 20, 14, 30);

        // Act
        event.setStartTime(newStart);

        // Assert
        assertEquals(newStart, event.getStartTime());
    }

    @Test
    void testEventSetEndTime() {
        // Arrange
        Event event = new Event();
        LocalDateTime newEnd = LocalDateTime.of(2025, 3, 20, 16, 30);

        // Act
        event.setEndTime(newEnd);

        // Assert
        assertEquals(newEnd, event.getEndTime());
    }

    @Test
    void testEventSetUser() {
        // Arrange
        Event event = new Event();
        User user = new User("testuser", "password");

        // Act
        event.setUser(user);

        // Assert
        assertEquals(user, event.getUser());
    }

    @Test
    void testEventGetUserId_WithUser() {
        // Arrange
        User user = new User("alice", "password");
        user.setId(123L);
        Event event = new Event("Meeting", 
            LocalDateTime.now(),
            LocalDateTime.now().plusHours(1),
            user);

        // Act
        Long userId = event.getUserId();

        // Assert
        assertEquals(123L, userId);
    }

    @Test
    void testEventGetUserId_WithoutUser() {
        // Arrange
        Event event = new Event();

        // Act
        Long userId = event.getUserId();

        // Assert
        assertNull(userId);
    }

    @Test
    void testEventSetStatus_AllValues() {
        // Arrange
        Event event = new Event();

        // Act & Assert
        event.setStatus("PLANNED");
        assertEquals("PLANNED", event.getStatus());

        event.setStatus("CANCELLED");
        assertEquals("CANCELLED", event.getStatus());

        event.setStatus("DONE");
        assertEquals("DONE", event.getStatus());

        event.setStatus("IN_PROGRESS");
        assertEquals("IN_PROGRESS", event.getStatus());
    }

    @Test
    void testEventWithLocation() {
        // Arrange
        User user = new User("bob", "password");
        Location location = new Location("Paris", 48.8566, 2.3522);
        
        Event event = new Event("Conference", 
            LocalDateTime.of(2025, 6, 15, 10, 0),
            LocalDateTime.of(2025, 6, 15, 18, 0),
            user);

        // Act
        event.setLocation(location);

        // Assert
        assertNotNull(event.getLocation());
        assertEquals(location, event.getLocation());
        assertEquals("Paris", event.getLocation().getAddress());
    }

    @Test
    void testEventLocation_GetterSetter() {
        // Arrange
        Event event = new Event();
        Location location1 = new Location("Location 1");
        Location location2 = new Location("Location 2");

        // Act
        event.setLocation(location1);
        assertEquals(location1, event.getLocation());

        event.setLocation(location2);
        assertEquals(location2, event.getLocation());

        event.setLocation(null);

        // Assert
        assertNull(event.getLocation());
    }

    @Test
    void testEventConstructorWithThreeParameters() {
        // Arrange
        LocalDateTime start = LocalDateTime.of(2025, 1, 15, 10, 0);
        LocalDateTime end = LocalDateTime.of(2025, 1, 15, 11, 0);

        // Act
        Event event = new Event("Test Event", start, end);

        // Assert
        assertEquals("Test Event", event.getSummary());
        assertEquals(start, event.getStartTime());
        assertEquals(end, event.getEndTime());
        assertNull(event.getUser());
        assertEquals("PLANNED", event.getStatus());
    }

    @Test
    void testEventWithEmptySummary() {
        // Arrange & Act
        Event event = new Event("", 
            LocalDateTime.now(),
            LocalDateTime.now().plusHours(1));

        // Assert
        assertEquals("", event.getSummary());
    }

    @Test
    void testEventWithNullStatus() {
        // Arrange
        Event event = new Event();

        // Act
        event.setStatus(null);

        // Assert
        assertNull(event.getStatus());
    }

    @Test
    void testEventTimeSpan() {
        // Arrange
        LocalDateTime start = LocalDateTime.of(2025, 1, 15, 9, 0);
        LocalDateTime end = LocalDateTime.of(2025, 1, 15, 17, 0);
        User user = new User("charlie", "password");

        // Act
        Event event = new Event("All Day Event", start, end, user);

        // Assert
        assertEquals(8, java.time.Duration.between(start, end).toHours());
        assertEquals(start, event.getStartTime());
        assertEquals(end, event.getEndTime());
    }
}
