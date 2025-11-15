package com.example.backend.model;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests unitaires pour l'entité Location.
 */
class LocationTest {

    @Test
    @DisplayName("Constructeur avec adresse complète uniquement")
    void testConstructor_AddressOnly() {
        // Arrange & Act - Adresse complète valide
        Location location = new Location("10 Rue de Rivoli, 75001 Paris, France");

        // Assert
        assertEquals("10 Rue de Rivoli, 75001 Paris, France", location.getAddress());
        assertNull(location.getLatitude());
        assertNull(location.getLongitude());
        assertNull(location.getName());
        assertFalse(location.hasCoordinates());
    }

    @Test
    @DisplayName("Constructeur avec coordonnées GPS uniquement")
    void testConstructor_WithCoordinates() {
        // Arrange & Act
        Location location = new Location(48.8566, 2.3522);

        // Assert
        assertEquals(null, location.getName());
        assertEquals(48.8566, location.getLatitude());
        assertEquals(2.3522, location.getLongitude());
        assertNull(location.getName());
        assertTrue(location.hasCoordinates());
    }

    @Test
    @DisplayName("Validation - Adresse trop courte est rejetée")
    void testValidation_AddressTooShort() {
        // Act & Assert
        IllegalArgumentException exception = assertThrows(
            IllegalArgumentException.class,
            () -> new Location("Paris")
        );
        
        assertTrue(exception.getMessage().contains("trop courte"));
    }

    @Test
    @DisplayName("Validation - Adresse sans virgule est rejetée")
    void testValidation_AddressWithoutComma() {
        // Act & Assert
        IllegalArgumentException exception = assertThrows(
            IllegalArgumentException.class,
            () -> new Location("123 Main Street Paris")
        );
        
        assertTrue(exception.getMessage().contains("virgule"));
    }

    @Test
    @DisplayName("Validation - Adresse null est rejetée")
    void testValidation_NullAddress() {
        // Act & Assert
        IllegalArgumentException exception = assertThrows(
            IllegalArgumentException.class,
            () -> new Location(null)
        );
        
        assertTrue(exception.getMessage().contains("vide"));
    }

    @Test
    @DisplayName("Validation - Adresse vide est rejetée")
    void testValidation_EmptyAddress() {
        // Act & Assert
        IllegalArgumentException exception = assertThrows(
            IllegalArgumentException.class,
            () -> new Location("   ")
        );
        
        assertTrue(exception.getMessage().contains("vide"));
    }

    @Test
    @DisplayName("Validation - Latitude invalide (> 90)")
    void testValidation_LatitudeTooHigh() {
        // Act & Assert
        IllegalArgumentException exception = assertThrows(
            IllegalArgumentException.class,
            () -> new Location(100.0, 2.3522)
        );
        
        assertTrue(exception.getMessage().contains("Latitude invalide"));
    }

    @Test
    @DisplayName("Validation - Latitude invalide (< -90)")
    void testValidation_LatitudeTooLow() {
        // Act & Assert
        IllegalArgumentException exception = assertThrows(
            IllegalArgumentException.class,
            () -> new Location(-100.0, 2.3522)
        );
        
        assertTrue(exception.getMessage().contains("Latitude invalide"));
    }

    @Test
    @DisplayName("Validation - Longitude invalide (> 180)")
    void testValidation_LongitudeTooHigh() {
        // Act & Assert
        IllegalArgumentException exception = assertThrows(
            IllegalArgumentException.class,
            () -> new Location(48.8566, 200.0)
        );
        
        assertTrue(exception.getMessage().contains("Longitude invalide"));
    }

    @Test
    @DisplayName("Validation - Longitude invalide (< -180)")
    void testValidation_LongitudeTooLow() {
        // Act & Assert
        IllegalArgumentException exception = assertThrows(
            IllegalArgumentException.class,
            () -> new Location(48.8566, -200.0)
        );
        
        assertTrue(exception.getMessage().contains("Longitude invalide"));
    }

    @Test
    @DisplayName("Méthode hasCoordinates retourne false si latitude est nulle")
    void testHasCoordinates_NullLatitude() {
        // Arrange
        Location location = new Location("Paris, France");
        location.setLongitude(2.3522);

        // Act & Assert
        assertFalse(location.hasCoordinates());
    }

