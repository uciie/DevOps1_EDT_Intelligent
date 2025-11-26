package com.example.backend.repository;

import com.example.backend.model.Event;
import com.example.backend.model.Task;
import com.example.backend.model.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests d'intégration pour TaskRepository.
 */
@DataJpaTest
class TaskRepositoryTest {

    @Autowired
    private TestEntityManager entityManager;

    @Autowired
    private TaskRepository taskRepository;

    private User testUser;
    private User anotherUser;

    @BeforeEach
    void setUp() {
        testUser = new User("alice", "password");
        entityManager.persist(testUser);

        anotherUser = new User("bob", "password");
        entityManager.persist(anotherUser);

        entityManager.flush();
    }

    @Test
    @DisplayName("Sauvegarde et récupération d'une tâche")
    void testSaveAndFindTask() {
        // Arrange
        Task task  = new Task("Faire les courses", 60, 5, false, testUser, (LocalDateTime) null); // CORRIGÉ

        // Act
        Task saved = taskRepository.save(task);
        entityManager.flush();
        entityManager.clear();

        Optional<Task> found = taskRepository.findById(saved.getId());

        // Assert
        assertTrue(found.isPresent());
        assertEquals("Faire les courses", found.get().getTitle());
        assertEquals(60, found.get().getEstimatedDuration());
        assertEquals(5, found.get().getPriority());
        assertFalse(found.get().isDone());
        assertEquals(testUser.getId(), found.get().getUserId());
    }

    @Test
    @DisplayName("Recherche des tâches par utilisateur")
    void testFindByUser_Id() {
        // Arrange
        Task task1 = new Task("Tâche 1", 30, 3, false, testUser, (LocalDateTime) null); // CORRIGÉ
        Task task2 = new Task("Tâche 2", 45, 4, false, testUser, (LocalDateTime) null); // CORRIGÉ
        Task task3 = new Task("Tâche 3", 60, 2, false, anotherUser, (LocalDateTime) null); // CORRIGÉ

        taskRepository.save(task1);
        taskRepository.save(task2);
        taskRepository.save(task3);
        entityManager.flush();

        // Act
        List<Task> userTasks = taskRepository.findByUser_Id(testUser.getId());
        List<Task> anotherUserTasks = taskRepository.findByUser_Id(anotherUser.getId());

        // Assert
        assertEquals(2, userTasks.size());
        assertEquals(1, anotherUserTasks.size());
        assertTrue(userTasks.stream().allMatch(t -> t.getUserId().equals(testUser.getId())));
    }

    @Test
    @DisplayName("Recherche retourne liste vide pour utilisateur sans tâches")
    void testFindByUser_Id_EmptyList() {
        // Arrange
        User newUser = new User("charlie", "password");
        entityManager.persist(newUser);
        entityManager.flush();

        // Act
        List<Task> tasks = taskRepository.findByUser_Id(newUser.getId());

        // Assert
        assertNotNull(tasks);
        assertTrue(tasks.isEmpty());
    }

    @Test
    @DisplayName("Tâche avec événement associé")
    void testTaskWithEvent() {
        // Arrange
        Event event = new Event(
            "Réunion",
            LocalDateTime.of(2025, 1, 15, 10, 0),
            LocalDateTime.of(2025, 1, 15, 12, 0),
            testUser
        );
        entityManager.persist(event);
        entityManager.flush();

        Task task = new Task("Préparer présentation", 90, 5, false, testUser, event);

        // Act
        Task saved = taskRepository.save(task);
        entityManager.flush();
        entityManager.clear();

        // Assert
        Task found = taskRepository.findById(saved.getId()).orElseThrow();
        assertNotNull(found.getEventId());
        assertEquals(event.getId(), found.getEventId());
    }

