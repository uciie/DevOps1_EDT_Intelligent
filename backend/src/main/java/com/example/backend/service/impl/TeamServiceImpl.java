package com.example.backend.service.impl;

import java.util.List;
import java.util.Set;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.beans.factory.annotation.Autowired;

import com.example.backend.model.Team;
import com.example.backend.model.User;
import com.example.backend.model.TeamInvitation;
import com.example.backend.repository.TeamRepository;
import com.example.backend.repository.UserRepository;
import com.example.backend.repository.TeamInvitationRepository;
import com.example.backend.service.TeamService;

@Service
public class TeamServiceImpl implements TeamService {

    private final TeamRepository teamRepository;
    private final UserRepository userRepository;
    
    @Autowired
    private TeamInvitationRepository invitationRepository;

    public TeamServiceImpl(TeamRepository teamRepository, UserRepository userRepository) {
        this.teamRepository = teamRepository;
        this.userRepository = userRepository;
    }

    @Override
    @Transactional
    public Team createTeam(String name, String description, Long ownerId) {
        User owner = userRepository.findById(ownerId)
                .orElseThrow(() -> new IllegalArgumentException("Utilisateur introuvable"));

        Team team = new Team(name, description, ownerId);
        // L'owner est automatiquement le premier membre
        team.addMember(owner);
        
        return teamRepository.save(team);
    }

    @Override
    @Transactional
    public void inviteMember(Long teamId, Long userId) {
        Team team = teamRepository.findById(teamId)
                .orElseThrow(() -> new IllegalArgumentException("Équipe introuvable"));
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("Utilisateur introuvable"));

        // Création de l'invitation avec le statut PENDING
        TeamInvitation invitation = new TeamInvitation();
        invitation.setTeam(team);
        invitation.setInvitedUser(user);
        invitation.setStatus(TeamInvitation.Status.PENDING);
        
        invitationRepository.save(invitation);
    }

    @Override
    @Transactional
    public void respondToInvitation(Long invitationId, boolean accept) {
        TeamInvitation invitation = invitationRepository.findById(invitationId)
                .orElseThrow(() -> new IllegalArgumentException("Invitation introuvable"));

        if (accept) {
            invitation.setStatus(TeamInvitation.Status.ACCEPTED);
            Team team = invitation.getTeam();
            User user = invitation.getInvitedUser();
            
            // Logique initiale d'ajout de membre déplacée ici après acceptation
            team.addMember(user);
            teamRepository.save(team);
        } else {
            invitation.setStatus(TeamInvitation.Status.REJECTED);
        }
        
        invitationRepository.save(invitation);
    }

    @Override
    public List<TeamInvitation> getPendingInvitations(Long userId) {
        return invitationRepository.findByInvitedUserIdAndStatus(userId, TeamInvitation.Status.PENDING);
    }

    @Override
    @Transactional
    public Team addMemberByUserId(Long teamId, Long userId) {
        Team team = teamRepository.findById(teamId)
                .orElseThrow(() -> new IllegalArgumentException("Équipe introuvable"));

        // Récupérer l'utilisateur ou lancer l'exception
        User newMember = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("Utilisateur " + userId + " introuvable"));

        // Ajouter le membre directement (Ancienne logique RM-01)
        team.addMember(newMember);
        
        return teamRepository.save(team);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Team> getTeamsByUserId(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("Utilisateur introuvable"));
        return user.getTeams(); 
    }

    @Override
    public Set<User> getTeamMembers(Long teamId) {
        Team team = teamRepository.findById(teamId)
                .orElseThrow(() -> new IllegalArgumentException("Équipe introuvable"));
        
        // Maintenant, team.getMembers() renvoie bien Set<User>, donc ça correspond !
        return team.getMembers();
    }

    @Override
    @Transactional
    public void removeMember(Long teamId, Long memberIdToRemove, Long requesterId) {
        Team team = teamRepository.findById(teamId)
                .orElseThrow(() -> new IllegalArgumentException("Équipe introuvable"));

        // Logique de suppression (à compléter selon vos besoins de sécurité)
        User userToRemove = userRepository.findById(memberIdToRemove)
                .orElseThrow(() -> new IllegalArgumentException("Membre introuvable"));
        
        team.getMembers().remove(userToRemove);
        teamRepository.save(team);
    }

    @Override
    @Transactional
    public void deleteTeam(Long teamId, Long requesterId) {
        Team team = teamRepository.findById(teamId)
                .orElseThrow(() -> new IllegalArgumentException("Équipe introuvable"));

        if (!team.getOwnerId().equals(requesterId)) {
            throw new SecurityException("Seul le créateur peut supprimer l'équipe.");
        }

        // On nettoie les relations avant de supprimer pour éviter les erreurs de clé étrangère
        team.getMembers().clear(); 
        teamRepository.delete(team);
    }
}