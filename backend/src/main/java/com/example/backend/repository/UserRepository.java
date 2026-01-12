package com.example.backend.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.example.backend.model.User;

/**
 * Repository pour l'entité User
 */
@Repository
public interface UserRepository extends JpaRepository<User, Long> {
    
    /**
     * Trouve un utilisateur par son username
     * @param username le nom d'utilisateur
     * @return Optional contenant l'utilisateur si trouvé
     */
    Optional<User> findByUsername(String username);
    
    /**
     * Vérifie si un username existe déjà
     * @param username le nom d'utilisateur
     * @return true si le username existe
     */
    boolean existsByUsername(String username);

    Optional<User> findById(Long id);
}