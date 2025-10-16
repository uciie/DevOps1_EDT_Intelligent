package com.example.backend.controller;

import com.example.backend.model.User;
import com.example.backend.service.UserService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Contrôleur pour la gestion des utilisateurs.
 * Fournit des points de terminaison pour l'enregistrement et la récupération des utilisateurs.
 */
@RestController
@RequestMapping("/api/users")
@CrossOrigin(origins = "http://localhost:5173")
public class UserController {

    private final UserService userService;

    /**
     * Construit un nouveau UserController avec le service utilisateur donné.
     *
     * @param userService le service à utiliser pour la gestion des utilisateurs.
     */
    public UserController(UserService userService) {
        this.userService = userService;
    }

    /**
     * Enregistre un nouvel utilisateur.
     *
     * @param user l'utilisateur à enregistrer.
     * @return une ResponseEntity contenant l'utilisateur nouvellement créé ou une erreur si l'enregistrement échoue.
     */
    @PostMapping("/register")
    public ResponseEntity<User> register(@RequestBody User user) {
        try {
            User newUser = userService.registerUser(user.getUsername(), user.getPassword());
            newUser.setPassword(null); // on ne renvoie pas le mot de passe
            return ResponseEntity.ok(newUser);
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(null);
        }
    }

    /**
     * Récupère tous les utilisateurs.
     *
     * @return une liste de tous les utilisateurs.
     */
    @GetMapping
    public List<User> getAll() {
        return userService.getAllUsers();
    }
}
