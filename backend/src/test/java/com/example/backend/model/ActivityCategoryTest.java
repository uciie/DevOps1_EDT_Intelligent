package com.example.backend.model;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class ActivityCategoryTest {

    @Test
    void testAllEnumValues() {
        // Vérifier que tous les valeurs de l'enum sont présentes
        ActivityCategory[] categories = ActivityCategory.values();
        
        assertEquals(8, categories.length);
        assertTrue(java.util.Arrays.asList(categories).contains(ActivityCategory.TRAVAIL));
        assertTrue(java.util.Arrays.asList(categories).contains(ActivityCategory.ETUDE));
        assertTrue(java.util.Arrays.asList(categories).contains(ActivityCategory.SPORT));
        assertTrue(java.util.Arrays.asList(categories).contains(ActivityCategory.LOISIR));
        assertTrue(java.util.Arrays.asList(categories).contains(ActivityCategory.MENAGER));
        assertTrue(java.util.Arrays.asList(categories).contains(ActivityCategory.RENCONTRE));
        assertTrue(java.util.Arrays.asList(categories).contains(ActivityCategory.AUTRE));
        assertTrue(java.util.Arrays.asList(categories).contains(ActivityCategory.FOCUS));
    }

    @Test
    void testValueOf() {
        // Test de conversion depuis String
        assertEquals(ActivityCategory.TRAVAIL, ActivityCategory.valueOf("TRAVAIL"));
        assertEquals(ActivityCategory.ETUDE, ActivityCategory.valueOf("ETUDE"));
        assertEquals(ActivityCategory.SPORT, ActivityCategory.valueOf("SPORT"));
        assertEquals(ActivityCategory.LOISIR, ActivityCategory.valueOf("LOISIR"));
        assertEquals(ActivityCategory.MENAGER, ActivityCategory.valueOf("MENAGER"));
        assertEquals(ActivityCategory.RENCONTRE, ActivityCategory.valueOf("RENCONTRE"));
        assertEquals(ActivityCategory.AUTRE, ActivityCategory.valueOf("AUTRE"));
    }

    @Test
    void testValueOf_InvalidValue() {
        // Test avec une valeur invalide
        assertThrows(IllegalArgumentException.class, () -> {
            ActivityCategory.valueOf("INVALID");
        });
    }

    @Test
    void testEnumEquality() {
        // Test d'égalité entre valeurs enum
        ActivityCategory cat1 = ActivityCategory.TRAVAIL;
        ActivityCategory cat2 = ActivityCategory.TRAVAIL;
        ActivityCategory cat3 = ActivityCategory.SPORT;

        assertEquals(cat1, cat2);
        assertNotEquals(cat1, cat3);
        assertSame(cat1, cat2);
    }

    @Test
    void testEnumToString() {
        // Test de la méthode toString() implicite
        assertEquals("TRAVAIL", ActivityCategory.TRAVAIL.toString());
        assertEquals("ETUDE", ActivityCategory.ETUDE.toString());
        assertEquals("SPORT", ActivityCategory.SPORT.toString());
        assertEquals("LOISIR", ActivityCategory.LOISIR.toString());
        assertEquals("MENAGER", ActivityCategory.MENAGER.toString());
        assertEquals("RENCONTRE", ActivityCategory.RENCONTRE.toString());
        assertEquals("AUTRE", ActivityCategory.AUTRE.toString());
    }

    @Test
    void testEnumOrdinal() {
        // Test de l'ordre des valeurs enum
        ActivityCategory[] categories = ActivityCategory.values();
        
        // Vérifier que l'ordre est cohérent
        assertTrue(ActivityCategory.TRAVAIL.ordinal() < ActivityCategory.ETUDE.ordinal());
        assertTrue(ActivityCategory.ETUDE.ordinal() < ActivityCategory.SPORT.ordinal());
    }
}
