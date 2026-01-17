package com.example.backend.service.impl;

import com.example.backend.model.*;
import com.example.backend.repository.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * Tests du service Team avec Mockito.
 */
@ExtendWith(MockitoExtension.class)
class TeamServiceImplTest {

    @Mock
    private TeamRepository teamRepository;
    @Mock
    private UserRepository userRepository;
    @Mock
    private TeamInvitationRepository invitationRepository;
    @Mock
    private TaskRepository taskRepository;

    @InjectMocks
    private TeamServiceImpl teamService;

    @Test
    void createTeam_Succes() {
        // Mock : l'utilisateur existe
        User user = new User("testuser", "testpass");
        user.setId(1L);
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        
        // Mock : renvoie l'équipe sauvegardée
        when(teamRepository.save(any(Team.class))).thenAnswer(i -> i.getArguments()[0]);

        Team result = teamService.createTeam("DevOps", "Desc", 1L);

        assertThat(result.getName()).isEqualTo("DevOps");
        verify(teamRepository, times(1)).save(any(Team.class));
    }

    @Test
    void deleteTeam_Erreur_PasOwner() {
        // Mock : une équipe avec owner ID 1
        Team team = new Team("Alpha", "Desc", 1L);
        when(teamRepository.findById(100L)).thenReturn(Optional.of(team));

        // Test : l'utilisateur 2 tente de supprimer l'équipe de l'utilisateur 1
        assertThatThrownBy(() -> teamService.deleteTeam(100L, 2L))
            .isInstanceOf(SecurityException.class)
            .hasMessageContaining("Seul le créateur");
            
        // On vérifie qu'aucune suppression n'a été tentée
        verify(teamRepository, never()).delete(any());
    }
}