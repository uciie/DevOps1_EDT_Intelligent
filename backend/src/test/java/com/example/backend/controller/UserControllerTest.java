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
import java.util.Map;

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
        // Le contrôleur retourne maintenant ResponseEntity<?> car le corps est une Map
        ResponseEntity<?> response = userController.register(inputUser);

        // Assert
        assertEquals(201, response.getStatusCode().value()); // Expecting CREATED (201)
        assertNotNull(response.getBody());
        
        // Vérification du contenu de la Map
        assertTrue(response.getBody() instanceof Map);
        Map<?, ?> body = (Map<?, ?>) response.getBody();
        
        assertEquals("john_doe", body.get("username"));
        assertEquals(1L, body.get("id"));
        assertFalse(body.containsKey("password"), "Le mot de passe ne doit pas être présent dans la réponse");

        verify(userService, times(1)).registerUser("john_doe", "password123");
    }

    @Test
    void testRegister_Failure() {
        // Arrange
        User inputUser = new User("john_doe", "password123");

        when(userService.registerUser("john_doe", "password123"))
                .thenThrow(new RuntimeException("User already exists"));

        // Act
        ResponseEntity<?> response = userController.register(inputUser);

        // Assert
        assertEquals(400, response.getStatusCode().value());
        assertNotNull(response.getBody());
        
        assertTrue(response.getBody() instanceof Map);
        Map<?, ?> body = (Map<?, ?>) response.getBody();
        assertTrue(body.containsKey("error"));
        assertEquals("User already exists", body.get("error"));

        verify(userService, times(1)).registerUser("john_doe", "password123");
    }

    @Test
    void testGetAllUsers() {
        // Arrange
        User u1 = new User("alice", "pass1");
        u1.setId(1L); // On simule des IDs pour être précis
        User u2 = new User("bob", "pass2");
        u2.setId(2L);

        List<User> users = Arrays.asList(u1, u2);

        when(userService.getAllUsers()).thenReturn(users);

        // Act
        // On capture la réponse. 
        // Note : Assurez-vous que votre Controller retourne bien ResponseEntity<List<Map<String, Object>>>
        ResponseEntity<List<Map<String, Object>>> response = userController.getAll();

        // Assert
        assertEquals(200, response.getStatusCode().value());
        
        List<Map<String, Object>> result = response.getBody();
        assertNotNull(result);
        assertEquals(2, result.size());
        
        // Vérification du premier utilisateur (Alice)
        assertEquals("alice", result.get(0).get("username"));
        assertEquals(1L, result.get(0).get("id"));
        
        // VÉRIFICATIONS CRITIQUES :
        // 1. Le mot de passe ne doit pas être là
        assertFalse(result.get(0).containsKey("password"), "Le mot de passe ne doit pas être exposé");
        
        // 2. Les listes 'events' et 'tasks' ne doivent pas être là (pour résoudre votre problème JSON précédent)
        assertFalse(result.get(0).containsKey("events"), "Les événements ne doivent pas être chargés");
        assertFalse(result.get(0).containsKey("tasks"), "Les tâches ne doivent pas être chargées");

        verify(userService, times(1)).getAllUsers();
    }
}