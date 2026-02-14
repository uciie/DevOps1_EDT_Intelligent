package com.example.backend.service;

import org.springframework.stereotype.Service;
import com.google.api.client.googleapis.auth.oauth2.GoogleAuthorizationCodeTokenRequest;
import com.google.api.client.googleapis.auth.oauth2.GoogleTokenResponse;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.jackson2.JacksonFactory;
import org.springframework.beans.factory.annotation.Value;

import com.example.backend.model.User;
import com.example.backend.repository.UserRepository;

import java.io.IOException;
import java.util.List;
import java.util.Optional;

@Service
public class UserService {

    @Value("${google.client.id}")
    private String clientId;

    @Value("${google.client.secret}")
    private String clientSecret;

    @Value("${google.redirect.uri}")
    private String redirectUri;

    private final UserRepository userRepository;

    public UserService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    /**
     * Enregistre un nouvel utilisateur
     * 
     * @param username le nom d'utilisateur
     * @param password le mot de passe
     * @return l'utilisateur créé
     * @throws IllegalArgumentException si username ou password est null
     * @throws RuntimeException si le username existe déjà
     */
    public User registerUser(String username, String password) {
        if (username == null) {
            throw new IllegalArgumentException("username must not be null");
        }
        if (password == null) {
            throw new IllegalArgumentException("password must not be null");
        }

        if (userRepository.findByUsername(username).isPresent()) {
            throw new RuntimeException("Nom d'utilisateur déjà pris");
        }

        User newUser = new User(username, password);
        return userRepository.save(newUser);
    }

    /**
     * Authentifie un utilisateur avec username et password
     * 
     * @param username le nom d'utilisateur
     * @param password le mot de passe
     * @return l'utilisateur si les identifiants sont corrects, null sinon
     */
    public User authenticate(String username, String password) {
        if (username == null || password == null) {
            return null;
        }

        Optional<User> userOpt = userRepository.findByUsername(username);

        if (userOpt.isEmpty()) {
            return null; // Utilisateur non trouvé
        }

        User user = userOpt.get();

        // ATTENTION: En production, utiliser BCrypt pour comparer les mots de passe hachés
        // Exemple: if (!passwordEncoder.matches(password, user.getPassword()))
        if (!user.getPassword().equals(password)) {
            return null; // Mot de passe incorrect
        }

        return user; // Authentification réussie
    }

    /**
     * Récupère tous les utilisateurs
     * 
     * @return la liste de tous les utilisateurs
     */
    public List<User> getAllUsers() {
        return userRepository.findAll();
    }

    /**
     * Récupère un utilisateur par son ID
     * 
     * @param id l'ID de l'utilisateur
     * @return l'utilisateur trouvé
     * @throws RuntimeException si l'utilisateur n'existe pas
     */
    public User getUserById(Long id) {
        return userRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Utilisateur non trouvé avec l'ID: " + id));
    }

    /**
     * Vérifie si un username existe
     * 
     * @param username le nom d'utilisateur
     * @return true si le username existe
     */
    public boolean existsByUsername(String username) {
        return userRepository.existsByUsername(username);
    }

    /**
     * Récupère un utilisateur par son nom d'utilisateur
     * 
     * @param username le nom d'utilisateur
     * @return l'utilisateur trouvé, ou null si non trouvé
     */
    public User getUserByUsername(String username) {
        return userRepository.findByUsername(username).orElse(null);
    }

    /**
     * Échange le code d'autorisation Google contre des tokens et les enregistre pour l'utilisateur.
     * 
     * @param userId l'ID de l'utilisateur
     * @param code le code d'autorisation reçu de Google
     * @return l'utilisateur mis à jour avec les tokens Google
     * @throws IOException si une erreur survient lors de l'échange du code
     */
    public User saveGoogleTokens(Long userId, String code) throws IOException {
        GoogleTokenResponse response = new GoogleAuthorizationCodeTokenRequest(
                new NetHttpTransport(),
                JacksonFactory.getDefaultInstance(),
                "https://oauth2.googleapis.com/token",
                clientId,
                clientSecret,
                code,
                redirectUri)
                .execute();

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("Utilisateur non trouvé"));

        user.setGoogleAccessToken(response.getAccessToken());
        
        // Le refresh token n'est envoyé par Google que lors de la première autorisation 
        // ou si l'accès est forcé en mode "offline"
        if (response.getRefreshToken() != null) {
            user.setGoogleRefreshToken(response.getRefreshToken());
        }

        return userRepository.save(user);
    }

    /**
     * Révoque l'accès Google de l'utilisateur en effaçant ses tokens en base.
     * Note : Cette méthode n'appelle PAS l'API de révocation Google (optionnel,
     * voir commentaire ci-dessous). Elle efface simplement les tokens localement.
     *
     * @param userId l'ID de l'utilisateur
     * @return l'utilisateur mis à jour
     */
    public User unlinkGoogleAccount(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("Utilisateur non trouvé avec l'ID: " + userId));

        user.setGoogleAccessToken(null);
        user.setGoogleRefreshToken(null);

        return userRepository.save(user);
    }
}