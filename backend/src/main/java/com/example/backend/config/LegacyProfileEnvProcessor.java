package com.example.backend.config;

import java.util.HashMap;
import java.util.Map;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.env.EnvironmentPostProcessor;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.MapPropertySource;

public class LegacyProfileEnvProcessor implements EnvironmentPostProcessor {

    @Override
    public void postProcessEnvironment(ConfigurableEnvironment environment, SpringApplication application) {
        String legacy = System.getenv("SPRING_PROFILES");
        if (legacy != null && !legacy.isEmpty()) {
            Map<String, Object> props = new HashMap<>();
            props.put("spring.config.activate.on-profile", legacy);
            environment.getPropertySources().addFirst(new MapPropertySource("legacy-spring-profiles", props));
        }
    }
}
