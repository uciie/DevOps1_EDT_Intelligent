package com.example.backend.service;

import com.example.backend.model.Event;
import com.example.backend.model.Location;
import com.example.backend.model.TravelTime;
import com.example.backend.model.TravelTime.TransportMode;
import com.example.backend.model.User;
import com.example.backend.repository.EventRepository;
import com.example.backend.repository.TravelTimeRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Tests unitaires pour TravelTimeService
 */
@ExtendWith(MockitoExtension.class)
class TravelTimeServiceTest {

    @Mock
    private TravelTimeRepository travelTimeRepository;

    @Mock
    private TravelTimeCalculator travelTimeCalculator;

    @Mock
    private EventRepository eventRepository;

    @InjectMocks
    private TravelTimeService travelTimeService;

    private User testUser;
    private Event fromEvent;
    private Event toEvent;
    private Location location1;
    private Location location2;
    private TravelTime testTravelTime;

    @BeforeEach
    void setUp() {
        testUser = new User("testuser", "password123");
        testUser.setId(1L);

        location1 = new Location(
            "10 Rue de la Paix, 75002 Paris, France",
            48.8692,
            2.3312
        );
        location1.setId(1L);
        location1.setName("Bureau");

        location2 = new Location(
            "5 Avenue Anatole France, 75007 Paris, France",
            48.8584,
            2.2945
        );
        location2.setId(2L);
        location2.setName("Tour Eiffel");

        fromEvent = new Event(
            "Départ bureau",
            LocalDateTime.of(2025, 11, 25, 9, 0),
            LocalDateTime.of(2025, 11, 25, 10, 0),
            testUser
        );
        fromEvent.setId(1L);
        fromEvent.setLocation(location1);

        toEvent = new Event(
            "Réunion client",
            LocalDateTime.of(2025, 11, 25, 11, 0),
            LocalDateTime.of(2025, 11, 25, 12, 0),
            testUser
        );
        toEvent.setId(2L);
        toEvent.setLocation(location2);

        testTravelTime = new TravelTime(
            fromEvent,
            toEvent,
            testUser,
            LocalDateTime.of(2025, 11, 25, 10, 0),
            25
        );
        testTravelTime.setId(1L);
        testTravelTime.setMode(TransportMode.DRIVING);
    }

    @Test
    void testGetTravelTimesByUserId() {
        // Given
        List<TravelTime> travelTimes = Arrays.asList(testTravelTime);
        when(travelTimeRepository.findByUser_Id(1L)).thenReturn(travelTimes);

        // When
        List<TravelTime> result = travelTimeService.getTravelTimesByUserId(1L);

        // Then
        assertThat(result).hasSize(1);
        assertThat(result.get(0).getDurationMinutes()).isEqualTo(25);
        verify(travelTimeRepository, times(1)).findByUser_Id(1L);
    }

    @Test
    void testGetTravelTimesByUserIdAndDateRange() {
        // Given
        LocalDateTime start = LocalDateTime.of(2025, 11, 25, 0, 0);
        LocalDateTime end = LocalDateTime.of(2025, 11, 26, 0, 0);
        List<TravelTime> travelTimes = Arrays.asList(testTravelTime);
        
        when(travelTimeRepository.findByUser_IdAndStartTimeBetween(1L, start, end))
            .thenReturn(travelTimes);

        // When
        List<TravelTime> result = travelTimeService.getTravelTimesByUserIdAndDateRange(1L, start, end);

        // Then
        assertThat(result).hasSize(1);
        verify(travelTimeRepository, times(1))
            .findByUser_IdAndStartTimeBetween(1L, start, end);
    }

