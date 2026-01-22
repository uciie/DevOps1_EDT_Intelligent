package com.example.backend.dto;

import com.example.backend.model.ActivityCategory;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class ActivityStatsDTOTest {

    @Test
    void testConstructorAndGetters() {
        // Arrange & Act
        ActivityStatsDTO dto = new ActivityStatsDTO(ActivityCategory.TRAVAIL, 5, 300);

        // Assert
        assertEquals(ActivityCategory.TRAVAIL, dto.getCategory());
        assertEquals(5, dto.getCount());
        assertEquals(300, dto.getTotalMinutes());
        assertEquals(60, dto.getAverageMinutes()); // 300 / 5 = 60
    }

    @Test
    void testAverageMinutesCalculation() {
        // Arrange & Act
        ActivityStatsDTO dto = new ActivityStatsDTO(ActivityCategory.SPORT, 3, 90);

        // Assert
        assertEquals(30, dto.getAverageMinutes()); // 90 / 3 = 30
    }

    @Test
    void testAverageMinutesWithZeroCount() {
        // Arrange & Act
        ActivityStatsDTO dto = new ActivityStatsDTO(ActivityCategory.LOISIR, 0, 0);

        // Assert
        assertEquals(0, dto.getAverageMinutes()); // Division par zéro protégée
    }

    @Test
    void testAverageMinutesWithSingleSession() {
        // Arrange & Act
        ActivityStatsDTO dto = new ActivityStatsDTO(ActivityCategory.ETUDE, 1, 120);

        // Assert
        assertEquals(120, dto.getAverageMinutes()); // 120 / 1 = 120
    }

    @Test
    void testAllActivityCategories() {
        // Test avec toutes les catégories
        for (ActivityCategory category : ActivityCategory.values()) {
            ActivityStatsDTO dto = new ActivityStatsDTO(category, 2, 60);
            assertEquals(category, dto.getCategory());
            assertEquals(30, dto.getAverageMinutes());
        }
    }

    @Test
    void testLargeValues() {
        // Arrange & Act
        ActivityStatsDTO dto = new ActivityStatsDTO(ActivityCategory.MENAGER, 100, 5000);

        // Assert
        assertEquals(100, dto.getCount());
        assertEquals(5000, dto.getTotalMinutes());
        assertEquals(50, dto.getAverageMinutes()); // 5000 / 100 = 50
    }

    @Test
    void testRoundingBehavior() {
        // Test avec division non exacte
        ActivityStatsDTO dto = new ActivityStatsDTO(ActivityCategory.RENCONTRE, 3, 100);

        // Assert - La division entière donne 33 (100 / 3 = 33.33...)
        assertEquals(33, dto.getAverageMinutes());
    }
}
