package com.example.backend.service;

import java.util.List;
import java.util.Set;

import com.example.backend.model.Team;
import com.example.backend.model.User;
import com.example.backend.model.TeamInvitation; // Import de la nouvelle entité

public interface TeamService {
    // Créer une équipe
    Team createTeam(String name, String description, Long ownerId);

    // Ajouter un membre via son userId (RM-01)
    // Note : Cette méthode peut être conservée pour un ajout forcé par l'admin 
    // ou modifiée pour appeler en interne la logique d'invitation.
    Team addMemberByUserId(Long teamId, Long userId);

    // Envoyer une invitation
    void inviteMember(Long teamId, Long inviterId, Long invitedUserId);

    // Répondre à une invitation (Accepter/Refuser)
    void respondToInvitation(Long invitationId, boolean accept);

    // Récupérer les invitations en attente pour un utilisateur
    List<TeamInvitation> getPendingInvitations(Long userId);

    // Récupérer les équipes d'un utilisateur
    List<Team> getTeamsByUserId(Long userId);

    // Récupérer les membres d'une équipe
    Set<User> getTeamMembers(Long teamId);

    void removeMember(Long teamId, Long memberIdToRemove, Long requesterId);

    void deleteTeam(Long teamId, Long requesterId);
}