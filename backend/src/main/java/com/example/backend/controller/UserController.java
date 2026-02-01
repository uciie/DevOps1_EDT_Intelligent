package com.example.backend.controller;

import com.example.backend.model.User;
import com.example.backend.service.UserService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Contrôleur pour la gestion des utilisateurs.
 * Fournit des points de terminaison pour l'enregistrement, la connexion et la récupération des utilisateurs.
 */
@RestController
@RequestMapping("/api/users")
@CrossOrigin(origins = "http://localhost:5173")
public class UserController {

    private final UserService userService;
    private final String username = "username";
    private final String password = "password";
    private final String id = "id";

    /**
     * Construit un nouveau UserController avec le service utilisateur donné.
     *
     * @param userService le service à utiliser pour la gestion des utilisateurs.
     */
    public UserController(UserService userService) {
        this.userService = userService;
    }

    /**
     * POST /api/users/login
     * Body: { "username": "...", "password": "..." }
     * 
     * @param credentials Map contenant username et password
     * @return ResponseEntity avec l'utilisateur ou une erreur
     */
    @PostMapping("/login")
    public ResponseEntity<Object> login(@RequestBody Map<String, String> credentials) {
        String username = credentials.get(this.username);
        String password = credentials.get(this.password);

        // Validation
        if (username == null || username.trim().isEmpty()) {
            Map<String, String> error = new HashMap<>();
            error.put("error", "Username requis");
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error);
        }

        if (password == null || password.trim().isEmpty()) {
            Map<String, String> error = new HashMap<>();
            error.put("error", "Password requis");
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error);
        }

        // Authentification via le service
        User user = userService.authenticate(username, password);

        if (user == null) {
            Map<String, String> error = new HashMap<>();
            error.put("error", "Identifiants incorrects");
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(error);
        }

        // Succès: retourner l'utilisateur SANS le mot de passe
        Map<String, Object> response = new HashMap<>();
        response.put(this.id, user.getId());
        response.put(this.username, user.getUsername());

        return ResponseEntity.ok(response);
    }

    /**
     * Enregistre un nouvel utilisateur.
     *
     * @param user l'utilisateur à enregistrer.
     * @return une ResponseEntity contenant l'utilisateur nouvellement créé ou une erreur si l'enregistrement échoue.
     */
    @PostMapping("/register")
    public ResponseEntity<Object> register(@RequestBody User user) {
        try {
            User newUser = userService.registerUser(user.getUsername(), user.getPassword());
            
            // Retourner sans le mot de passe
            Map<String, Object> response = new HashMap<>();
            response.put(this.id, newUser.getId());
            response.put(this.username, newUser.getUsername());
            
            return ResponseEntity.status(HttpStatus.CREATED).body(response);
        } catch (RuntimeException e) {
            Map<String, String> error = new HashMap<>();
            error.put("error", e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error);
        }
    }

    /**
     * Récupère tous les utilisateurs (ID et Username uniquement).
     */
    @GetMapping
    public ResponseEntity<List<Map<String, Object>>> getAll() {
        List<User> users = userService.getAllUsers();
        
        // On transforme la liste d'utilisateurs en liste de Maps simplifiées
        List<Map<String, Object>> simpleUsers = users.stream()
            .map(user -> {
                Map<String, Object> userMap = new HashMap<>();
                userMap.put(this.id, user.getId());
                userMap.put(this.username, user.getUsername());
                // On n'ajoute PAS events ni tasks ici
                return userMap;
            })
            .toList(); // Ou .collect(Collectors.toList()) pour les vieilles versions de Java
        
        return ResponseEntity.ok(simpleUsers);
    }

    /**
     * Récupère un utilisateur par son nom d'utilisateur.
     *
     * @param username le nom d'utilisateur de l'utilisateur à récupérer.
     * @return une ResponseEntity contenant l'utilisateur ou une erreur si l'utilisateur n'est pas trouvé.
     */    
    @GetMapping("/username/{username}")
    public ResponseEntity<Object> getByUsername(@PathVariable String username) {
        User user = userService.getUserByUsername(username);
        if (user == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
        }
        return ResponseEntity.ok(user);
    }

    /**
     * Recevoir le code d'autorisation Google OAuth2.
     * POST /api/users/{id}/google-auth
     * 
     * @param id l'ID de l'utilisateur
     * @param payload Map contenant le code d'autorisation
     * @return ResponseEntity avec l'utilisateur mis à jour ou une erreur
     */
    @PostMapping("/{id}/google-auth")
    public ResponseEntity<?> handleGoogleCallback(@PathVariable Long id, @RequestBody Map<String, String> payload) {
        String code = payload.get("code");
        try {
            User updatedUser = userService.saveGoogleTokens(id, code);
            return ResponseEntity.ok(updatedUser);
        } catch (IOException e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Erreur lors de l'échange du code Google: " + e.getMessage());
        }
    }
}