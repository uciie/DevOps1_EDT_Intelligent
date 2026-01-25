package com.example.backend.repository;

import com.example.backend.model.TeamInvitation;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;

// Repository pour gérer les invitations d'équipe
public interface TeamInvitationRepository extends JpaRepository<TeamInvitation, Long> {
    List<TeamInvitation> findByTeam_Id(Long teamId);
    List<TeamInvitation> findByInvitedUser_IdAndStatus(Long userId, TeamInvitation.Status status);
    // Trouver une invitation spécifique en attente
    Optional<TeamInvitation> findByTeam_IdAndInvitedUser_IdAndStatus(Long teamId, Long invitedUserId, TeamInvitation.Status status);
}