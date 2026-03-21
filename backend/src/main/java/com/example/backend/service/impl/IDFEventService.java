package com.example.backend.service.impl;

import com.example.backend.dto.EventDTO;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

@Service
public class IDFEventService {

    private final RestTemplate restTemplate = new RestTemplate();
    private final ObjectMapper objectMapper = new ObjectMapper();

    public List<EventDTO> searchEvents(String userQuery) {
        String today = java.time.LocalDate.now().toString();



        // On utilise 'suggest' ou le filtrage direct sur le champ keywords_fr
        String url = UriComponentsBuilder.fromHttpUrl("https://data.iledefrance.fr/api/explore/v2.1/catalog/datasets/evenements-publics-cibul/records")
                // Filtre : Date de fin future ET (le mot est dans les mots-clés OU le titre)
                .queryParam("where", "lastdate_end >= \"" + today + "\" AND (keywords_fr = \"" + userQuery + "\" OR title_fr LIKE \"*" + userQuery + "*\")")
                .queryParam("order_by", "lastdate_end ASC")
                .queryParam("limit", 10)
                .build()
                .toUriString();

        System.out.println("\n--- [DEBUG START] ---");
        System.out.println("1. URL appelée : " + url);

        try {
            ResponseEntity<String> responseEntity = restTemplate.getForEntity(url, String.class);
            System.out.println("2. Status Code API : " + responseEntity.getStatusCode());
            
            // Log du début de la réponse brute (pour vérifier la structure)
            String body = responseEntity.getBody();
            if (body != null && body.length() > 500) {
                System.out.println("3. Extrait réponse brute : " + body.substring(0, 500) + "...");
            } else {
                System.out.println("3. Réponse brute : " + body);
            }

            JsonNode root = objectMapper.readTree(body);
            JsonNode results = root.path("results");
            
            System.out.println("4. Nombre de résultats trouvés dans le JSON : " + (results.isArray() ? results.size() : 0));

            List<EventDTO> events = new ArrayList<>();
            if (results.isArray()) {
                for (JsonNode node : results) {
                    EventDTO dto = new EventDTO();
                    
                    // On teste les deux versions (avec et sans _fr)
                    String title = node.path("title_fr").asText(node.path("title").asText("Sans titre"));
                    dto.setTitle(title);
                    
                    dto.setDescription(node.path("description_fr").asText(node.path("description").asText("")));
                    String timingsStr = node.path("timings").asText("");
LocalDateTime eventStart = null;
LocalDateTime eventEnd = null;

try {
    if (!timingsStr.isEmpty()) {
        // On parse la String timings pour en faire un vrai tableau JSON
        JsonNode timingsArray = objectMapper.readTree(timingsStr);
        
        if (timingsArray.isArray()) {
    for (JsonNode timing : timingsArray) {
        LocalDateTime start = parseDate(timing.path("begin").asText());
        // On ne prend que le premier créneau de 2026 qu'on trouve
        if (start != null && start.getYear() == 2026) {
            eventStart = start;
            eventEnd = parseDate(timing.path("end").asText());
            break; // On a trouvé notre date de 2026, on sort !
        }
    }
}
    }
} catch (Exception e) {
    System.err.println("Erreur parsing timings: " + e.getMessage());
}

// 2. Si timings était vide ou a échoué, on utilise les champs par défaut en secours
if (eventStart == null) eventStart = parseDate(node.path("firstdate_begin").asText());
if (eventEnd == null) eventEnd = parseDate(node.path("lastdate_end").asText());

dto.setStart(eventStart);
dto.setEnd(eventEnd);
                    dto.setLocation(node.path("location_name").asText("Lieu non précisé"));
                    
                    String addr = node.path("location_address").asText("");
                    String city = node.path("location_city").asText("");
                    dto.setAddress(addr + (addr.isEmpty() || city.isEmpty() ? "" : ", ") + city);
                    
                    dto.setEventUrl(node.path("canonicalurl").asText());
                    // 2. INSERTION ICI : Lecture des mots-clés
                    List<String> tags = new ArrayList<>();
                    JsonNode keywordsNode = node.path("keywords_fr");
                    if (keywordsNode.isArray()) {
                        for (JsonNode kn : keywordsNode) {
                            tags.add(kn.asText());
                        }
                    }
                    dto.setKeywords(tags);
                    dto.setImage(node.path("image").asText(node.path("thumbnail").asText(null)));
                    System.out.println("   > Mots-clés pour " + dto.getTitle() + " : " + tags);
                    System.out.println("   > Ajout de l'événement : " + title);
                    events.add(dto);
                }
            }
            
            System.out.println("5. Taille finale de la liste envoyée au contrôleur : " + events.size());
            System.out.println("--- [DEBUG END] ---\n");
            return events;

        } catch (Exception e) {
            System.err.println("!!! ERREUR CRITIQUE API IDF : " + e.getMessage());
            e.printStackTrace(); // Pour voir la pile d'erreur complète
            return new ArrayList<>();
        }
    }

    private LocalDateTime parseDate(String dateStr) {
        if (dateStr == null || dateStr.isEmpty() || dateStr.equals("null")) return null;
        try {
            if (dateStr.contains("T")) {
                return LocalDateTime.parse(dateStr, DateTimeFormatter.ISO_OFFSET_DATE_TIME);
            } else {
                return java.time.LocalDate.parse(dateStr).atStartOfDay();
            }
        } catch (Exception e) {
            return null;
        }
    }
}