    @Test
    void testCalculateAndCreateTravelTime() {
        // Given
        when(eventRepository.findById(1L)).thenReturn(Optional.of(fromEvent));
        when(eventRepository.findById(2L)).thenReturn(Optional.of(toEvent));
        when(travelTimeCalculator.calculateTravelTime(location1, location2, TransportMode.DRIVING))
            .thenReturn(25);
        when(travelTimeRepository.save(any(TravelTime.class))).thenReturn(testTravelTime);

        // When
        TravelTime result = travelTimeService.calculateAndCreateTravelTime(1L, 2L, TransportMode.DRIVING);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getDurationMinutes()).isEqualTo(25);
        verify(eventRepository, times(1)).findById(1L);
        verify(eventRepository, times(1)).findById(2L);
        verify(travelTimeCalculator, times(1)).calculateTravelTime(location1, location2, TransportMode.DRIVING);
        verify(travelTimeRepository, times(1)).save(any(TravelTime.class));
    }

    @Test
    void testCalculateAndCreateTravelTimeFromEventNotFound() {
        // Given
        when(eventRepository.findById(999L)).thenReturn(Optional.empty());

        // When & Then
        assertThatThrownBy(() -> 
            travelTimeService.calculateAndCreateTravelTime(999L, 2L, TransportMode.DRIVING)
        )
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("FromEvent not found");
    }

    @Test
    void testCalculateAndCreateTravelTimeToEventNotFound() {
        // Given
        when(eventRepository.findById(1L)).thenReturn(Optional.of(fromEvent));
        when(eventRepository.findById(999L)).thenReturn(Optional.empty());

        // When & Then
        assertThatThrownBy(() -> 
            travelTimeService.calculateAndCreateTravelTime(1L, 999L, TransportMode.DRIVING)
        )
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("ToEvent not found");
    }

    @Test
    void testCreateTravelTime() {
        // Given
        when(travelTimeCalculator.calculateTravelTime(location1, location2, TransportMode.DRIVING))
            .thenReturn(25);
        when(travelTimeRepository.save(any(TravelTime.class))).thenReturn(testTravelTime);

        // When
        TravelTime result = travelTimeService.createTravelTime(fromEvent, toEvent, TransportMode.DRIVING);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getDurationMinutes()).isEqualTo(25);
        assertThat(result.getStartTime()).isEqualTo(fromEvent.getEndTime());
        verify(travelTimeCalculator, times(1)).calculateTravelTime(location1, location2, TransportMode.DRIVING);
        verify(travelTimeRepository, times(1)).save(any(TravelTime.class));
    }

    @Test
    void testCreateTravelTimeWithoutFromLocation() {
        // Given
        Event eventWithoutLocation = new Event(
            "Event sans localisation",
            LocalDateTime.of(2025, 11, 25, 9, 0),
            LocalDateTime.of(2025, 11, 25, 10, 0),
            testUser
        );

        // When & Then
        assertThatThrownBy(() -> 
            travelTimeService.createTravelTime(eventWithoutLocation, toEvent, TransportMode.DRIVING)
        )
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("Les événements doivent avoir une localisation");
    }

    @Test
    void testCreateTravelTimeWithoutToLocation() {
        // Given
        Event eventWithoutLocation = new Event(
            "Event sans localisation",
            LocalDateTime.of(2025, 11, 25, 11, 0),
            LocalDateTime.of(2025, 11, 25, 12, 0),
            testUser
        );

        // When & Then
        assertThatThrownBy(() -> 
            travelTimeService.createTravelTime(fromEvent, eventWithoutLocation, TransportMode.DRIVING)
        )
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("Les événements doivent avoir une localisation");
    }

    @Test
    void testCreateTravelTimeWithDifferentModes() {
        // Given
        when(travelTimeCalculator.calculateTravelTime(location1, location2, TransportMode.WALKING))
            .thenReturn(150);
        when(travelTimeRepository.save(any(TravelTime.class))).thenAnswer(i -> i.getArgument(0));

        // When
        TravelTime result = travelTimeService.createTravelTime(fromEvent, toEvent, TransportMode.WALKING);

        // Then
        assertThat(result.getMode()).isEqualTo(TransportMode.WALKING);
        verify(travelTimeCalculator, times(1)).calculateTravelTime(location1, location2, TransportMode.WALKING);
    }

    @Test
    void testUpdateTravelTime() {
        // Given
        when(travelTimeRepository.findById(1L)).thenReturn(Optional.of(testTravelTime));
        when(travelTimeRepository.save(any(TravelTime.class))).thenReturn(testTravelTime);
        LocalDateTime newStartTime = LocalDateTime.of(2025, 11, 25, 10, 30);

        // When
        travelTimeService.updateTravelTime(1L, newStartTime);

        // Then
        verify(travelTimeRepository, times(1)).findById(1L);
        verify(travelTimeRepository, times(1)).save(any(TravelTime.class));
    }

    @Test
    void testUpdateTravelTimeNotFound() {
        // Given
        when(travelTimeRepository.findById(999L)).thenReturn(Optional.empty());

        // When & Then
        assertThatThrownBy(() -> 
            travelTimeService.updateTravelTime(999L, LocalDateTime.now())
        )
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("TravelTime not found");
    }

    @Test
    void testDeleteTravelTime() {
        // When
        travelTimeService.deleteTravelTime(1L);

        // Then
        verify(travelTimeRepository, times(1)).deleteById(1L);
    }

    @Test
    void testCreateTravelTimeCalculatesDistance() {
        // Given
        when(travelTimeCalculator.calculateTravelTime(location1, location2, TransportMode.DRIVING))
            .thenReturn(25);
        when(travelTimeRepository.save(any(TravelTime.class))).thenAnswer(i -> {
            TravelTime tt = i.getArgument(0);
            tt.setId(1L);
            return tt;
        });

        // When
        TravelTime result = travelTimeService.createTravelTime(fromEvent, toEvent, TransportMode.DRIVING);

        // Then
        assertThat(result.getDistanceKm()).isNotNull();
        assertThat(result.getDistanceKm()).isGreaterThan(0);
        // La distance entre ces deux points est d'environ 1.5 km à vol d'oiseau
        assertThat(result.getDistanceKm()).isLessThan(3.0);
    }
}