    @Test
    @DisplayName("Méthode hasCoordinates retourne false si longitude est nulle")
    void testHasCoordinates_NullLongitude() {
        // Arrange
        Location location = new Location("Paris, France");
        location.setLatitude(48.8566);

        // Act & Assert
        assertFalse(location.hasCoordinates());
    }

    @Test
    @DisplayName("Méthode hasCoordinates retourne true si les deux coordonnées sont définies")
    void testHasCoordinates_BothCoordinatesSet() {
        // Arrange
        Location location = new Location("Paris, France");
        location.setLatitude(48.8566);
        location.setLongitude(2.3522);

        // Act & Assert
        assertTrue(location.hasCoordinates());
    }

    @Test
    @DisplayName("Setter et getter pour le nom de la localisation")
    void testNameSetterAndGetter() {
        // Arrange
        Location location = new Location("10 Rue de Rivoli, 75001 Paris, France");

        // Act
        location.setName("Bureau Principal");

        // Assert
        assertEquals("Bureau Principal", location.getName());
    }

    @Test
    @DisplayName("Modification des coordonnées via setters")
    void testCoordinatesSetters() {
        // Arrange
        Location location = new Location("Lyon, France");

        // Act
        location.setLatitude(45.7640);
        location.setLongitude(4.8357);

        // Assert
        assertEquals(45.7640, location.getLatitude());
        assertEquals(4.8357, location.getLongitude());
        assertTrue(location.hasCoordinates());
    }

    @Test
    @DisplayName("L'ID est null avant la persistance")
    void testId_NullBeforePersistence() {
        // Arrange & Act
        Location location = new Location(48.8566, 2.3522);

        // Assert
        assertNull(location.getId());
    }

    @Test
    @DisplayName("Setter d'ID fonctionne correctement")
    void testIdSetter() {
        // Arrange
        Location location = new Location("Paris, France");

        // Act
        location.setId(123L);

        // Assert
        assertEquals(123L, location.getId());
    }

    @Test
    @DisplayName("Coordonnées négatives sont acceptées")
    void testNegativeCoordinates() {
        // Arrange & Act - Buenos Aires, Argentine
        Location location = new Location(-34.6037, -58.3816);
        location.setAddress("Buenos Aires, Argentina");

        // Assert
        assertEquals(-34.6037, location.getLatitude());
        assertEquals(-58.3816, location.getLongitude());
        assertTrue(location.hasCoordinates());
    }

    @Test
    @DisplayName("Coordonnées aux limites géographiques")
    void testCoordinatesAtGeographicLimits() {
        // Arrange & Act - Pôle Nord
        Location northPole = new Location(90.0, 0.0);
        northPole.setAddress("North Pole, Arctic");
        
        // Équateur sur le méridien de Greenwich
        Location equator = new Location(0.0, 0.0);
        equator.setAddress("Equator, Atlantic Ocean");
        
        // Point à -180° de longitude
        Location westLimit = new Location(0.0, -180.0);
        westLimit.setAddress("International Date Line, Pacific");

        // Assert
        assertTrue(northPole.hasCoordinates());
        assertTrue(equator.hasCoordinates());
        assertTrue(westLimit.hasCoordinates());
        
        assertEquals(90.0, northPole.getLatitude());
        assertEquals(0.0, equator.getLatitude());
        assertEquals(-180.0, westLimit.getLongitude());
    }

    @Test
    @DisplayName("getDisplayName retourne le nom si défini")
    void testGetDisplayName_WithName() {
        // Arrange
        Location location = new Location("5 Avenue Anatole France, 75007 Paris, France");
        location.setName("Tour Eiffel");

        // Act & Assert
        assertEquals("Tour Eiffel", location.getDisplayName());
    }

    @Test
    @DisplayName("getDisplayName retourne l'adresse courte si pas de nom")
    void testGetDisplayName_WithoutName() {
        // Arrange
        Location location = new Location("Place Bellecour, 69002 Lyon, France");

        // Act & Assert
        assertEquals("Place Bellecour", location.getDisplayName());
    }

