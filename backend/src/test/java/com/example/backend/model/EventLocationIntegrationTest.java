package com.example.backend.model;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

import java.time.LocalDateTime;

/**
 * Tests d'intégration pour Event avec Location.
 * Teste les relations et la cohérence entre Event et Location.
 */
class EventLocationIntegrationTest {

    @Test
    void testEvent_WithLocation() {
        // Arrange - Adresse complète valide
        User user = new User("alice", "password");
        Location location = new Location(48.8566, 2.3522);
        location.setAddress("Paris, France");
        
        Event event = new Event("Meeting", 
            LocalDateTime.of(2025, 1, 15, 10, 0),
            LocalDateTime.of(2025, 1, 15, 11, 0),
            user);

        // Act
        event.setLocation(location);

        // Assert
        assertNotNull(event.getLocation());
        assertEquals(location, event.getLocation());
        assertEquals("Paris, France", event.getLocation().getAddress());
        assertTrue(event.getLocation().hasCoordinates());
    }

    @Test
    void testEvent_WithoutLocation() {
        // Arrange
        User user = new User("bob", "password");
        
        Event event = new Event("Virtual Meeting", 
            LocalDateTime.of(2025, 1, 15, 14, 0),
            LocalDateTime.of(2025, 1, 15, 15, 0),
            user);

        // Assert
        assertNull(event.getLocation());
    }

    @Test
    void testEvent_LocationWithName() {
        // Arrange - Adresse complète avec nom
        User user = new User("charlie", "password");
        Location location = new Location(40.7128, -74.0060);
        location.setAddress("123 Main Street, New York, USA");
        location.setName("Office HQ");
        
        Event event = new Event("Team Meeting", 
            LocalDateTime.now(),
            LocalDateTime.now().plusHours(1),
            user);

        // Act
        event.setLocation(location);

        // Assert
        assertEquals("Office HQ", event.getLocation().getName());
        assertEquals("123 Main Street, New York, USA", event.getLocation().getAddress());
    }

    @Test
    void testEvent_ChangeLocation() {
        // Arrange - Adresses complètes
        User user = new User("dave", "password");
        Location location1 = new Location(48.8566, 2.3522);
        location1.setAddress("Old Office, Paris, France");
        Location location2 = new Location(48.8700, 2.3400);
        location2.setAddress("New Office, Lyon, France");
        
        Event event = new Event("Meeting", 
            LocalDateTime.now(),
            LocalDateTime.now().plusHours(1),
            user);

        // Act
        event.setLocation(location1);
        assertEquals(location1, event.getLocation());
        
        event.setLocation(location2);

        // Assert
        assertEquals(location2, event.getLocation());
        assertEquals("New Office, Lyon, France", event.getLocation().getAddress());
    }

    @Test
    void testMultipleEvents_SameLocation() {
        // Arrange - Adresse complète
        User user = new User("eve", "password");
        Location sharedLocation = new Location(48.8566, 2.3522);
        sharedLocation.setAddress("Conference Room, Paris, France");
        
        Event event1 = new Event("Morning Meeting", 
            LocalDateTime.of(2025, 1, 15, 9, 0),
            LocalDateTime.of(2025, 1, 15, 10, 0),
            user);
        
        Event event2 = new Event("Afternoon Meeting", 
            LocalDateTime.of(2025, 1, 15, 14, 0),
            LocalDateTime.of(2025, 1, 15, 15, 0),
            user);

        // Act
        event1.setLocation(sharedLocation);
        event2.setLocation(sharedLocation);

        // Assert
        assertSame(sharedLocation, event1.getLocation());
        assertSame(sharedLocation, event2.getLocation());
        assertEquals(event1.getLocation(), event2.getLocation());
    }

    @Test
    void testEvent_LocationWithCompleteAddress() {
        // Arrange - Test avec adresse très détaillée
        User user = new User("frank", "password");
        Location location = new Location(48.8584, 2.2945);
        location.setAddress("5 Avenue Anatole France, 75007 Paris, France");
        location.setName("Tour Eiffel");
        
        Event event = new Event("Site Visit", 
            LocalDateTime.of(2025, 6, 15, 14, 0),
            LocalDateTime.of(2025, 6, 15, 16, 0),
            user);

        // Act
        event.setLocation(location);

        // Assert
        assertEquals("Tour Eiffel", event.getLocation().getName());
        assertEquals("Tour Eiffel", event.getLocation().getDisplayName());
        assertEquals("5 Avenue Anatole France, 75007 Paris, France", event.getLocation().getAddress());
        assertTrue(event.getLocation().hasCoordinates());
    }
}