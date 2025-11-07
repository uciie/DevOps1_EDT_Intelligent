package com.example.backend;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationContext;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests pour l'application Backend.
 * Vérifie que le contexte Spring se charge correctement.
 */
@SpringBootTest
class BackendApplicationTests {
	@Autowired
    private ApplicationContext applicationContext;

    @Test
    void contextLoads() {
        assertNotNull(applicationContext);
    }

    @Test
    void testApplicationContextHasRequiredBeans() {
        // Vérifier que les beans principaux sont chargés
        assertTrue(applicationContext.containsBean("userService"));
        assertTrue(applicationContext.containsBean("calendarImportService"));
        assertTrue(applicationContext.containsBean("defaultScheduleOptimizerService"));
    }

    @Test
    void testApplicationContextHasRepositories() {
        // Vérifier que les repositories sont chargés
        assertTrue(applicationContext.containsBean("userRepository"));
        assertTrue(applicationContext.containsBean("eventRepository"));
        assertTrue(applicationContext.containsBean("taskRepository"));
        assertTrue(applicationContext.containsBean("travelTimeRepository"));
        assertTrue(applicationContext.containsBean("locationRepository"));
    }

    @Test
    void testApplicationContextHasControllers() {
        // Vérifier que les contrôleurs sont chargés
        assertTrue(applicationContext.containsBean("userController"));
        assertTrue(applicationContext.containsBean("scheduleController"));
        assertTrue(applicationContext.containsBean("calendarImportController"));
    }
}
