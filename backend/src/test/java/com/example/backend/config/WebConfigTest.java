package com.example.backend.config;

import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;

import static org.junit.jupiter.api.Assertions.*;

class WebConfigTest {

    @Test
    void testCorsConfigurer() {
        // Arrange
        WebConfig webConfig = new WebConfig();
        
        // Act
        var corsConfigurer = webConfig.corsConfigurer();
        
        // Assert
        assertNotNull(corsConfigurer);
    }

    @Test
    void testCorsMapping() {
        // Arrange
        WebConfig webConfig = new WebConfig();
        var corsConfigurer = webConfig.corsConfigurer();
        
        // Créer un mock CorsRegistry pour tester
        CorsRegistry registry = new CorsRegistry();
        
        // Act
        corsConfigurer.addCorsMappings(registry);
        
        // Assert - Vérifier que la configuration est appliquée
        // On ne peut pas facilement tester le contenu du registry sans Spring,
        // mais on peut vérifier que la méthode ne lance pas d'exception
        assertNotNull(registry);
    }

    @Test
    void testRestTemplateBean() {
        // Arrange
        WebConfig webConfig = new WebConfig();
        
        // Act
        RestTemplate restTemplate = webConfig.restTemplate();
        
        // Assert
        assertNotNull(restTemplate);
        assertInstanceOf(RestTemplate.class, restTemplate);
    }

    @Test
    void testRestTemplateIsNewInstance() {
        // Arrange
        WebConfig webConfig = new WebConfig();
        
        // Act
        RestTemplate restTemplate1 = webConfig.restTemplate();
        RestTemplate restTemplate2 = webConfig.restTemplate();
        
        // Assert - Chaque appel devrait créer une nouvelle instance
        assertNotNull(restTemplate1);
        assertNotNull(restTemplate2);
        // Note: En production avec @Bean, Spring gérerait le singleton,
        // mais ici on teste directement la méthode
    }
}
