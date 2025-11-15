package com.example.backend.repository;

import com.example.backend.model.User;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests d'intégration pour UserRepository.
 * Utilise une base de données H2 en mémoire pour tester les opérations CRUD.
 */
@DataJpaTest
class UserRepositoryTest {

    @Autowired
    private TestEntityManager entityManager;

    @Autowired
    private UserRepository userRepository;

    @Test
    void testSaveUser() {
        // Arrange
        User user = new User("testuser", "password123");

        // Act
        User savedUser = userRepository.save(user);

        // Assert
        assertNotNull(savedUser.getId());
        assertEquals("testuser", savedUser.getUsername());
        assertEquals("password123", savedUser.getPassword());
    }

    @Test
    void testFindByUsername() {
        // Arrange
        User user = new User("alice", "password");
        entityManager.persist(user);
        entityManager.flush();

        // Act
        Optional<User> foundUser = userRepository.findByUsername("alice");

        // Assert
        assertTrue(foundUser.isPresent());
        assertEquals("alice", foundUser.get().getUsername());
        assertEquals("password", foundUser.get().getPassword());
    }

    @Test
    void testFindByUsername_NotFound() {
        // Act
        Optional<User> foundUser = userRepository.findByUsername("nonexistent");

        // Assert
        assertFalse(foundUser.isPresent());
    }

    @Test
    void testFindByUsername_CaseSensitive() {
        // Arrange
        User user = new User("BobTheBuilder", "password");
        entityManager.persist(user);
        entityManager.flush();

        // Act
        Optional<User> foundExact = userRepository.findByUsername("BobTheBuilder");
        Optional<User> foundLowerCase = userRepository.findByUsername("bobthebuilder");

        // Assert
        assertTrue(foundExact.isPresent());
        assertFalse(foundLowerCase.isPresent());
    }

    @Test
    void testFindById() {
        // Arrange
        User user = new User("charlie", "password");
        entityManager.persist(user);
        entityManager.flush();

        // Act
        Optional<User> foundUser = userRepository.findById(user.getId());

        // Assert
        assertTrue(foundUser.isPresent());
        assertEquals("charlie", foundUser.get().getUsername());
    }

    @Test
    void testFindById_NotFound() {
        // Act
        Optional<User> foundUser = userRepository.findById(999L);

        // Assert
        assertFalse(foundUser.isPresent());
    }

    @Test
    void testFindAll() {
        // Arrange
        User user1 = new User("dave", "password1");
        User user2 = new User("eve", "password2");
        entityManager.persist(user1);
        entityManager.persist(user2);
        entityManager.flush();

        // Act
        List<User> users = userRepository.findAll();

        // Assert
        assertTrue(users.size() >= 2);
        assertTrue(users.stream().anyMatch(u -> u.getUsername().equals("dave")));
        assertTrue(users.stream().anyMatch(u -> u.getUsername().equals("eve")));
    }

    @Test
    void testUpdateUser() {
        // Arrange
        User user = new User("frank", "oldpassword");
        entityManager.persist(user);
        entityManager.flush();

        // Act
        user.setPassword("newpassword");
        User updatedUser = userRepository.save(user);

        // Assert
        assertEquals("newpassword", updatedUser.getPassword());
        assertEquals("frank", updatedUser.getUsername());
    }

    @Test
    void testUpdateUsername() {
        // Arrange
        User user = new User("grace", "password");
        entityManager.persist(user);
        entityManager.flush();

        // Act
        user.setUsername("grace_updated");
        User updatedUser = userRepository.save(user);

        // Assert
        assertEquals("grace_updated", updatedUser.getUsername());
    }

    @Test
    void testDeleteUser() {
        // Arrange
        User user = new User("henry", "password");
        entityManager.persist(user);
        entityManager.flush();

        Long userId = user.getId();

        // Act
        userRepository.deleteById(userId);
        entityManager.flush();

        // Assert
        Optional<User> deletedUser = userRepository.findById(userId);
        assertFalse(deletedUser.isPresent());
    }

    @Test
    void testDeleteUser_VerifyNotFoundByUsername() {
        // Arrange
        User user = new User("iris", "password");
        entityManager.persist(user);
        entityManager.flush();

        // Act
        userRepository.deleteById(user.getId());
        entityManager.flush();

        // Assert
        Optional<User> foundUser = userRepository.findByUsername("iris");
        assertFalse(foundUser.isPresent());
    }

