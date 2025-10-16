package com.example.backend.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * Configuration Web pour l'application
 * Cette classe va configurer les paramètres CORS (Cross-Origin REsource Sharing) pour autoriser les requêtes
 * provenant du front-end hébérgé sur le localhost 5173
 */
@Configuration
public class WebConfig {
    /*
     * Configure les mappage CORS pour l'application
     * @return un Webconfigurer avec la configuration CORS
     */
    @Bean
    public WebMvcConfigurer corsConfigurer() {
        /*
         * Ajoute des mappages CORS au registre.
         * 
         * @param registry le registre CORS.
         */
        return new WebMvcConfigurer() {

            @Override
            public void addCorsMappings(CorsRegistry registry) {
                registry.addMapping("/api/**")
                        .allowedOrigins("http://localhost:5173")
                        .allowedMethods("GET", "POST", "PUT", "DELETE", "OPTIONS")
                        .allowedHeaders("*")
                        .allowCredentials(true);
            }
        };
    }
}