    @Test
    @DisplayName("Tâche sans événement associé")
    void testTaskWithoutEvent() {
        // Arrange
        Task task = new Task("Tâche libre", 45, 3, false, testUser, (LocalDateTime) null); // CORRIGÉ

        // Act
        Task saved = taskRepository.save(task);
        entityManager.flush();
        entityManager.clear();

        // Assert
        Task found = taskRepository.findById(saved.getId()).orElseThrow();
        assertNull(found.getEventId());
    }

    @Test
    @DisplayName("Mise à jour du statut d'une tâche")
    void testUpdateTaskStatus() {
        // Arrange
        Task task  = new Task("À supprimer", 30, 2, false, testUser, (LocalDateTime) null); // CORRIGÉ
        Task saved = taskRepository.save(task);
        entityManager.flush();
        entityManager.clear();

        // Act
        Task toUpdate = taskRepository.findById(saved.getId()).orElseThrow();
        toUpdate.setDone(true);
        taskRepository.save(toUpdate);
        entityManager.flush();
        entityManager.clear();

        // Assert
        Task updated = taskRepository.findById(saved.getId()).orElseThrow();
        assertTrue(updated.isDone());
    }

    @Test
    @DisplayName("Mise à jour de la priorité")
    void testUpdatePriority() {
        // Arrange
        Task task = new Task("Tâche", 30, 3, false, testUser, (LocalDateTime) null); 
        Task saved = taskRepository.save(task);
        entityManager.flush();
        entityManager.clear();

        // Act
        Task toUpdate = taskRepository.findById(saved.getId()).orElseThrow();
        toUpdate.setPriority(10);
        taskRepository.save(toUpdate);
        entityManager.flush();
        entityManager.clear();

        // Assert
        Task updated = taskRepository.findById(saved.getId()).orElseThrow();
        assertEquals(10, updated.getPriority());
    }

    @Test
    @DisplayName("Suppression d'une tâche")
    void testDeleteTask() {
        // Arrange
        Task task = new Task("À supprimer", 30, 2, false, testUser, (LocalDateTime) null); 
        Task saved = taskRepository.save(task);
        entityManager.flush();
        Long taskId = saved.getId();

        // Act
        taskRepository.deleteById(taskId);
        entityManager.flush();

        // Assert
        Optional<Task> found = taskRepository.findById(taskId);
        assertFalse(found.isPresent());
    }

    @Test
    @DisplayName("Tâche avec durée estimée zéro")
    void testTaskWithZeroDuration() {
        // Arrange
        Task task = new Task("Tâche rapide", 0, 1, false, testUser, (LocalDateTime) null);

        // Act
        Task saved = taskRepository.save(task);
        entityManager.flush();
        entityManager.clear();

        // Assert
        Task found = taskRepository.findById(saved.getId()).orElseThrow();
        assertEquals(0, found.getEstimatedDuration());
    }

    @Test
    @DisplayName("Tâche avec priorité négative")
    void testTaskWithNegativePriority() {
        // Arrange
        Task task = new Task("Tâche", 30, -1, false, testUser, (LocalDateTime) null);

        // Act
        Task saved = taskRepository.save(task);
        entityManager.flush();
        entityManager.clear();

        // Assert
        Task found = taskRepository.findById(saved.getId()).orElseThrow();
        assertEquals(-1, found.getPriority());
    }

    @Test
    @DisplayName("Tâche avec titre très long")
    void testTaskWithLongTitle() {
        // Arrange
        String longTitle = "Ceci est un titre de tâche extrêmement long qui contient beaucoup " +
                          "de caractères pour tester la capacité de la base de données à gérer " +
                          "des titres de tâches volumineux sans problème";
        Task task = new Task(longTitle, 60, 3, false, testUser, (LocalDateTime) null);

        // Act
        Task saved = taskRepository.save(task);
        entityManager.flush();
        entityManager.clear();

        // Assert
        Task found = taskRepository.findById(saved.getId()).orElseThrow();
        assertEquals(longTitle, found.getTitle());
    }