    @Test
    @DisplayName("getFullDescription contient toutes les informations")
    void testGetFullDescription_Complete() {
        // Arrange
        Location location = new Location(48.8584, 2.2945);
        location.setAddress("5 Avenue Anatole France, 75007 Paris, France");
        location.setName("Tour Eiffel");

        // Act
        String description = location.getFullDescription();

        // Assert
        assertTrue(description.contains("Tour Eiffel"));
        assertTrue(description.contains("5 Avenue Anatole France"));
        assertTrue(description.contains("48.8584"));
        assertTrue(description.contains("2.2945"));
    }

    @Test
    @DisplayName("getFullDescription avec GPS uniquement")
    void testGetFullDescription_WithGPSOnly() {
        // Arrange
        Location location = new Location(45.7579, 4.8319);
        location.setName("Place Bellecour");

        // Act
        String description = location.getFullDescription();

        // Assert
        assertTrue(description.contains("Place Bellecour"));
        assertTrue(description.contains("GPS:"));
        assertTrue(description.contains("45.7579"));
    }

    @Test
    @DisplayName("getFullDescription sans coordonnées")
    void testGetFullDescription_WithoutCoordinates() {
        // Arrange
        Location location = new Location("Marseille, France");
        location.setName("Vieux Port");

        // Act
        String description = location.getFullDescription();

        // Assert
        assertTrue(description.contains("Vieux Port"));
        assertTrue(description.contains("Marseille"));
        assertFalse(description.contains("GPS"));
    }

    @Test
    @DisplayName("toString contient les informations principales")
    void testToString() {
        // Arrange
        Location location = new Location(48.8566, 2.3522);
        location.setName("Paris Centre");
        location.setId(42L);

        // Act
        String result = location.toString();

        // Assert
        assertTrue(result.contains("42"));
        assertTrue(result.contains("Paris Centre"));
        assertTrue(result.contains("48.8566"));
        assertTrue(result.contains("2.3522"));
    }

    @Test
    @DisplayName("Modification de l'adresse via setter valide la nouvelle adresse")
    void testSetAddress_ValidatesNewAddress() {
        // Arrange
        Location location = new Location("Paris, France");

        // Act & Assert - Adresse valide
        location.setAddress("Lyon, France");
        assertEquals("Lyon, France", location.getAddress());

        // Act & Assert - Adresse invalide
        assertThrows(IllegalArgumentException.class, () -> {
            location.setAddress("Invalid");
        });
    }

    @Test
    @DisplayName("Modification de latitude via setter valide la coordonnée")
    void testSetLatitude_ValidatesCoordinate() {
        // Arrange
        Location location = new Location(48.8566, 2.3522);

        // Act & Assert - Latitude valide
        location.setLatitude(45.7640);
        assertEquals(45.7640, location.getLatitude());

        // Act & Assert - Latitude invalide
        assertThrows(IllegalArgumentException.class, () -> {
            location.setLatitude(100.0);
        });
    }

    @Test
    @DisplayName("Modification de longitude via setter valide la coordonnée")
    void testSetLongitude_ValidatesCoordinate() {
        // Arrange
        Location location = new Location(48.8566, 2.3522);

        // Act & Assert - Longitude valide
        location.setLongitude(4.8357);
        assertEquals(4.8357, location.getLongitude());

        // Act & Assert - Longitude invalide
        assertThrows(IllegalArgumentException.class, () -> {
            location.setLongitude(200.0);
        });
    }

    @Test
    @DisplayName("Créer une location avec GPS puis ajouter adresse et nom")
    void testConstructor_GPSThenAddressAndName() {
        // Arrange & Act
        Location location = new Location(48.8584, 2.2945);
        location.setAddress("5 Avenue Anatole France, 75007 Paris, France");
        location.setName("Tour Eiffel");

        // Assert
        assertEquals("5 Avenue Anatole France, 75007 Paris, France", location.getAddress());
        assertEquals(48.8584, location.getLatitude());
        assertEquals(2.2945, location.getLongitude());
        assertEquals("Tour Eiffel", location.getName());
        assertTrue(location.hasCoordinates());
    }
}