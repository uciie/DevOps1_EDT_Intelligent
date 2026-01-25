package com.example.backend.service.impl;

import com.example.backend.model.*;
import com.example.backend.repository.*;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;
import java.util.List;

import static org.assertj.core.api.Assertions.*;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
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

    private User owner;

    @BeforeEach
    void setUp() {
        owner = new User("owner", "pw");
        owner.setId(10L);
        // Manually construct and inject autowired fields (some autowired fields not injected by @InjectMocks)
        teamService = new TeamServiceImpl(teamRepository, userRepository, invitationRepository, taskRepository);
        try {
            java.lang.reflect.Field f1 = TeamServiceImpl.class.getDeclaredField("invitationRepository");
            f1.setAccessible(true);
            f1.set(teamService, invitationRepository);
            java.lang.reflect.Field f2 = TeamServiceImpl.class.getDeclaredField("taskRepository");
            f2.setAccessible(true);
            f2.set(teamService, taskRepository);
        } catch (NoSuchFieldException | IllegalAccessException e) {
            throw new RuntimeException(e);
        }
    }

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

    @Test
    void createTeam_success_savesTeam() {
        when(userRepository.findById(10L)).thenReturn(Optional.of(owner));
        Team saved = new Team("T","d",10L);
        when(teamRepository.save(any())).thenReturn(saved);

        Team t = teamService.createTeam("T","d",10L);

        assertThat(t.getOwnerId()).isEqualTo(10L);
        verify(teamRepository, times(1)).save(any(Team.class));
    }

    @Test
    void inviteMember_existingInvitation_updatesInviter() {
        Team team = new Team("T","d",10L); team.setId(1L);
        when(teamRepository.findById(1L)).thenReturn(Optional.of(team));

        TeamInvitation inv = new TeamInvitation(); inv.setId(2L);
        when(invitationRepository.findByTeam_IdAndInvitedUser_IdAndStatus(eq(1L), eq(5L), eq(com.example.backend.model.TeamInvitation.Status.PENDING))).thenReturn(Optional.of(inv));
        when(userRepository.findById(10L)).thenReturn(Optional.of(new User("a","b")));

        teamService.inviteMember(1L, 10L, 5L);

        verify(invitationRepository, times(1)).save(inv);
    }

    @Test
    void respondToInvitation_accept_addsMember() {
        Team team = new Team("T","d",10L); team.setId(1L);
        User invited = new User("u","p"); invited.setId(5L);
        TeamInvitation inv = new TeamInvitation(); inv.setId(7L); inv.setTeam(team); inv.setInvitedUser(invited);
        when(invitationRepository.findById(7L)).thenReturn(Optional.of(inv));
        when(teamRepository.save(any(Team.class))).thenAnswer(i -> i.getArgument(0));
        when(invitationRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        teamService.respondToInvitation(7L, true);

        verify(teamRepository).save(team);
        assertThat(team.getMembers()).contains(invited);
    }

    @Test
    void removeMember_ownerCannotBeRemoved_throwsSecurityException() {
        Team team = new Team("T","d",10L); team.setId(1L);
        when(teamRepository.findById(1L)).thenReturn(Optional.of(team));

        assertThrows(SecurityException.class, () -> teamService.removeMember(1L, 10L, 10L));
    }

    @Test
    void createTeam_UserNotFound_ThrowsException() {
        when(userRepository.findById(99L)).thenReturn(Optional.empty());
        
        assertThrows(IllegalArgumentException.class, () -> 
            teamService.createTeam("Nom", "Desc", 99L)
        );
    }

    @Test
    void inviteMember_RequesterNotOwner_ThrowsException() {
        Team team = new Team("T", "D", 1L); // Owner ID = 1
        team.setId(10L);
        when(teamRepository.findById(10L)).thenReturn(Optional.of(team));
        
        // Requester ID = 2 (pas le proprio)
        assertThrows(SecurityException.class, () -> 
            teamService.inviteMember(10L, 2L, 3L)
        );
    }

    @Test
    void inviteMember_AlreadyInvited_DoesNotCreateNew() {
        Team team = new Team("T", "D", 1L);
        team.setId(10L);
        when(teamRepository.findById(10L)).thenReturn(Optional.of(team));
        when(userRepository.findById(1L)).thenReturn(Optional.of(new User()));
        
        // Simuler une invitation déjà existante
        lenient().when(invitationRepository.findByTeam_IdAndInvitedUser_IdAndStatus(eq(10L), eq(3L), eq(TeamInvitation.Status.PENDING)))
            .thenReturn(Optional.of(new TeamInvitation()));

        teamService.inviteMember(10L, 1L, 3L);

        verify(invitationRepository, times(1)).save(any(TeamInvitation.class));
    }

    @Test
    void removeMember_ReassignsTasksToOwner() {
        User owner = new User("owner","pw"); owner.setId(1L);
        User member = new User("member","pw"); member.setId(2L);
        Team team = new Team("T", "D", 1L);
        team.getMembers().add(member);

        Task task = new Task();
        task.setAssignee(member);
        task.setUser(owner); // Le créateur de la tâche est l'owner

        when(teamRepository.findById(anyLong())).thenReturn(Optional.of(team));
        when(userRepository.findById(2L)).thenReturn(Optional.of(member));
        when(taskRepository.findByAssignee(member)).thenReturn(List.of(task));

        // requester is owner (1), member to remove is 2
        teamService.removeMember(10L, 2L, 1L);

        assertThat(task.getAssignee()).isEqualTo(owner);
        verify(taskRepository).save(task);
    }

    @Test
    void deleteTeam_NotOwner_ThrowsException() {
        Team team = new Team("T", "D", 1L);
        when(teamRepository.findById(10L)).thenReturn(Optional.of(team));

        assertThrows(SecurityException.class, () -> 
            teamService.deleteTeam(10L, 2L) // 2 est un intrus
        );
    }
}