    @Test
    @DisplayName("Plusieurs tâches avec différentes priorités")
    void testMultipleTasksWithDifferentPriorities() {
        // Arrange
        Task lowPriority = new Task("Basse priorité", 30, 1, false, testUser, (LocalDateTime) null); 
        Task mediumPriority = new Task("Moyenne priorité", 45, 5, false, testUser, (LocalDateTime) null); 
        Task highPriority = new Task("Haute priorité", 60, 10, false, testUser, (LocalDateTime) null);

        // Act
        taskRepository.save(lowPriority);
        taskRepository.save(mediumPriority);
        taskRepository.save(highPriority);
        entityManager.flush();

        // Assert
        List<Task> tasks = taskRepository.findByUser_Id(testUser.getId());
        assertEquals(3, tasks.size());
    }

    @Test
    @DisplayName("Tâches terminées et non terminées pour le même utilisateur")
    void testDoneAndUndoneTasks() {
        // CORRECTION : Cast explicite pour la deadline
        Task done1 = new Task("Terminée 1", 30, 3, true, testUser, (LocalDateTime) null); 
        Task done2 = new Task("Terminée 2", 45, 4, true, testUser, (LocalDateTime) null); 
        Task undone = new Task("À faire", 60, 5, false, testUser, (LocalDateTime) null); 
        // ...

        taskRepository.save(done1);
        taskRepository.save(done2);
        taskRepository.save(undone);
        entityManager.flush();

        // Act
        List<Task> allTasks = taskRepository.findByUser_Id(testUser.getId());

        // Assert
        assertEquals(3, allTasks.size());
        long doneCount = allTasks.stream().filter(Task::isDone).count();
        long undoneCount = allTasks.stream().filter(t -> !t.isDone()).count();
        assertEquals(2, doneCount);
        assertEquals(1, undoneCount);
    }

    @Test
    @DisplayName("Modification de la durée estimée")
    void testUpdateEstimatedDuration() {
        // Arrange
        Task task = new Task("Tâche", 30, 3, false, testUser, (LocalDateTime) null);
        Task saved = taskRepository.save(task);
        entityManager.flush();
        entityManager.clear();

        // Act
        Task toUpdate = taskRepository.findById(saved.getId()).orElseThrow();
        toUpdate.setEstimatedDuration(90);
        taskRepository.save(toUpdate);
        entityManager.flush();
        entityManager.clear();

        // Assert
        Task updated = taskRepository.findById(saved.getId()).orElseThrow();
        assertEquals(90, updated.getEstimatedDuration());
    }

    @Test
    @DisplayName("Modification du titre")
    void testUpdateTitle() {
        // Arrange
        Task task =  new Task("Titre initial", 30, 3, false, testUser, (LocalDateTime) null);
        Task saved = taskRepository.save(task);
        entityManager.flush();
        entityManager.clear();

        // Act
        Task toUpdate = taskRepository.findById(saved.getId()).orElseThrow();
        toUpdate.setTitle("Titre modifié");
        taskRepository.save(toUpdate);
        entityManager.flush();
        entityManager.clear();

        // Assert
        Task updated = taskRepository.findById(saved.getId()).orElseThrow();
        assertEquals("Titre modifié", updated.getTitle());
    }

    @Test
    @DisplayName("Récupération de toutes les tâches")
    void testFindAll() {
        // Arrange
        taskRepository.save(new Task("Tâche 1", 30, 1, false, testUser, (LocalDateTime) null)); // CORRIGÉ
        taskRepository.save(new Task("Tâche 2", 45, 2, false, testUser, (LocalDateTime) null)); // CORRIGÉ
        taskRepository.save(new Task("Tâche 3", 60, 3, false, anotherUser, (LocalDateTime) null)); // CORRIGÉ
        entityManager.flush();
        // Act
        List<Task> tasks = taskRepository.findAll();

        // Assert
        assertTrue(tasks.size() >= 3);
    }
}