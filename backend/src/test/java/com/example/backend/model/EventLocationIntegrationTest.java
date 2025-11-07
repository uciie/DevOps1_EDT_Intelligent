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
        // Arrange
        User user = new User("alice", "password");
        Location location = new Location("Paris, France", 48.8566, 2.3522);
        
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
        // Arrange
        User user = new User("charlie", "password");
        Location location = new Location("123 Main St", 40.7128, -74.0060);
        location.setName("Office HQ");
        
        Event event = new Event("Team Meeting", 
            LocalDateTime.now(),
            LocalDateTime.now().plusHours(1),
            user);

        // Act
        event.setLocation(location);

        // Assert
        assertEquals("Office HQ", event.getLocation().getName());
        assertEquals("123 Main St", event.getLocation().getAddress());
    }

    @Test
    void testEvent_ChangeLocation() {
        // Arrange
        User user = new User("dave", "password");
        Location location1 = new Location("Old Office", 48.8566, 2.3522);
        Location location2 = new Location("New Office", 48.8700, 2.3400);
        
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
        assertEquals("New Office", event.getLocation().getAddress());
    }

    @Test
    void testMultipleEvents_SameLocation() {
        // Arrange
        User user = new User("eve", "password");
        Location sharedLocation = new Location("Conference Room", 48.8566, 2.3522);
        
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
}