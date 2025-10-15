package com.example.backend.controller;

import com.example.backend.model.User;
import com.example.backend.service.UserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.http.ResponseEntity;

import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class UserControllerTest {

    @Mock
    private UserService userService;

    @InjectMocks
    private UserController userController;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
    }

    @Test
    void testRegister_Success() {
        // Arrange
        User inputUser = new User("john_doe", "password123");
        User savedUser = new User("john_doe", "password123");
        savedUser.setId(1L);

        when(userService.registerUser("john_doe", "password123")).thenReturn(savedUser);

        // Act
        ResponseEntity<User> response = userController.register(inputUser);

        // Assert
        assertEquals(200, response.getStatusCode().value());
        assertNotNull(response.getBody());
        assertEquals("john_doe", response.getBody().getUsername());
        assertNull(response.getBody().getPassword(), "Le mot de passe ne doit pas être renvoyé au client");

        verify(userService, times(1)).registerUser("john_doe", "password123");
    }

    @Test
    void testRegister_Failure() {
        // Arrange
        User inputUser = new User("john_doe", "password123");

        when(userService.registerUser("john_doe", "password123"))
                .thenThrow(new RuntimeException("User already exists"));

        // Act
        ResponseEntity<User> response = userController.register(inputUser);

        // Assert
        assertEquals(400, response.getStatusCode().value());
        assertNull(response.getBody(), "En cas d'erreur, le corps de la réponse doit être null");

        verify(userService, times(1)).registerUser("john_doe", "password123");
    }

    @Test
    void testGetAllUsers() {
        // Arrange
        List<User> users = Arrays.asList(
                new User("alice", "pass1"),
                new User("bob", "pass2")
        );

        when(userService.getAllUsers()).thenReturn(users);

        // Act
        List<User> result = userController.getAll();

        // Assert
        assertEquals(2, result.size());
        assertEquals("alice", result.get(0).getUsername());
        assertEquals("bob", result.get(1).getUsername());
        verify(userService, times(1)).getAllUsers();
    }
}