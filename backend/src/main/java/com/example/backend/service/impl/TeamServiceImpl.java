package com.example.backend.service.impl;

import com.example.backend.model.Team;
import com.example.backend.model.User;
import com.example.backend.repository.TeamRepository;
import com.example.backend.repository.UserRepository;
import com.example.backend.service.TeamService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.Set;

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

        Team team = new Team(name, description);
        // L'owner est automatiquement le premier membre
        team.addMember(owner);
        
        return teamRepository.save(team);
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
    public List<Team> getTeamsByUserId(Long userId) {
        User user = userRepository.findById(userId).orElseThrow();
        return user.getTeams(); 
    }

    @Override
    public Set<Long> getTeamMembers(Long teamId) {
        Team team = teamRepository.findById(teamId)
                .orElseThrow(() -> new IllegalArgumentException("Équipe introuvable"));
        return team.getMembers();
    }
}