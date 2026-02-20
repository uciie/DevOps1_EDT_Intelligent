package com.example.backend.service.impl;

import java.util.List;
import java.util.Set;
import java.util.Optional;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.backend.model.Team;
import com.example.backend.model.User;
import com.example.backend.model.Task;
import com.example.backend.model.TeamInvitation;
import com.example.backend.repository.TeamRepository;
import com.example.backend.repository.UserRepository;
import com.example.backend.repository.TeamInvitationRepository;
import com.example.backend.repository.TaskRepository;
import com.example.backend.service.TeamService;

@Service
public class TeamServiceImpl implements TeamService {

    private final TeamRepository teamRepository;
    private final UserRepository userRepository;
    private final TeamInvitationRepository invitationRepository;
    private final TaskRepository taskRepository;

    public TeamServiceImpl(TeamRepository teamRepository,
                           UserRepository userRepository,
                           TeamInvitationRepository invitationRepository,
                           TaskRepository taskRepository) {
        this.teamRepository = teamRepository;
        this.userRepository = userRepository;
        this.invitationRepository = invitationRepository;
        this.taskRepository = taskRepository;
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
    public void inviteMember(Long teamId, Long inviterId, Long invitedUserId) {
        Team team = teamRepository.findById(teamId)
                .orElseThrow(() -> new RuntimeException("Équipe non trouvée"));
        // Vérifier que l'inviteur a le droit d'inviter (seul le propriétaire pour l'instant)
        if (!team.getOwnerId().equals(inviterId)) {
            throw new SecurityException("Vous n'avez pas le droit d'inviter des membres.");
        }
        
        // 2. Vérifier si une invitation en attente existe déjà
        Optional<TeamInvitation> existingInvitation = invitationRepository
            .findByTeam_IdAndInvitedUser_IdAndStatus(teamId, invitedUserId, TeamInvitation.Status.PENDING);

        if (existingInvitation.isPresent()) {
            // MISE À JOUR : On met à jour la date ou l'inviteur si nécessaire
            TeamInvitation invitation = existingInvitation.get();
            invitation.setInviter(userRepository.findById(inviterId)
                    .orElseThrow(() -> new RuntimeException("Inviteur non trouvé")));
            // Vous pouvez ajouter un champ 'updatedAt' si disponible dans votre modèle
            invitationRepository.save(invitation);
        }
        else{
            // 3. Sinon, créer une nouvelle invitation
            TeamInvitation newInvitation = new TeamInvitation();
            newInvitation.setTeam(team);
            newInvitation.setInviter(userRepository.findById(inviterId).get());
            newInvitation.setInvitedUser(userRepository.findById(invitedUserId).get());
            newInvitation.setStatus(TeamInvitation.Status.PENDING);
            invitationRepository.save(newInvitation);
        }
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
        return invitationRepository.findByInvitedUser_IdAndStatus(userId, TeamInvitation.Status.PENDING);
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
    public Set<Team> getTeamsByUserId(Long userId) {
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

    // 1. Empêcher le propriétaire de "quitter" l'équipe sans la supprimer
    // (Sinon l'équipe se retrouve sans membre owner, ou "orpheline")
    if (team.getOwnerId().equals(memberIdToRemove)) {
        throw new SecurityException("Le propriétaire ne peut pas quitter l'équipe. Il doit la supprimer.");
    }

    // 2. Vérification des droits
    boolean isSelfRemoval = requesterId.equals(memberIdToRemove); // Quitter
    boolean isOwnerAction = team.getOwnerId().equals(requesterId); // Exclure

    if (!isSelfRemoval && !isOwnerAction) {
        throw new SecurityException("Vous n'avez pas le droit de supprimer ce membre.");
    }

    User userToRemove = userRepository.findById(memberIdToRemove)
            .orElseThrow(() -> new IllegalArgumentException("Membre introuvable"));

        // réattribution des tâches assignées à ce membre au créateur de l'équipe ou les libérer
        List<Task> tasksToUpdate = taskRepository.findByAssignee(userToRemove);
    for (Task task : tasksToUpdate) {
        User creator = task.getUser();
        if (creator != null && !creator.getId().equals(memberIdToRemove)) {
            task.setAssignee(creator);
        } else {
            task.setAssignee(null);
        }
        taskRepository.save(task);
    }
    
    team.removeMember(userToRemove); // Utilise ta méthode helper dans Team.java
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

        List<TeamInvitation> invitations = invitationRepository.findByTeam_Id(teamId); // Suppose l'ajout de cette méthode dans le repo
        invitationRepository.deleteAll(invitations);

        List<Task> tasks = taskRepository.findByTeam_Id(teamId); // Suppose l'ajout de cette méthode dans TaskRepository
        for (Task task : tasks) {
            task.setTeam(null);
            taskRepository.save(task);
        }
        
        // On nettoie les relations avant de supprimer pour éviter les erreurs de clé étrangère
        team.getMembers().clear(); 
        teamRepository.delete(team);
    }
}