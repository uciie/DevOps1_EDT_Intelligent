package com.example.backend.controller;

import com.example.backend.dto.TimeSlot;
import com.example.backend.service.impl.FocusService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Rôle de la classe :
 * Tester la couche Web (API REST). On vérifie que les endpoints sont accessibles,
 * que les paramètres sont bien reçus et que le JSON retourné est valide.
 */
@WebMvcTest(FocusController.class) // On ne teste que ce contrôleur
public class FocusControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private FocusService focusService; // On simule le service

    /**
     * Test : Vérifier que l'appel GET retourne une liste de suggestions en JSON.
     */
    @Test
    void devraitRetournerListeSuggestionsEnFormatJson() throws Exception {
        // GIVEN : Une suggestion fictive que le service devrait retourner
        LocalDate dateTest = LocalDate.of(2024, 10, 25);
        LocalDateTime start = dateTest.atTime(14, 0);
        LocalDateTime end = dateTest.atTime(17, 0);
        TimeSlot suggestion = new TimeSlot(start, end);

        when(focusService.getOptimizedFocusSlots(eq(1L), any(LocalDate.class)))
                .thenReturn(List.of(suggestion));

        // WHEN & THEN : On simule l'appel HTTP GET
        mockMvc.perform(get("/api/focus/suggestions")
                .param("userId", "1")
                .param("date", "2024-10-25")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk()) // Statut 200 OK
                .andExpect(jsonPath("$[0].durationMinutes").value(180)) // Vérifie une valeur du JSON
                .andExpect(jsonPath("$[0].start").exists()); // Vérifie que la date est présente
    }

    /**
     * Test : Vérifier la gestion d'une date mal formatée.
     */
    @Test
    void devraitRetournerErreurSiDateMalFormatee() throws Exception {
        mockMvc.perform(get("/api/focus/suggestions")
                .param("userId", "1")
                .param("date", "25-10-2024") // Mauvais format (ISO attendu: YYYY-MM-DD)
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest()); // Doit retourner une erreur 400
    }
}