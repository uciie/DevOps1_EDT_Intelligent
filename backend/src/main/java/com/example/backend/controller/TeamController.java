package com.example.backend.controller;

import com.example.backend.model.Team;
import com.example.backend.model.TeamInvitation;
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
    // AJOUT : Endpoint pour supprimer un membre
    // DELETE /api/teams/{teamId}/members/{memberId}?requesterId=...
    @DeleteMapping("/{teamId}/members/{memberId}")
    public ResponseEntity<?> removeMember(
            @PathVariable Long teamId,
            @PathVariable Long memberId,
            @RequestParam Long requesterId) { // On passe l'ID de celui qui fait la demande
        
        try {
            teamService.removeMember(teamId, memberId, requesterId);
            return ResponseEntity.ok().build();
        } catch (SecurityException e) {
            return ResponseEntity.status(403).body(e.getMessage());
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }
    @DeleteMapping("/{teamId}")
    public ResponseEntity<?> deleteTeam(
            @PathVariable Long teamId,
            @RequestParam Long requesterId) {
        try {
            teamService.deleteTeam(teamId, requesterId);
            return ResponseEntity.ok().build();
        } catch (SecurityException e) {
            return ResponseEntity.status(403).body(e.getMessage());
        } catch (IllegalArgumentException e) {
            return ResponseEntity.notFound().build();
        }
    }

    @PostMapping("/{teamId}/invite/{invitedUserId}")
    public ResponseEntity<TeamInvitation> inviteUser(
            @PathVariable Long teamId,
            @PathVariable Long invitedUserId,
            @RequestParam Long inviterId) {
        
        // L'appel au service gérera soit la création, soit la mise à jour 
        // si une invitation est déjà en attente.
        teamService.inviteMember(teamId, inviterId, invitedUserId);
        
        return ResponseEntity.ok().build();
    }

    @GetMapping("/invitations/pending/{userId}")
    public List<TeamInvitation> getInvitations(@PathVariable Long userId) {
        return teamService.getPendingInvitations(userId);
    }

    @PostMapping("/invitations/{invitationId}/respond")
    public ResponseEntity<?> respond(@PathVariable Long invitationId, @RequestParam boolean accept) {
        teamService.respondToInvitation(invitationId, accept);
        return ResponseEntity.ok().build();
    }
}