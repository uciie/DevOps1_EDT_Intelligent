package com.example.backend.controller;

import com.example.backend.model.Team;
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
    public ResponseEntity<Set<Long>> getTeamMembers(@PathVariable Long teamId) {
        return ResponseEntity.ok(teamService.getTeamMembers(teamId));
    }
}