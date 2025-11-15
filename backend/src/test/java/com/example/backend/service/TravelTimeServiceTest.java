package com.example.backend.service;

import com.example.backend.model.Event;
import com.example.backend.model.Location;
import com.example.backend.model.TravelTime;
import com.example.backend.model.TravelTime.TransportMode;
import com.example.backend.model.User;
import com.example.backend.repository.TravelTimeRepository;
import com.example.backend.service.impl.SimpleTravelTimeCalculator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Tests unitaires complets pour TravelTimeService et calculateurs.
 * SimpleTravelTimeCalculator est testé ici.
 * GoogleMapsTravelTimeCalculator est testé séparément.
 * Les tests couvrent la création, mise à jour, suppression des temps de trajet,
 * ainsi que les calculs de temps de trajet pour différents modes de transport.
 */
class TravelTimeServiceTest {

    @Mock
    private TravelTimeRepository travelTimeRepository;

    private TravelTimeService travelTimeService;
    private SimpleTravelTimeCalculator calculator;

    private User testUser;
    private Location parisLocation;
    private Location lyonLocation;
    private Location marseilleLocation;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        calculator = new SimpleTravelTimeCalculator();
        travelTimeService = new TravelTimeService(travelTimeRepository, calculator);

        // Données de test
        testUser = new User("testuser", "password");
        testUser.setId(1L);

        parisLocation = new Location("Paris, France", 48.8566, 2.3522);
        parisLocation.setId(1L);

        lyonLocation = new Location("Lyon, France", 45.7640, 4.8357);
        lyonLocation.setId(2L);

