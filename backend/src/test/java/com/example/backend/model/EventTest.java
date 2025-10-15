package com.example.backend.model;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Tests unitaires pour la classe Event.
 * Vérifie le bon fonctionnement des constructeurs, getters/setters
 * et des relations entre Event, User et Task.
 */
class EventTest {

    /**
     * Teste le constructeur principal de Event ainsi que ses getters.
     * Vérifie que les valeurs sont correctement initialisées et que l'ID est nul avant la persistance.
     */
    @Test
    void testEventConstructorAndGetters() {
        // Création d'un utilisateur associé à l'événement
        User user = new User("alice", "password");
        // Création d'une plage horaire
        LocalDateTime start = LocalDateTime.now();
        LocalDateTime end = start.plusHours(2);

        // Création d'un événement
        Event event = new Event("Project meeting", start, end, user);

        // Vérifications des valeurs du constructeur
        assertNull(event.getId()); // L'ID doit être null avant la sauvegarde JPA
        assertEquals("Project meeting", event.getSummary());
        assertEquals(start, event.getStartTime());
        assertEquals(end, event.getEndTime());
        assertEquals(user, event.getUser());
        assertEquals("PLANNED", event.getStatus()); // Statut par défaut
    }

    /**
     * Teste les méthodes alias (constructeur alternatif) sans utilisateur.
     * Vérifie que les valeurs passées sont bien affectées.
     */
    @Test
    void testEventAliasMethods() {
        LocalDateTime start = LocalDateTime.now();
        LocalDateTime end = start.plusHours(1);

        // Utilisation du constructeur simplifié sans user
        Event event = new Event("Alias test", start, end);

        // Vérification des attributs
        assertEquals("Alias test", event.getSummary());
        assertEquals(start, event.getStartTime());
        assertEquals(end, event.getEndTime());
    }

    /**
     * Teste la relation entre Event, User et Task.
     * Vérifie que les tâches liées à un événement sont bien associées
     * et que le statut peut être modifié correctement.
     */
    @Test
    void testEventRelationsAndStatus() {
        // Création de l'utilisateur et de l'événement
        User user = new User("bob", "pwd");
        Event event = new Event("Workshop", LocalDateTime.now(), LocalDateTime.now().plusHours(1), user);

        // Création de deux tâches liées à l'événement
        Task task1 = new Task("Prepare slides", 30, 2, false, user, event);
        Task task2 = new Task("Send invites", 15, 1, true, user, event);

        // Association des tâches à l'événement
        event.setTasks(List.of(task1, task2));

        // Modification du statut
        event.setStatus("DONE");

        // Vérifications des relations et du statut
        assertEquals("DONE", event.getStatus());
        assertEquals(2, event.getTasks().size());
        assertEquals(user, event.getUser());
        assertEquals(user.getId(), event.getUserId()); // Vérifie la cohérence user ↔ event
    }
}
