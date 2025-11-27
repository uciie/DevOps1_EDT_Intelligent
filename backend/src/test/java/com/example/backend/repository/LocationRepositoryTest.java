package com.example.backend.repository;

import com.example.backend.model.Location;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests d'intégration pour LocationRepository.
 * Utilise une base de données H2 en mémoire pour tester les opérations CRUD.
 */
@DataJpaTest
class LocationRepositoryTest {

    @Autowired
    private TestEntityManager entityManager;

    @Autowired
    private LocationRepository locationRepository;

    @Test
    void testSaveLocation() {
        // Arrange - Adresse complète valide
        Location location = new Location(48.8566, 2.3522);
        location.setAddress("Paris, France");
        location.setName("Paris Centre");

        // Act
        Location savedLocation = locationRepository.save(location);

        // Assert
        assertNotNull(savedLocation.getId());
        assertEquals("Paris Centre", savedLocation.getName());
        assertEquals(48.8566, savedLocation.getLatitude());
        assertEquals(2.3522, savedLocation.getLongitude());
    }

    @Test
    void testFindByAddress() {
        // Arrange - Adresse complète valide
        Location location = new Location(45.7640, 4.8357);
        location.setAddress("Lyon, France");
        entityManager.persist(location);
        entityManager.flush();

        // Act
        Optional<Location> foundLocation = locationRepository.findByAddress("Lyon, France");

        // Assert
        assertTrue(foundLocation.isPresent());
        assertEquals("Lyon, France", foundLocation.get().getAddress());
        assertEquals(45.7640, foundLocation.get().getLatitude());
    }

    @Test
    void testFindByAddress_NotFound() {
        // Act
        Optional<Location> foundLocation = locationRepository.findByAddress("Non Existent Address, Country");

        // Assert
        assertFalse(foundLocation.isPresent());
    }

    @Test
    void testFindByName() {
        // Arrange - Adresse complète avec nom
        Location location = new Location(40.7128, -74.0060);
        location.setAddress("123 Main Street, New York, USA");
        location.setName("Office HQ");
        entityManager.persist(location);
        entityManager.flush();

        // Act
        Optional<Location> foundLocation = locationRepository.findByName("Office HQ");

        // Assert
        assertTrue(foundLocation.isPresent());
        assertEquals("Office HQ", foundLocation.get().getName());
        assertEquals("123 Main Street, New York, USA", foundLocation.get().getAddress());
    }

    @Test
    void testFindByName_NotFound() {
        // Act
        Optional<Location> foundLocation = locationRepository.findByName("Non Existent Name");

        // Assert
        assertFalse(foundLocation.isPresent());
    }

    @Test
    void testSaveLocationWithoutCoordinates() {
        // Arrange - Adresse valide sans coordonnées
        Location location = new Location("Tokyo, Japan");

        // Act
        Location savedLocation = locationRepository.save(location);

        // Assert
        assertNotNull(savedLocation.getId());
        assertEquals("Tokyo, Japan", savedLocation.getAddress());
        assertNull(savedLocation.getLatitude());
        assertNull(savedLocation.getLongitude());
        assertFalse(savedLocation.hasCoordinates());
    }

    @Test
    void testSaveLocationWithName() {
        // Arrange - Adresse complète avec nom
        Location location = new Location(34.0522, -118.2437);
        location.setAddress("456 Oak Avenue, Los Angeles, USA");
        location.setName("Home");

        // Act
        Location savedLocation = locationRepository.save(location);

        // Assert
        assertNotNull(savedLocation.getId());
        assertEquals("Home", savedLocation.getName());
        assertTrue(savedLocation.hasCoordinates());
    }

    @Test
    void testFindAll() {
        // Arrange - Adresses complètes
        Location location1 = new Location(48.8566, 2.3522);
        location1.setAddress("Address 1, City 1, Country");
        Location location2 = new Location(45.7640, 4.8357);
        location2.setAddress("Address 2, City 2, Country");
        entityManager.persist(location1);
        entityManager.persist(location2);
        entityManager.flush();

        // Act
        List<Location> locations = locationRepository.findAll();

        // Assert
        assertTrue(locations.size() >= 2);
        assertTrue(locations.stream().anyMatch(l -> l.getAddress().equals("Address 1, City 1, Country")));
        assertTrue(locations.stream().anyMatch(l -> l.getAddress().equals("Address 2, City 2, Country")));
    }

    @Test
    void testUpdateLocation() {
        // Arrange - Adresse complète initiale
        Location location = new Location(48.8566, 2.3522);
        location.setAddress("Old Address, City, Country");
        entityManager.persist(location);
        entityManager.flush();

        // Act - Mise à jour avec nouvelle adresse complète
        location.setAddress("New Address, City, Country");
        location.setLatitude(45.7640);
        location.setLongitude(4.8357);
        location.setName("Updated Name");
        Location updatedLocation = locationRepository.save(location);

        // Assert
        assertEquals("New Address, City, Country", updatedLocation.getAddress());
        assertEquals(45.7640, updatedLocation.getLatitude());
        assertEquals(4.8357, updatedLocation.getLongitude());
        assertEquals("Updated Name", updatedLocation.getName());
    }