    @Test
    void testCount() {
        // Arrange
        long initialCount = userRepository.count();

        User user = new User("jack", "password");
        entityManager.persist(user);
        entityManager.flush();

        // Act
        long newCount = userRepository.count();

        // Assert
        assertEquals(initialCount + 1, newCount);
    }

    @Test
    void testExistsById() {
        // Arrange
        User user = new User("kate", "password");
        entityManager.persist(user);
        entityManager.flush();

        // Act
        boolean exists = userRepository.existsById(user.getId());
        boolean notExists = userRepository.existsById(999L);

        // Assert
        assertTrue(exists);
        assertFalse(notExists);
    }

    @Test
    void testSaveUser_EmptyPassword() {
        // Arrange
        User user = new User("lisa", "");

        // Act
        User savedUser = userRepository.save(user);

        // Assert
        assertNotNull(savedUser.getId());
        assertEquals("", savedUser.getPassword());
    }

    @Test
    void testSaveMultipleUsersWithDifferentUsernames() {
        // Arrange
        User user1 = new User("mike", "password1");
        User user2 = new User("nancy", "password2");
        User user3 = new User("oscar", "password3");

        // Act
        userRepository.save(user1);
        userRepository.save(user2);
        userRepository.save(user3);

        // Assert
        assertEquals(3, userRepository.findAll().stream()
            .filter(u -> u.getUsername().equals("mike") || 
                        u.getUsername().equals("nancy") || 
                        u.getUsername().equals("oscar"))
            .count());
    }

    @Test
    void testFindByUsername_WithSpecialCharacters() {
        // Arrange
        User user = new User("user_123-test", "password");
        entityManager.persist(user);
        entityManager.flush();

        // Act
        Optional<User> foundUser = userRepository.findByUsername("user_123-test");

        // Assert
        assertTrue(foundUser.isPresent());
        assertEquals("user_123-test", foundUser.get().getUsername());
    }

    @Test
    void testSaveUser_LongUsername() {
        // Arrange
        String longUsername = "a".repeat(100);
        User user = new User(longUsername, "password");

        // Act
        User savedUser = userRepository.save(user);

        // Assert
        assertNotNull(savedUser.getId());
        assertEquals(longUsername, savedUser.getUsername());
    }

    @Test
    void testSaveUser_LongPassword() {
        // Arrange
        String longPassword = "p".repeat(200);
        User user = new User("peter", longPassword);

        // Act
        User savedUser = userRepository.save(user);

        // Assert
        assertNotNull(savedUser.getId());
        assertEquals(longPassword, savedUser.getPassword());
    }

    @Test
    void testFindAll_EmptyDatabase() {
        // Arrange
        userRepository.deleteAll();
        entityManager.flush();

        // Act
        List<User> users = userRepository.findAll();

        // Assert
        assertTrue(users.isEmpty());
    }

    @Test
    void testSaveAndFlush() {
        // Arrange
        User user = new User("quinn", "password");

        // Act
        User savedUser = userRepository.saveAndFlush(user);

        // Assert
        assertNotNull(savedUser.getId());
        
        // Vérifier que l'utilisateur est bien persisté immédiatement
        Optional<User> foundUser = userRepository.findById(savedUser.getId());
        assertTrue(foundUser.isPresent());
    }

    @Test
    void testUserWithNullLists() {
        // Arrange
        User user = new User("rachel", "password");
        
        // Act
        User savedUser = userRepository.save(user);

        // Assert
        assertNotNull(savedUser.getId());
        assertNull(savedUser.getEvents());
        assertNull(savedUser.getTasks());
    }

    @Test
    void testMultipleFindByUsername() {
        // Arrange
        User user1 = new User("sam", "password1");
        User user2 = new User("tom", "password2");
        entityManager.persist(user1);
        entityManager.persist(user2);
        entityManager.flush();

        // Act
        Optional<User> foundSam = userRepository.findByUsername("sam");
        Optional<User> foundTom = userRepository.findByUsername("tom");
        Optional<User> foundNone = userRepository.findByUsername("uma");

        // Assert
        assertTrue(foundSam.isPresent());
        assertTrue(foundTom.isPresent());
        assertFalse(foundNone.isPresent());
        assertEquals("sam", foundSam.get().getUsername());
        assertEquals("tom", foundTom.get().getUsername());
    }
}