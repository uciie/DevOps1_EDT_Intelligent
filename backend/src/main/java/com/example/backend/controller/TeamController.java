package com.example.backend.controller;

import com.example.backend.model.Team;
import com.example.backend.model.User;
import com.example.backend.service.TeamService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Set;

@RestController
@RequestMapping("/api/teams")
public class TeamController {

    private final TeamService teamService;

    public TeamController(TeamService teamService) {
        this.teamService = teamService;
    }

    // 1. Créer une équipe 
    @PostMapping("/user/{userId}")
    public ResponseEntity<Team> createTeam(
            @RequestBody Team team, 
            @PathVariable Long userId) {
        return ResponseEntity.ok(teamService.createTeam(team.getName(), team.getDescription(), userId));
    }

    
    // 2. Ajouter un membre (POST /api/teams/1/members?userId=2)
    @PostMapping("/{teamId}/members")
    public ResponseEntity<Team> addMember(
            @PathVariable Long teamId,
            @RequestParam Long userId) {
        return ResponseEntity.ok(teamService.addMemberByUserId(teamId, userId));
    }

    // 3. Voir mes équipes (GET /api/teams/user/1)
    @GetMapping("/user/{userId}")
    public ResponseEntity<List<Team>> getMyTeams(@PathVariable Long userId) {
        return ResponseEntity.ok(teamService.getTeamsByUserId(userId));
    }

    // 4. Voir les membres d'une équipe (GET /api/teams/1/members)
    @GetMapping("/{teamId}/members")
    public ResponseEntity<Set<User>> getTeamMembers(@PathVariable Long teamId) {
        return ResponseEntity.ok(teamService.getTeamMembers(teamId));
    }
    @Transactional
    public void removeMember(Long teamId, Long memberIdToRemove, Long requesterId) {
        Team team = teamRepository.findById(teamId)
                .orElseThrow(() -> new IllegalArgumentException("Équipe introuvable"));

        // 1. Vérification de sécurité : Seul le chef d'équipe peut supprimer quelqu'un
        if (!team.getOwnerId().equals(requesterId)) {
            throw new SecurityException("Seul le chef d'équipe peut supprimer un membre.");
        }

        // 2. Empêcher le chef de se supprimer lui-même (optionnel, selon ta logique)
        if (memberIdToRemove.equals(team.getOwnerId())) {
             throw new IllegalArgumentException("Le chef d'équipe ne peut pas être supprimé de cette manière.");
        }

        User memberToRemove = userRepository.findById(memberIdToRemove)
                .orElseThrow(() -> new IllegalArgumentException("Utilisateur introuvable"));

        team.removeMember(memberToRemove); // Utilise la méthode utilitaire existante dans Team
        teamRepository.save(team);
    }
}