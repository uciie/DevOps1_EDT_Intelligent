package com.example.backend.model;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

import java.time.LocalDateTime;

/**
 * Tests unitaires pour la classe TravelTime.
 */
class TravelTimeTest {

    @Test
    void testTravelTimeConstructor() {
        User user = new User("alice", "password");
        Location loc1 = new Location("Paris", 48.8566, 2.3522);
        Location loc2 = new Location("Lyon", 45.7640, 4.8357);
        
        Event event1 = new Event("Meeting", 
            LocalDateTime.of(2025, 1, 15, 10, 0),
            LocalDateTime.of(2025, 1, 15, 11, 0),
            user);
        event1.setLocation(loc1);
        
        Event event2 = new Event("Conference", 
            LocalDateTime.of(2025, 1, 15, 15, 0),
            LocalDateTime.of(2025, 1, 15, 16, 0),
            user);
        event2.setLocation(loc2);
        
        LocalDateTime startTime = LocalDateTime.of(2025, 1, 15, 11, 0);
        
        TravelTime travelTime = new TravelTime(event1, event2, user, startTime, 240);
        
        assertNull(travelTime.getId());
        assertEquals(event1, travelTime.getFromEvent());
        assertEquals(event2, travelTime.getToEvent());
        assertEquals(user, travelTime.getUser());
        assertEquals(startTime, travelTime.getStartTime());
        assertEquals(240, travelTime.getDurationMinutes());
        assertEquals(startTime.plusMinutes(240), travelTime.getEndTime());
        assertEquals(TravelTime.TransportMode.DRIVING, travelTime.getMode());
    }

    @Test
    void testSetStartTime_UpdatesEndTime() {
        TravelTime travelTime = new TravelTime();
        travelTime.setDurationMinutes(60);
        
        LocalDateTime newStartTime = LocalDateTime.of(2025, 1, 15, 14, 0);
        travelTime.setStartTime(newStartTime);
        
        assertEquals(newStartTime, travelTime.getStartTime());
        assertEquals(newStartTime.plusMinutes(60), travelTime.getEndTime());
    }

    @Test
    void testSetDurationMinutes_UpdatesEndTime() {
        TravelTime travelTime = new TravelTime();
        LocalDateTime startTime = LocalDateTime.of(2025, 1, 15, 14, 0);
        travelTime.setStartTime(startTime);
        
        travelTime.setDurationMinutes(90);
        
        assertEquals(90, travelTime.getDurationMinutes());
        assertEquals(startTime.plusMinutes(90), travelTime.getEndTime());
    }

    @Test
    void testSetMode() {
        TravelTime travelTime = new TravelTime();
        
        travelTime.setMode(TravelTime.TransportMode.WALKING);
        assertEquals(TravelTime.TransportMode.WALKING, travelTime.getMode());
        
        travelTime.setMode(TravelTime.TransportMode.CYCLING);
        assertEquals(TravelTime.TransportMode.CYCLING, travelTime.getMode());
        
        travelTime.setMode(TravelTime.TransportMode.TRANSIT);
        assertEquals(TravelTime.TransportMode.TRANSIT, travelTime.getMode());
    }

    @Test
    void testSetDistanceKm() {
        TravelTime travelTime = new TravelTime();
        
        travelTime.setDistanceKm(392.5);
        assertEquals(392.5, travelTime.getDistanceKm());
    }

    @Test
    void testSetEvents() {
        TravelTime travelTime = new TravelTime();
        User user = new User("bob", "password");
        
        Event fromEvent = new Event("Start", 
            LocalDateTime.now(), 
            LocalDateTime.now().plusHours(1), 
            user);
        Event toEvent = new Event("End", 
            LocalDateTime.now().plusHours(2), 
            LocalDateTime.now().plusHours(3), 
            user);
        
        travelTime.setFromEvent(fromEvent);
        travelTime.setToEvent(toEvent);
        travelTime.setUser(user);
        
        assertEquals(fromEvent, travelTime.getFromEvent());
        assertEquals(toEvent, travelTime.getToEvent());
        assertEquals(user, travelTime.getUser());
    }

    @Test
    void testSetEndTime() {
        TravelTime travelTime = new TravelTime();
        LocalDateTime endTime = LocalDateTime.of(2025, 1, 15, 16, 0);
        
        travelTime.setEndTime(endTime);
        
        assertEquals(endTime, travelTime.getEndTime());
    }

    @Test
    void testTransportModeEnum() {
        assertEquals(4, TravelTime.TransportMode.values().length);
        assertNotNull(TravelTime.TransportMode.valueOf("WALKING"));
        assertNotNull(TravelTime.TransportMode.valueOf("DRIVING"));
        assertNotNull(TravelTime.TransportMode.valueOf("TRANSIT"));
        assertNotNull(TravelTime.TransportMode.valueOf("CYCLING"));
    }
}