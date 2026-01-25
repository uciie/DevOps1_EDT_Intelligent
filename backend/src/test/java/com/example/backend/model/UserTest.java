package com.example.backend.model;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

import java.util.List;

/**
 * Tests unitaires pour la classe User.
 * Ces tests vérifient les constructeurs, les accesseurs (getters/setters)
 * et les relations entre User, Event et Task.
 */
class UserTest {

    /**
     * Teste le constructeur avec paramètres et les getters.
     * Vérifie que les valeurs sont correctement affectées
     * et que les listes d'événements et de tâches sont nulles avant initialisation.
     */
    @Test
    void testUserConstructorAndGetters() {
        // Création d'un utilisateur avec nom d'utilisateur et mot de passe
        User user = new User("TEST", "TEST");

        // Vérification des valeurs initiales
        assertNull(user.getId()); // L'ID est géré automatiquement par JPA (doit être null avant persistance)
        assertEquals("TEST", user.getUsername());
        assertEquals("TEST", user.getPassword());
        assertNull(user.getEvents()); // Pas encore d'événements associés
        assertNull(user.getTasks());  // Pas encore de tâches associées
    }

    /**
     * Teste les setters et getters classiques de la classe User.
     * Vérifie que chaque propriété peut être définie et lue correctement.
     */
    @Test
    void testUserSetters() {
        // Création d'un utilisateur vide
        User user = new User();

        // Affectation des valeurs à l'aide des setters
        user.setId(1L);
        user.setUsername("bob");
        user.setPassword("1234");

        // Vérification des valeurs récupérées par les getters
        assertEquals(1L, user.getId());
        assertEquals("bob", user.getUsername());
        assertEquals("1234", user.getPassword());
    }

    /**
     * Teste les relations entre User, Event et Task.
     * Vérifie que les événements et tâches associés à un utilisateur
     * sont correctement stockés et accessibles.
     */
    @Test
    void testUserRelations() {
        // Création d'un utilisateur
        User user = new User("john", "pass");

        // Création d'un événement et d'une tâche liés à cet utilisateur
        Event e = new Event("Meeting", java.time.LocalDateTime.now(),
                            java.time.LocalDateTime.now().plusHours(1), user);
        Task t = new Task("Task A", 60, 3, Task.TaskStatus.PENDING_CREATION, user, e);

        // Association des événements et tâches à l'utilisateur
        user.setEvents(List.of(e));
        user.setTasks(List.of(t));

        // Vérifications de la cohérence des relations
        assertEquals(1, user.getEvents().size());  // L'utilisateur a 1 événement
        assertEquals(1, user.getTasks().size());   // L'utilisateur a 1 tâche
        assertEquals(e, user.getEvents().getFirst()); // Vérifie que c'est bien le bon événement
        assertEquals(t, user.getTasks().getFirst());  // Vérifie que c'est bien la bonne tâche
    }
}