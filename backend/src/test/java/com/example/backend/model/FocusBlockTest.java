package com.example.backend.model;

import org.junit.jupiter.api.Test;
import java.time.LocalDateTime;
import static org.junit.jupiter.api.Assertions.*;

class FocusBlockTest {

    @Test
    void testFocusBlockProperties() {
        // Given
        User user = new User("Paul", "pass");
        LocalDateTime start = LocalDateTime.now();
        LocalDateTime end = start.plusHours(2);
        
        // When - Test du constructeur paramétré
        FocusBlock block = new FocusBlock(user, start, end);
        block.setId(1L);
        block.setFlexibility(5);
        block.setProtected(true);
        block.setStatus("CANCELLED");
        block.setSuggestedBySystem(true);

        // Then
        assertEquals(1L, block.getId());
        assertEquals(user, block.getUser());
        assertEquals(start, block.getStartTime());
        assertEquals(end, block.getEndTime());
        assertEquals(5, block.getFlexibility());
        assertTrue(block.isProtected());
        assertEquals("CANCELLED", block.getStatus());
        assertTrue(block.isSuggestedBySystem());
    }

    @Test
    void testDefaultValues() {
        // When
        FocusBlock block = new FocusBlock();
        
        // Then - Vérification de la valeur par défaut définie dans la classe
        assertEquals("ACTIVE", block.getStatus());
    }
}
