package com.example.backend.controller;

import com.example.backend.model.Team;
import com.example.backend.service.TeamService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.ResponseEntity;
import org.springframework.http.HttpStatus;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TeamControllerTest {

    @Mock
    private TeamService teamService;

    @InjectMocks
    private TeamController teamController;

    @Test
    void createTeam_Retourne200() {
        Team teamInput = new Team();
        teamInput.setName("Team1");
        
        // Simulation du service
        when(teamService.createTeam(anyString(), any(), anyLong()))
            .thenReturn(new Team("Team1", null, 1L));

        ResponseEntity<Team> response = teamController.createTeam(teamInput, 1L);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody().getName()).isEqualTo("Team1");
    }

    @Test
    void deleteTeam_Retourne403_SiSecurityException() {
        // Simule un refus d'autorisation par le service
        doThrow(new SecurityException("Interdit"))
            .when(teamService).deleteTeam(1L, 2L);

        ResponseEntity<Object> response = teamController.deleteTeam(1L, 2L);

        assertThat(response.getStatusCodeValue()).isEqualTo(403);
        assertThat(response.getBody()).isEqualTo("Interdit");
    }
}