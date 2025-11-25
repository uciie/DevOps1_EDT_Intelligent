package com.example.backend.config;

import com.example.backend.service.TravelTimeCalculator;
import com.example.backend.service.impl.SimpleTravelTimeCalculator;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.context.annotation.Profile;

/**
 * Configuration centralisée pour les tests.
 * Cette classe permet de définir des beans spécifiques aux tests.
 */
@TestConfiguration
@Profile("test")
public class TestConfig {

    /**
     * Bean TravelTimeCalculator pour les tests.
     * Utilise toujours le SimpleTravelTimeCalculator pour éviter les appels API.
     */
    @Bean
    @Primary
    public TravelTimeCalculator testTravelTimeCalculator() {
        return new SimpleTravelTimeCalculator();
    }
}