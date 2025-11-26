package com.example.backend.model;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

import java.time.LocalDateTime;

/**
 * Classe de test unitaire pour l'entité Task.
 * Elle vérifie que les constructeurs, accesseurs (getters/setters)
 * et méthodes utilitaires fonctionnent correctement.
 */
class TaskTest {

    /**
     * Teste le constructeur complet de Task ainsi que tous ses getters.
     * Ce test s'assure que les valeurs passées au constructeur
     * sont bien stockées et retournées correctement.
     */
    @Test
    void testTaskConstructorAndGetters() {
        // Création d'un utilisateur (nécessaire car Task est lié à User)
        User user = new User("john_doe", "password123");

        // Création d'un événement associé à cet utilisateur
        Event event = new Event("Test Event",
                LocalDateTime.now(),
                LocalDateTime.now().plusHours(1),
                user);

        // Création d'une tâche liée à l'utilisateur et à l'événement
        Task task = new Task("Test Task", 60, 1, false, user, event);

        // L'identifiant n'est pas encore généré (car non persisté)
        assertNull(task.getId());

        // Vérification des attributs du constructeur
        assertEquals("Test Task", task.getTitle());
        assertEquals(60, task.getEstimatedDuration());
        assertEquals(1, task.getPriority());
        assertFalse(task.isDone()); // Tâche non terminée
        assertEquals(user.getId(), task.getUserId());
        assertEquals(event.getId(), task.getEventId());

    }

    /**
     * Teste les setters de la classe Task.
     * Ce test crée une tâche vide et assigne des valeurs avec les setters,
     * puis vérifie qu'elles ont bien été enregistrées.
     */
    @Test
    void testTaskSetters() {
        // Création d'un utilisateur pour le lien de tâche
        User user = new User("alice", "password");

        // Création d'un nouvel événement
        Event event = new Event("New Event",
                LocalDateTime.now(),
                LocalDateTime.now().plusHours(2),
                user);

        // Création d'une tâche vide (via constructeur par défaut)
        Task task = new Task();

        // Affectation des valeurs via les setters
        task.setTitle("Updated Task");
        task.setEstimatedDuration(90);
        task.setPriority(2);
        task.setDone(true);
        task.setUser(user);
        task.setEvent(event);

        // Vérifications des valeurs stockées
        assertEquals("Updated Task", task.getTitle());
        assertEquals(90, task.getEstimatedDuration());
        assertEquals(2, task.getPriority());
        assertTrue(task.isDone());
        assertEquals(user.getId(), task.getUserId());
        assertEquals(event.getId(), task.getEventId());
    }
}