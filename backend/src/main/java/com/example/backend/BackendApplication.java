package com.example.backend;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * Classe principale de l'application backend.
 * Point d'entrée pour l'application Spring Boot.
 */
@SpringBootApplication(scanBasePackages = "com.example")
@EnableScheduling
public class BackendApplication {

    /**
     * Méthode principale qui démarre l'application Spring Boot.
     *
     * @param args arguments de la ligne de commande.
     */
    public static void main(String[] args) {
        SpringApplication.run(BackendApplication.class, args);
    }
}