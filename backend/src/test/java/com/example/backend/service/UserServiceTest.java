package com.example.backend.service;

import com.example.backend.model.User;
import com.example.backend.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Tests unitaires pour UserService.
 */
class UserServiceTest {

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private UserService userService;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
    }

    @Test
    @DisplayName("Enregistrement d'un nouvel utilisateur avec succès")
    void testRegisterUser_Success() {
        // Arrange
        String username = "alice";
        String password = "password123";
        
        when(userRepository.findByUsername(username)).thenReturn(Optional.empty());
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> {
            User user = invocation.getArgument(0);
            user.setId(1L);
            return user;
        });

        // Act
        User result = userService.registerUser(username, password);

        // Assert
        assertNotNull(result);
        assertEquals(username, result.getUsername());
        assertEquals(password, result.getPassword());
        assertNotNull(result.getId());
        
        verify(userRepository, times(1)).findByUsername(username);
        verify(userRepository, times(1)).save(any(User.class));
    }

    @Test
    @DisplayName("Enregistrement échoue si l'utilisateur existe déjà")
    void testRegisterUser_UsernameAlreadyExists() {
        // Arrange
        String username = "alice";
        String password = "password123";
        
        User existingUser = new User(username, "oldpassword");
        existingUser.setId(1L);
        
        when(userRepository.findByUsername(username)).thenReturn(Optional.of(existingUser));

        // Act & Assert
        RuntimeException exception = assertThrows(RuntimeException.class, () -> {
            userService.registerUser(username, password);
        });
        
        assertEquals("Nom d'utilisateur déjà pris", exception.getMessage());
        
        verify(userRepository, times(1)).findByUsername(username);
        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    @DisplayName("Récupération de tous les utilisateurs")
    void testGetAllUsers() {
        // Arrange
        User user1 = new User("alice", "pass1");
        user1.setId(1L);
        
        User user2 = new User("bob", "pass2");
        user2.setId(2L);
        
        User user3 = new User("charlie", "pass3");
        user3.setId(3L);
        
        List<User> users = Arrays.asList(user1, user2, user3);
        
        when(userRepository.findAll()).thenReturn(users);

        // Act
        List<User> result = userService.getAllUsers();

        // Assert
        assertNotNull(result);
        assertEquals(3, result.size());
        assertEquals("alice", result.get(0).getUsername());
        assertEquals("bob", result.get(1).getUsername());
        assertEquals("charlie", result.get(2).getUsername());
        
        verify(userRepository, times(1)).findAll();
    }

    @Test
    @DisplayName("Liste vide si aucun utilisateur")
    void testGetAllUsers_EmptyList() {
        // Arrange
        when(userRepository.findAll()).thenReturn(Arrays.asList());

        // Act
        List<User> result = userService.getAllUsers();

        // Assert
        assertNotNull(result);
        assertTrue(result.isEmpty());
        
        verify(userRepository, times(1)).findAll();
    }

    @Test
    @DisplayName("Enregistrement avec username null lance une exception")
    void testRegisterUser_NullUsername() {
        // Act & Assert
        assertThrows(Exception.class, () -> {
            userService.registerUser(null, "password");
        });
    }

    @Test
    @DisplayName("Enregistrement avec password null lance une exception")
    void testRegisterUser_NullPassword() {
        // Act & Assert
        assertThrows(Exception.class, () -> {
            userService.registerUser("alice", null);
        });
    }

    @Test
    @DisplayName("Enregistrement avec username vide")
    void testRegisterUser_EmptyUsername() {
        // Arrange
        String username = "";
        String password = "password123";
        // Act & Assert: empty username must be rejected by business rules
        when(userRepository.findByUsername(username)).thenReturn(Optional.empty());
        assertThrows(IllegalArgumentException.class, () -> userService.registerUser(username, password));
    }

    @Test
    @DisplayName("Vérification de l'unicité du username (case sensitive)")
    void testRegisterUser_CaseSensitive() {
        // Arrange
        String username1 = "Alice";
        String username2 = "alice";
        
        User existingUser = new User(username1, "password");
        
        when(userRepository.findByUsername(username1)).thenReturn(Optional.of(existingUser));
        when(userRepository.findByUsername(username2)).thenReturn(Optional.empty());
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> {
            User user = invocation.getArgument(0);
            user.setId(2L);
            return user;
        });

        // Act
        RuntimeException exception = assertThrows(RuntimeException.class, () -> {
            userService.registerUser(username1, "newpass");
        });
        
        User result = userService.registerUser(username2, "newpass");

        // Assert
        assertNotNull(exception);
        assertNotNull(result);
        assertEquals(username2, result.getUsername());
    }

    @Test
    @DisplayName("Enregistrement préserve le mot de passe tel quel")
    void testRegisterUser_PasswordNotHashed() {
        // Arrange
        String username = "alice";
        String password = "mySecretPassword123!";
        
        when(userRepository.findByUsername(username)).thenReturn(Optional.empty());
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> {
            User user = invocation.getArgument(0);
            user.setId(1L);
            return user;
        });

        // Act
        User result = userService.registerUser(username, password);

        // Assert
        assertEquals(password, result.getPassword());
        // Note: Dans une vraie application, le mot de passe devrait être hashé
    }

    @Test
    @DisplayName("Récupération de plusieurs utilisateurs dans l'ordre")
    void testGetAllUsers_OrderPreserved() {
        // Arrange
        User user1 = new User("alice", "pass1");
        user1.setId(1L);
        
        User user2 = new User("bob", "pass2");
        user2.setId(2L);
        
        when(userRepository.findAll()).thenReturn(Arrays.asList(user1, user2));

        // Act
        List<User> result = userService.getAllUsers();

        // Assert
        assertEquals(2, result.size());
        assertEquals(1L, result.get(0).getId());
        assertEquals(2L, result.get(1).getId());
    }

    @Test
    @DisplayName("Repository est appelé une seule fois pour getAllUsers")
    void testGetAllUsers_RepositoryCalledOnce() {
        // Arrange
        when(userRepository.findAll()).thenReturn(Arrays.asList());

        // Act
        userService.getAllUsers();
        userService.getAllUsers();

        // Assert
        verify(userRepository, times(2)).findAll();
    }
}