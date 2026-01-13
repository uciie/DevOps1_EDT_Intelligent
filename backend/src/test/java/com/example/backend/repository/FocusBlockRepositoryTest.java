package com.example.backend.repository;

import com.example.backend.model.FocusBlock;
import com.example.backend.model.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;

import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@DataJpaTest
public class FocusBlockRepositoryTest {

    @Autowired
    private FocusBlockRepository focusBlockRepository;

    @Autowired
    private UserRepository userRepository;

    private User user;

    @BeforeEach
    void setUp() {
        user = new User("Paul", "password");
        user = userRepository.save(user);
    }

    @Test
void testSaveAndFindByUserAndTimeRange() {
    // 1. GIVEN : On utilise une date fixe pour éviter les problèmes de précision
    LocalDateTime start = LocalDateTime.of(2026, 1, 13, 10, 0);
    LocalDateTime end = start.plusHours(2);
    
    FocusBlock block = new FocusBlock();
    block.setUser(user);
    block.setStartTime(start);
    block.setEndTime(end);
    block.setStatus("ACTIVE");
    
    // On utilise saveAndFlush pour forcer l'écriture immédiate
    focusBlockRepository.saveAndFlush(block);

    // 2. WHEN : On définit une plage large (toute la journée)
    LocalDateTime searchStart = start.toLocalDate().atStartOfDay(); // 00:00:00
    LocalDateTime searchEnd = start.toLocalDate().atTime(23, 59, 59); // 23:59:59
    
    // Appel de la méthode avec l'underscore (si tu l'as renommé comme au point 1)
    List<FocusBlock> results = focusBlockRepository.findByUser_IdAndStartTimeBetween(
        user.getId(), searchStart, searchEnd
    );

    // 3. THEN
    // Si ça échoue encore, on imprime pour debugger
    if (results.isEmpty()) {
        System.out.println("DEBUG - User ID recherché: " + user.getId());
        System.out.println("DEBUG - Total en base: " + focusBlockRepository.count());
    }

    assertFalse(results.isEmpty(), "La liste ne devrait pas être vide !");
    assertEquals(1, results.size());
    assertEquals(user.getId(), results.get(0).getUser().getId());
}

    @Test
    void testNoBlocksInTimeRange() {
        // GIVEN : Un bloc demain
        LocalDateTime tomorrow = LocalDateTime.now().plusDays(1);
        // FocusBlock block = new FocusBlock(user, tomorrow, tomorrow.plusHours(1));
        // focusBlockRepository.save(block);

        // WHEN : On cherche aujourd'hui
        List<FocusBlock> results = focusBlockRepository.findByUser_IdAndStartTimeBetween(
            user.getId(), LocalDateTime.now().minusHours(1), LocalDateTime.now()
        );

        // THEN
        assertTrue(results.isEmpty());
    }
}