        marseilleLocation = new Location("Marseille, France", 43.2965, 5.3698);
        marseilleLocation.setId(3L);
    }

    // ==================== Tests TravelTimeService ====================

    @Test
    @DisplayName("Création d'un temps de trajet avec coordonnées valides")
    void testCreateTravelTime_WithValidCoordinates() {
        // Arrange
        Event event1 = createEvent("Réunion Paris", parisLocation, 10, 0, 11, 0);
        Event event2 = createEvent("Conférence Lyon", lyonLocation, 15, 0, 16, 0);

        when(travelTimeRepository.save(any(TravelTime.class)))
            .thenAnswer(invocation -> {
                TravelTime tt = invocation.getArgument(0);
                tt.setId(1L);
                return tt;
            });

        // Act
        TravelTime result = travelTimeService.createTravelTime(
            event1, event2, TransportMode.DRIVING
        );
        System.out.println("Résultat du temps de trajet créé :");
        System.out.println(result);

        // Assert
        assertNotNull(result);
        assertEquals(event1, result.getFromEvent());
        assertEquals(event2, result.getToEvent());
        assertEquals(testUser, result.getUser());
        assertEquals(TransportMode.DRIVING, result.getMode());
        assertTrue(result.getDurationMinutes() > 0, "La durée doit être positive");
        assertNotNull(result.getDistanceKm(), "La distance doit être calculée");
        assertTrue(result.getDistanceKm() > 0, "La distance doit être positive");
        assertEquals(event1.getEndTime(), result.getStartTime());

        verify(travelTimeRepository, times(1)).save(any(TravelTime.class));
    }

    @Test
    @DisplayName("Création d'un temps de trajet sans localisation - Exception")
    void testCreateTravelTime_WithoutLocation_ThrowsException() {
        // Arrange
        Event event1 = createEvent("Event 1", null, 10, 0, 11, 0);
        Event event2 = createEvent("Event 2", lyonLocation, 15, 0, 16, 0);

        // Act & Assert
        IllegalArgumentException exception = assertThrows(
            IllegalArgumentException.class,
            () -> travelTimeService.createTravelTime(event1, event2, TransportMode.DRIVING)
        );

        assertEquals("Les événements doivent avoir une localisation", exception.getMessage());
        verify(travelTimeRepository, never()).save(any(TravelTime.class));
    }

    @Test
    @DisplayName("Mise à jour d'un temps de trajet")
    void testUpdateTravelTime() {
        // Arrange
        Event event1 = createEvent("Event 1", parisLocation, 10, 0, 11, 0);
        Event event2 = createEvent("Event 2", lyonLocation, 15, 0, 16, 0);

        TravelTime travelTime = new TravelTime(event1, event2, testUser, 
            LocalDateTime.of(2025, 1, 15, 11, 0), 60);
        travelTime.setId(1L);

        when(travelTimeRepository.findById(1L)).thenReturn(Optional.of(travelTime));
        when(travelTimeRepository.save(any(TravelTime.class))).thenReturn(travelTime);

        LocalDateTime newStartTime = LocalDateTime.of(2025, 1, 15, 12, 0);

        // Act
        travelTimeService.updateTravelTime(1L, newStartTime);

        // Assert
        ArgumentCaptor<TravelTime> captor = ArgumentCaptor.forClass(TravelTime.class);
        verify(travelTimeRepository).save(captor.capture());
        
        TravelTime savedTravelTime = captor.getValue();
        assertEquals(newStartTime, savedTravelTime.getStartTime());
    }

    @Test
    @DisplayName("Suppression d'un temps de trajet")
    void testDeleteTravelTime() {
        // Arrange
        Long travelTimeId = 1L;

        // Act
        travelTimeService.deleteTravelTime(travelTimeId);

        // Assert
        verify(travelTimeRepository, times(1)).deleteById(travelTimeId);
    }

    // ==================== Tests SimpleTravelTimeCalculator ====================

    @Test
    @DisplayName("Calcul du temps de trajet - Mode WALKING")
    void testSimpleCalculator_Walking() {
        // Arrange - Distance Paris-Lyon ~400km
        // À pied (5 km/h) devrait donner un temps très long

        // Act
        int travelTime = calculator.calculateTravelTime(
            parisLocation, lyonLocation, TransportMode.WALKING
        );

        // Assert
        assertTrue(travelTime > 0, "Le temps de trajet doit être positif");
        // Pour ~400km à 5km/h = ~4800 minutes
        assertTrue(travelTime > 1000, "Le temps à pied doit être très long");
    }

    @Test
    @DisplayName("Calcul du temps de trajet - Mode DRIVING")
    void testSimpleCalculator_Driving() {
        // Arrange - Distance Paris-Lyon ~400km
        // En voiture (40 km/h en ville) devrait être plus rapide

        // Act
        int travelTime = calculator.calculateTravelTime(
            parisLocation, lyonLocation, TransportMode.DRIVING
        );

        // Assert
        assertTrue(travelTime > 0, "Le temps de trajet doit être positif");
        // Pour ~400km à 40km/h = ~600 minutes
        assertTrue(travelTime > 400 && travelTime < 800, 
            "Le temps en voiture devrait être autour de 600 min");
    }

    @Test
    @DisplayName("Calcul du temps de trajet - Mode CYCLING")
    void testSimpleCalculator_Cycling() {
        // Arrange - Distance Paris-Lyon ~400km

        // Act
        int travelTime = calculator.calculateTravelTime(
            parisLocation, lyonLocation, TransportMode.CYCLING
        );

        // Assert
        assertTrue(travelTime > 0);
        // Pour ~400km à 15km/h = ~1600 minutes
        assertTrue(travelTime > 1000 && travelTime < 2000);
    }

    @Test
    @DisplayName("Calcul du temps de trajet - Mode TRANSIT")
    void testSimpleCalculator_Transit() {
        // Arrange

        // Act
        int travelTime = calculator.calculateTravelTime(
            parisLocation, lyonLocation, TransportMode.TRANSIT
        );

        // Assert
        assertTrue(travelTime > 0);
        // Pour ~400km à 25km/h = ~960 minutes
        assertTrue(travelTime > 800 && travelTime < 1200);
    }

    @Test
    @DisplayName("Calcul avec courte distance - Buffer minimum de 5 minutes")
    void testSimpleCalculator_ShortDistance_MinimumBuffer() {
        // Arrange - Deux points très proches
        Location loc1 = new Location(48.8566, 2.3522);
        loc1.setName("Point A");
        Location loc2 = new Location(48.8567, 2.3523); // ~100m
        loc2.setName("Point B");

        // Act
        int travelTime = calculator.calculateTravelTime(loc1, loc2, TransportMode.DRIVING);

        // Assert
        assertEquals(5, travelTime, "Le temps minimum doit être de 5 minutes");
    }

    @Test
    @DisplayName("Calcul sans coordonnées GPS - Valeur par défaut")
    void testSimpleCalculator_NoCoordinates_DefaultValue() {
        // Arrange
        Location gareLyonParis = new Location(
            "Place Louis-Armand, 75012 Paris, France"
        );
        
        Location garePartDieu = new Location(
            "Boulevard Vivier Merle, 69003 Lyon, France"
        );

        // Act
        int travelTime = calculator.calculateTravelTime(gareLyonParis, garePartDieu, TransportMode.DRIVING);

        // Assert
        assertEquals(15, travelTime, "Sans coordonnées, doit retourner 15 minutes par défaut");
    }

    @Test
    @DisplayName("Vérification de la cohérence des vitesses entre modes")
    void testSimpleCalculator_SpeedConsistency() {
        // Arrange - Même distance

        // Act
        int walkingTime = calculator.calculateTravelTime(parisLocation, lyonLocation, TransportMode.WALKING);
        int cyclingTime = calculator.calculateTravelTime(parisLocation, lyonLocation, TransportMode.CYCLING);
        int drivingTime = calculator.calculateTravelTime(parisLocation, lyonLocation, TransportMode.DRIVING);

        // Assert - Walking doit être le plus long, Driving le plus court
        assertTrue(walkingTime > cyclingTime, "Marcher doit prendre plus de temps que le vélo");
        assertTrue(cyclingTime > drivingTime, "Le vélo doit prendre plus de temps que la voiture");
    }

    @Test
    @DisplayName("Calcul de distance avec la formule de Haversine")
    void testDistanceCalculation() {
        // Arrange
        Event event1 = createEvent("Event 1", parisLocation, 10, 0, 11, 0);
        Event event2 = createEvent("Event 2", lyonLocation, 15, 0, 16, 0);

        when(travelTimeRepository.save(any(TravelTime.class)))
            .thenAnswer(invocation -> invocation.getArgument(0));

        // Act
        TravelTime result = travelTimeService.createTravelTime(
            event1, event2, TransportMode.DRIVING
        );

        // Assert
        assertNotNull(result.getDistanceKm());
        // Distance Paris-Lyon est environ 400 km
        assertTrue(result.getDistanceKm() > 350 && result.getDistanceKm() < 450,
            "La distance Paris-Lyon devrait être autour de 400 km, obtenu: " + result.getDistanceKm());
    }

    @Test
    @DisplayName("Temps de trajet cohérent avec la distance calculée")
    void testTravelTimeConsistentWithDistance() {
        // Arrange
        Event event1 = createEvent("Event 1", parisLocation, 10, 0, 11, 0);
        Event event2 = createEvent("Event 2", marseilleLocation, 15, 0, 16, 0);

        when(travelTimeRepository.save(any(TravelTime.class)))
            .thenAnswer(invocation -> invocation.getArgument(0));

        // Act
        TravelTime result = travelTimeService.createTravelTime(
            event1, event2, TransportMode.DRIVING
        );

        // Assert
        // Distance Paris-Marseille ~660 km
        // À 40 km/h = ~990 minutes
        double expectedTime = (result.getDistanceKm() / 40.0) * 60;
        assertTrue(Math.abs(result.getDurationMinutes() - expectedTime) < 10,
            "Le temps calculé devrait être cohérent avec la distance");
    }

    // ==================== Tests TravelTime Entity ====================

    @Test
    @DisplayName("Construction d'un TravelTime avec mise à jour automatique de endTime")
    void testTravelTimeEntity_AutomaticEndTimeCalculation() {
        // Arrange
        Event event1 = createEvent("Event 1", parisLocation, 10, 0, 11, 0);
        Event event2 = createEvent("Event 2", lyonLocation, 15, 0, 16, 0);
        LocalDateTime startTime = LocalDateTime.of(2025, 1, 15, 11, 0);

        // Act
        TravelTime travelTime = new TravelTime(event1, event2, testUser, startTime, 60);

        // Assert
        assertEquals(startTime, travelTime.getStartTime());
        assertEquals(startTime.plusMinutes(60), travelTime.getEndTime());
        assertEquals(60, travelTime.getDurationMinutes());
    }

    @Test
    @DisplayName("Modification de la durée met à jour l'heure de fin")
    void testTravelTimeEntity_UpdateDuration_UpdatesEndTime() {
        // Arrange
        Event event1 = createEvent("Event 1", parisLocation, 10, 0, 11, 0);
        Event event2 = createEvent("Event 2", lyonLocation, 15, 0, 16, 0);
        LocalDateTime startTime = LocalDateTime.of(2025, 1, 15, 11, 0);
        TravelTime travelTime = new TravelTime(event1, event2, testUser, startTime, 60);

        // Act
        travelTime.setDurationMinutes(90);

        // Assert
        assertEquals(startTime.plusMinutes(90), travelTime.getEndTime());
    }

    @Test
    @DisplayName("Modification de l'heure de début met à jour l'heure de fin")
    void testTravelTimeEntity_UpdateStartTime_UpdatesEndTime() {
        // Arrange
        Event event1 = createEvent("Event 1", parisLocation, 10, 0, 11, 0);
        Event event2 = createEvent("Event 2", lyonLocation, 15, 0, 16, 0);
        LocalDateTime startTime = LocalDateTime.of(2025, 1, 15, 11, 0);
        TravelTime travelTime = new TravelTime(event1, event2, testUser, startTime, 60);

        // Act
        LocalDateTime newStartTime = LocalDateTime.of(2025, 1, 15, 12, 0);
        travelTime.setStartTime(newStartTime);

        // Assert
        assertEquals(newStartTime.plusMinutes(60), travelTime.getEndTime());
    }

    // ==================== Méthodes utilitaires ====================

    private Event createEvent(String summary, Location location, 
                             int startHour, int startMinute, 
                             int endHour, int endMinute) {
        LocalDateTime start = LocalDateTime.of(2025, 1, 15, startHour, startMinute);
        LocalDateTime end = LocalDateTime.of(2025, 1, 15, endHour, endMinute);
        
        Event event = new Event(summary, start, end, testUser);
        event.setLocation(location);
        return event;
    }
}