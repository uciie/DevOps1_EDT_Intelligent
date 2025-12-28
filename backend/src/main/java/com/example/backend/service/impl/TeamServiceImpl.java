package com.example.backend.service.impl;

import java.util.List;
import java.util.Optional;
import java.util.Set;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.backend.model.Team;
import com.example.backend.model.User;
import com.example.backend.repository.TeamRepository;
import com.example.backend.repository.UserRepository;
import com.example.backend.service.TeamService;

@Service
public class TeamServiceImpl implements TeamService {

    private final TeamRepository teamRepository;
    private final UserRepository userRepository;

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
    @Transactional
    public void removeMember(Long teamId, Long memberIdToRemove, Long requesterId) {
        Team team = teamRepository.findById(teamId)
                .orElseThrow(() -> new IllegalArgumentException("Équipe introuvable"));

        // 1. Vérification de sécurité : Seul le chef d'équipe peut supprimer quelqu'un
        if (!team.getOwnerId().equals(requesterId)) {
            throw new SecurityException("Seul le chef d'équipe peut supprimer un membre.");
        }

        // 2. Empêcher le chef de se supprimer lui-même 
        if (memberIdToRemove.equals(team.getOwnerId())) {
             throw new IllegalArgumentException("Le chef d'équipe ne peut pas être supprimé de cette manière.");
        }

        User memberToRemove = userRepository.findById(memberIdToRemove)
                .orElseThrow(() -> new IllegalArgumentException("Utilisateur introuvable"));

        team.removeMember(memberToRemove); // Utilise la méthode utilitaire existante dans Team
        teamRepository.save(team);
    }

    @Override
    @Transactional
    public Team addMemberByUserId(Long teamId, Long userId) {
        Team team = teamRepository.findById(teamId)
                .orElseThrow(() -> new IllegalArgumentException("Équipe introuvable"));

        Optional<User> newMember = userRepository.findById(userId); 
        // Si elle n'existe pas, ajoutez : Optional<User> findById(Long id); dans UserRepository
        
        if (newMember == null) {
             throw new IllegalArgumentException("Utilisateur " + userId + " introuvable");
        }

        team.addMember(newMember.orElseThrow(() -> new IllegalArgumentException("Utilisateur " + userId + " introuvable")));
        return teamRepository.save(team);
    }

    @Override
    @Transactional
    public List<Team> getTeamsByUserId(Long userId) {
        User user = userRepository.findById(userId).orElseThrow();
        return user.getTeams(); 
    }

    @Override
    public Set<User> getTeamMembers(Long teamId) {
        Team team = teamRepository.findById(teamId)
                .orElseThrow(() -> new IllegalArgumentException("Équipe introuvable"));
        
        // Maintenant, team.getMembers() renvoie bien Set<User>, donc ça correspond !
        return team.getMembers();
    }
}