    @Test
    void testDeleteLocation() {
        // Arrange
        Location location = new Location(48.8566, 2.3522);
        location.setAddress("To Delete, City, Country");
        entityManager.persist(location);
        entityManager.flush();

        Long locationId = location.getId();

        // Act
        locationRepository.deleteById(locationId);
        entityManager.flush();

        // Assert
        Optional<Location> deletedLocation = locationRepository.findById(locationId);
        assertFalse(deletedLocation.isPresent());
    }

    @Test
    void testFindById() {
        // Arrange
        Location location = new Location(48.8566, 2.3522);
        location.setAddress("Find Me, City, Country");
        entityManager.persist(location);
        entityManager.flush();

        // Act
        Optional<Location> foundLocation = locationRepository.findById(location.getId());

        // Assert
        assertTrue(foundLocation.isPresent());
        assertEquals("Find Me, City, Country", foundLocation.get().getAddress());
    }

    @Test
    void testFindById_NotFound() {
        // Act
        Optional<Location> foundLocation = locationRepository.findById(999L);

        // Assert
        assertFalse(foundLocation.isPresent());
    }

    @Test
    void testCount() {
        // Arrange
        long initialCount = locationRepository.count();

        Location location = new Location(48.8566, 2.3522);
        location.setAddress("Count Test, City, Country");
        entityManager.persist(location);
        entityManager.flush();

        // Act
        long newCount = locationRepository.count();

        // Assert
        assertEquals(initialCount + 1, newCount);
    }

    @Test
    void testExistsById() {
        // Arrange
        Location location = new Location(48.8566, 2.3522);
        location.setAddress("Exists Test, City, Country");
        entityManager.persist(location);
        entityManager.flush();

        // Act
        boolean exists = locationRepository.existsById(location.getId());
        boolean notExists = locationRepository.existsById(999L);

        // Assert
        assertTrue(exists);
        assertFalse(notExists);
    }

    @Test
    void testFindByAddress_CaseSensitive() {
        // Arrange
        Location location = new Location(48.8566, 2.3522);
        location.setAddress("Paris, France");
        entityManager.persist(location);
        entityManager.flush();

        // Act
        Optional<Location> found = locationRepository.findByAddress("Paris, France");
        Optional<Location> notFound = locationRepository.findByAddress("paris, france");

        // Assert
        assertTrue(found.isPresent());
        assertFalse(notFound.isPresent());
    }

    @Test
    void testMultipleLocationsWithSameNameDifferentAddress() {
        // Arrange - Adresses complètes avec même nom
        Location location1 = new Location(40.7128, -74.0060);
        location1.setAddress("123 Main Street, New York, USA");
        location1.setName("Office");
        
        Location location2 = new Location(34.0522, -118.2437);
        location2.setAddress("456 Oak Avenue, Los Angeles, USA");
        location2.setName("Office");

        entityManager.persist(location1);
        entityManager.persist(location2);
        entityManager.flush();

        // Act
        Optional<Location> foundLocation = locationRepository.findByName("Office");

        // Assert
        assertTrue(foundLocation.isPresent());
        assertEquals("Office", foundLocation.get().getName());
        // Note: findByName retourne le premier trouvé, pas tous
    }

    @Test
    void testSaveLocationWithCompleteAddress() {
        // Arrange - Test avec adresse très complète
        Location location = new Location(48.8584, 2.2945);
        location.setAddress("5 Avenue Anatole France, 75007 Paris, France");
        location.setName("Tour Eiffel");

        // Act
        Location savedLocation = locationRepository.save(location);

        // Assert
        assertNotNull(savedLocation.getId());
        assertEquals("Tour Eiffel", savedLocation.getName());
        assertEquals("5 Avenue Anatole France, 75007 Paris, France", savedLocation.getAddress());
        assertTrue(savedLocation.hasCoordinates());
    }

    @Test
    void testFindByAddress_WithSpecialCharacters() {
        // Arrange - Adresse avec caractères spéciaux
        Location location = new Location(48.8049, 2.1204);
        location.setAddress("Château de Versailles, 78000 Versailles, France");
        entityManager.persist(location);
        entityManager.flush();

        // Act
        Optional<Location> foundLocation = locationRepository.findByAddress("Château de Versailles, 78000 Versailles, France");

        // Assert
        assertTrue(foundLocation.isPresent());
        assertEquals("Château de Versailles, 78000 Versailles, France", foundLocation.get().getAddress());
    }
}