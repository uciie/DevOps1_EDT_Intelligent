package com.example.backend.repository;

import com.example.backend.model.TeamInvitation;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;

// Repository pour gérer les invitations d'équipe
public interface TeamInvitationRepository extends JpaRepository<TeamInvitation, Long> {
    List<TeamInvitation> findByTeamId(Long teamId);
    List<TeamInvitation> findByInvitedUserIdAndStatus(Long userId, TeamInvitation.Status status);
    // Trouver une invitation spécifique en attente
    Optional<TeamInvitation> findByTeamIdAndInvitedUserIdAndStatus(Long teamId, Long invitedUserId, TeamInvitation.Status status);
}